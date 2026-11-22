package ru.inno.itpd.gateway.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class UserDtos {
    private UserDtos() {}

    public record BlockRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record CreateUserRequest(
            @NotBlank @Size(min = 1, max = 100) String username,
            @Email String email,
            @JsonProperty("first_name") @Size(max = 255) String firstName,
            @JsonProperty("last_name") @Size(max = 255) String lastName,
            @NotBlank String password,
            @JsonProperty("temporary_password") Boolean temporaryPassword
    ) {
        /** Пароль не должен попадать в логи, в том числе случайно через toString. */
        @Override
        public String toString() {
            return "CreateUserRequest[username=" + username + ", email=" + email + ", password=***]";
        }
    }

    public record BlockInfo(
            String reason,
            @JsonProperty("blocked_at") Instant blockedAt,
            @JsonProperty("blocked_by") String blockedBy,
            @JsonProperty("keycloak_disabled") boolean keycloakDisabled
    ) {
    }

    public record UserView(
            String id,
            String username,
            String email,
            @JsonInclude(JsonInclude.Include.NON_NULL) Boolean enabled,
            boolean blocked,
            @JsonInclude(JsonInclude.Include.NON_NULL) BlockInfo block,
            @JsonProperty("last_activity") Instant lastActivity
    ) {
    }
}
