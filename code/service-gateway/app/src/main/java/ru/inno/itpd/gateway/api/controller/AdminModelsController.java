package ru.inno.itpd.gateway.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.inno.itpd.gateway.api.dto.ModelDtos.AdminModelList;
import ru.inno.itpd.gateway.core.service.ModelCatalogService;

@RestController
@RequestMapping("/admin/models")
@Tag(name = "Admin: models")
@SecurityRequirement(name = "HTTPBearer")
public class AdminModelsController {
    private final ModelCatalogService models;

    public AdminModelsController(ModelCatalogService models) {
        this.models = models;
    }

    @GetMapping
    @Operation(summary = "List Models With Pricing", description = "Upstream models together with prices from the pricing table.",
            operationId = "admin_list_models")
    public AdminModelList list() {
        return new AdminModelList(models.listWithPricing());
    }
}
