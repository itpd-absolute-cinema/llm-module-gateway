package ru.inno.itpd.gateway.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.inno.itpd.gateway.api.dto.PricingDtos.PricingTable;
import ru.inno.itpd.gateway.api.dto.PricingDtos.PricingUpdate;
import ru.inno.itpd.gateway.core.service.PricingService;

@RestController
@RequestMapping("/admin/pricing")
@Tag(name = "Admin: pricing")
@SecurityRequirement(name = "HTTPBearer")
public class PricingAdminController {
    private final PricingService pricing;

    public PricingAdminController(PricingService pricing) {
        this.pricing = pricing;
    }

    @GetMapping
    @Operation(summary = "Get Pricing", operationId = "admin_get_pricing")
    public PricingTable get() {
        return pricing.table();
    }

    @PutMapping
    @Operation(summary = "Replace Pricing",
            description = "Replaces the whole price table. Costs of already recorded events do not change.",
            operationId = "admin_put_pricing")
    public PricingTable put(@Valid @RequestBody PricingUpdate body) {
        return pricing.replace(body.items());
    }
}
