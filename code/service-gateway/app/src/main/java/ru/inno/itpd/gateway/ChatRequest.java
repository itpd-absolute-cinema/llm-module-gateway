
package ru.inno.itpd.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {

    @NotBlank
    private String model;

    @NotEmpty
    @Valid
    private List<Message> messages;

    @AssertFalse
    private Boolean stream = false;

    @DecimalMin("0.0")
    @DecimalMax("2.0")
    private Double temperature;

    @JsonProperty("top_p")
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Double topP;

    @JsonProperty("max_tokens")
    @Positive
    private Integer maxTokens;

    @JsonProperty("max_completion_tokens")
    @Positive
    private Integer maxCompletionTokens;

    private Object stop;
}
