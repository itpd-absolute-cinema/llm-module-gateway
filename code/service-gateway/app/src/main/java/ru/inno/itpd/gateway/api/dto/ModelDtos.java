package ru.inno.itpd.gateway.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class ModelDtos {
    private ModelDtos() {}

    /** Модель в формате OpenAI. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModelInfo(String id, String object, long created, @JsonProperty("owned_by") String ownedBy) {
        public ModelInfo {
            if (object == null) object = "model";
            if (ownedBy == null) ownedBy = "unknown";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModelList(String object, List<ModelInfo> data) {
    }

    public record AdminModel(
            String id,
            long created,
            @JsonProperty("owned_by") String ownedBy,
            @JsonInclude(JsonInclude.Include.NON_NULL) ModelPricing pricing
    ) {
    }

    public record ModelPricing(
            @JsonProperty("input_price_per_1m") double inputPricePer1m,
            @JsonProperty("output_price_per_1m") double outputPricePer1m,
            String currency
    ) {
    }

    public record AdminModelList(List<AdminModel> items) {
    }
}
