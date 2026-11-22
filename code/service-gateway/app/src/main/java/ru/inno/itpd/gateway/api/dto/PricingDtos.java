package ru.inno.itpd.gateway.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class PricingDtos {
    private PricingDtos() {}

    public record PriceInput(
            @NotBlank @Size(max = 200) String model,
            @NotNull @PositiveOrZero @JsonProperty("input_price_per_1m") Double inputPricePer1m,
            @NotNull @PositiveOrZero @JsonProperty("output_price_per_1m") Double outputPricePer1m,
            @NotNull @Pattern(regexp = "[A-Z]{3}") String currency
    ) {
    }

    public record PricingUpdate(@NotNull @Valid List<@NotNull @Valid PriceInput> items) {
    }

    public record PriceItem(
            String model,
            @JsonProperty("input_price_per_1m") double inputPricePer1m,
            @JsonProperty("output_price_per_1m") double outputPricePer1m,
            String currency,
            @JsonProperty("updated_at") Instant updatedAt
    ) {
    }

    public record PricingTable(String currency, List<PriceItem> items) {
    }
}
