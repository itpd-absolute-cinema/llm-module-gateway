package ru.inno.itpd.gateway.core.dao.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document("pricing")
public record PricingDocument(
        @Id String model,
        @Field("input_price_per_1m") double inputPricePer1m,
        @Field("output_price_per_1m") double outputPricePer1m,
        String currency,
        @Field("updated_at") Instant updatedAt
) {
}
