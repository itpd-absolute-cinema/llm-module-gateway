package ru.inno.itpd.gateway.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.inno.itpd.gateway.api.dto.PageResponse;
import ru.inno.itpd.gateway.api.dto.UserDtos.BlockRequest;
import ru.inno.itpd.gateway.api.dto.UserDtos.CreateUserRequest;
import ru.inno.itpd.gateway.api.dto.UserDtos.UserView;
import ru.inno.itpd.gateway.core.service.UserAdminService;
import ru.inno.itpd.gateway.util.QueryParams;

@RestController
@RequestMapping("/admin/users")
@Tag(name = "Admin: users")
@SecurityRequirement(name = "HTTPBearer")
public class UserAdminController {
    private final UserAdminService users;

    public UserAdminController(UserAdminService users) {
        this.users = users;
    }

    @GetMapping
    @Operation(summary = "List Users", description = "Users from Keycloak with block status and last activity.",
            operationId = "admin_list_users")
    public PageResponse<UserView> list(@RequestParam(required = false) String search,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        QueryParams.page(page, size);
        return users.list(QueryParams.blankToNull(search), page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create User",
            description = "Creates an enabled user in Keycloak (email marked as verified) with a password and the realm role "
                    + "`chat-user`. If the role cannot be assigned, the created user is deleted.",
            operationId = "admin_create_user")
    public UserView create(@Valid @RequestBody CreateUserRequest body) {
        return users.create(body);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get User", operationId = "admin_get_user")
    public UserView get(@PathVariable String id) {
        return users.get(id);
    }

    @PostMapping("/{id}/block")
    @Operation(summary = "Block User",
            description = "Blocks the user in the gateway immediately (403 user_blocked on the next request) "
                    + "and, if configured, disables the account in Keycloak.",
            operationId = "admin_block_user")
    public UserView block(@PathVariable String id, @Valid @RequestBody BlockRequest body) {
        return users.block(id, body.reason().strip());
    }

    @PostMapping("/{id}/unblock")
    @Operation(summary = "Unblock User", operationId = "admin_unblock_user")
    public UserView unblock(@PathVariable String id) {
        return users.unblock(id);
    }
}
