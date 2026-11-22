package ru.inno.itpd.gateway.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.inno.itpd.gateway.api.dto.PageResponse;
import ru.inno.itpd.gateway.api.dto.UserDtos.BlockInfo;
import ru.inno.itpd.gateway.api.dto.UserDtos.CreateUserRequest;
import ru.inno.itpd.gateway.api.dto.UserDtos.UserView;
import ru.inno.itpd.gateway.config.properties.GatewayProperties;
import ru.inno.itpd.gateway.core.client.keycloak.KeycloakAdminClient;
import ru.inno.itpd.gateway.core.client.keycloak.KeycloakAdminClient.KcUser;
import ru.inno.itpd.gateway.core.dao.entity.BlockedUserDocument;
import ru.inno.itpd.gateway.core.dao.repository.BlockedUserRepository;
import ru.inno.itpd.gateway.core.exception.GatewayException;
import ru.inno.itpd.gateway.core.security.Actor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UserAdminService {
    private static final Logger log = LoggerFactory.getLogger(UserAdminService.class);
    private static final String CHAT_ROLE = "chat-user";
    private static final Pattern USER_ID = Pattern.compile("[A-Za-z0-9._:@-]{1,128}");

    private final KeycloakAdminClient keycloak;
    private final BlockedUserRepository blocked;
    private final UsageQueryService usage;
    private final AuditService audit;
    private final GatewayProperties props;

    public UserAdminService(KeycloakAdminClient keycloak, BlockedUserRepository blocked, UsageQueryService usage,
                            AuditService audit, GatewayProperties props) {
        this.keycloak = keycloak;
        this.blocked = blocked;
        this.usage = usage;
        this.audit = audit;
        this.props = props;
    }

    public PageResponse<UserView> list(String search, int page, int size) {
        requireKeycloak();
        List<KcUser> users = keycloak.search(search, page * size, size);
        long total = keycloak.count(search);
        List<String> ids = users.stream().map(KcUser::id).toList();
        Map<String, BlockedUserDocument> blockMap = blocked.findAllById(ids).stream()
                .collect(Collectors.toMap(BlockedUserDocument::userId, b -> b));
        Map<String, Instant> activity = usage.lastActivity(ids);
        List<UserView> items = users.stream()
                .map(u -> view(u.id(), u, blockMap.get(u.id()), activity.get(u.id())))
                .toList();
        return new PageResponse<>(items, page, size, total);
    }

    /**
     * Создаёт пользователя в Keycloak и выдаёт ему роль chat-user. Если роль выдать не удалось,
     * созданный пользователь удаляется (иначе остался бы аккаунт без роли).
     */
    public UserView create(CreateUserRequest req) {
        requireKeycloak();
        // Keycloak хранит username и email в нижнем регистре — возвращаем то, что реально создано
        String username = req.username().strip().toLowerCase(Locale.ROOT);
        String email = req.email() == null || req.email().isBlank() ? null : req.email().strip().toLowerCase(Locale.ROOT);

        String id = keycloak.createUser(new KeycloakAdminClient.NewUser(username, email,
                blankToNull(req.firstName()), blankToNull(req.lastName()), req.password(),
                Boolean.TRUE.equals(req.temporaryPassword())));

        try {
            keycloak.assignRealmRole(id, CHAT_ROLE);
        } catch (RuntimeException e) {
            rollbackCreatedUser(id, username, e);
            throw e instanceof GatewayException g ? g : new GatewayException(HttpStatus.BAD_GATEWAY,
                    "identity_provider_error", "Could not assign the realm role '" + CHAT_ROLE + "'; the user was not created");
        }

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("username", username);
        after.put("email", email);
        after.put("roles", List.of(CHAT_ROLE));   // пароля здесь нет и быть не должно
        audit.record("user.create", "user", id, null, after);
        return new UserView(id, username, email, true, false, null, null);
    }

    private void rollbackCreatedUser(String id, String username, RuntimeException cause) {
        log.warn("Assigning role '{}' to the new user {} failed ({}); deleting the user", CHAT_ROLE, id, cause.getMessage());
        try {
            keycloak.deleteUser(id);
        } catch (RuntimeException deleteError) {
            log.error("Could not delete the user {} ('{}') after a failed role assignment: it is left in Keycloak without the role",
                    id, username, deleteError);
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "identity_provider_error",
                    "The user was created (id " + id + ") but the role '" + CHAT_ROLE + "' could not be assigned, "
                            + "and rolling back failed; delete the user in Keycloak manually");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    public UserView get(String id) {
        requireValidId(id);
        requireKeycloak();
        KcUser user = keycloak.find(id).orElseThrow(() -> notFound());
        return snapshot(id, user);
    }

    public synchronized UserView block(String id, String reason) {
        requireValidId(id);
        Actor actor = Actor.current();
        if (id.equals(actor.id())) {
            throw new GatewayException(HttpStatus.CONFLICT, "cannot_block_self", "An administrator cannot block themselves");
        }
        KcUser kcUser = null;
        if (keycloak.isConfigured()) {
            kcUser = keycloak.find(id).orElseThrow(() -> notFound());
        }
        if (blocked.existsById(id)) {
            throw new GatewayException(HttpStatus.CONFLICT, "user_already_blocked", "The user is already blocked");
        }

        // сначала запись в Mongo — с этого момента запросы пользователя отклоняются
        BlockedUserDocument doc = blocked.save(new BlockedUserDocument(id, reason, Instant.now(), actor.id(), false));

        // потом, по возможности, выключаем учётку в Keycloak (токен перестанет обновляться)
        if (keycloak.isConfigured() && props.keycloak().syncEnabled() && !Boolean.FALSE.equals(kcUser.enabled())) {
            try {
                keycloak.setEnabled(id, false);
                doc = blocked.save(new BlockedUserDocument(id, reason, doc.blockedAt(), actor.id(), true));
            } catch (GatewayException e) {
                log.warn("User {} is blocked in the gateway, but disabling it in Keycloak failed: {}", id, e.getMessage());
            }
        }

        audit.record("user.block", "user", id, Map.of("blocked", false),
                Map.of("blocked", true, "reason", reason, "keycloak_disabled", doc.keycloakDisabled()));
        return snapshot(id, kcUser);
    }

    public synchronized UserView unblock(String id) {
        requireValidId(id);
        BlockedUserDocument doc = blocked.findById(id).orElseThrow(() ->
                new GatewayException(HttpStatus.CONFLICT, "user_not_blocked", "The user is not blocked"));
        blocked.deleteById(id);

        // включаем обратно только если выключал сам шлюз
        if (doc.keycloakDisabled() && keycloak.isConfigured()) {
            try {
                keycloak.setEnabled(id, true);
            } catch (GatewayException e) {
                log.warn("User {} is unblocked in the gateway, but enabling it in Keycloak failed: {}", id, e.getMessage());
            }
        }

        audit.record("user.unblock", "user", id,
                Map.of("blocked", true, "reason", doc.reason()), Map.of("blocked", false));
        KcUser kcUser = keycloak.isConfigured() ? keycloak.find(id).orElse(null) : null;
        return snapshot(id, kcUser);
    }

    // ---- helpers ----

    private UserView snapshot(String id, KcUser kc) {
        return view(id, kc, blocked.findById(id).orElse(null), usage.lastActivity(List.of(id)).get(id));
    }

    private static UserView view(String id, KcUser kc, BlockedUserDocument block, Instant lastActivity) {
        BlockInfo info = block == null ? null
                : new BlockInfo(block.reason(), block.blockedAt(), block.blockedBy(), block.keycloakDisabled());
        return new UserView(id, kc == null ? null : kc.username(), kc == null ? null : kc.email(),
                kc == null ? null : kc.enabled(), block != null, info, lastActivity);
    }

    private void requireKeycloak() {
        if (!keycloak.isConfigured()) {
            throw new GatewayException(HttpStatus.SERVICE_UNAVAILABLE, "identity_provider_not_configured",
                    "Keycloak admin access is not configured (KEYCLOAK_BASE_URL, KEYCLOAK_ADMIN_CLIENT_ID, KEYCLOAK_ADMIN_CLIENT_SECRET)");
        }
    }

    private static void requireValidId(String id) {
        if (id == null || !USER_ID.matcher(id).matches()) {
            throw new GatewayException(HttpStatus.BAD_REQUEST, "invalid_user_id", "Invalid user id");
        }
    }

    private static GatewayException notFound() {
        return new GatewayException(HttpStatus.NOT_FOUND, "user_not_found", "User not found");
    }
}
