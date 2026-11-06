
package ru.inno.itpd.gateway.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Message {

    @NotNull
    private Role role;

    @NotNull
    private String content;

    public enum Role {
        system,
        developer,
        user,
        assistant
    }
}
