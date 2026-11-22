package ru.inno.itpd.gateway.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.inno.itpd.gateway.api.dto.ModelDtos.ModelList;
import ru.inno.itpd.gateway.core.exception.GatewayException;
import ru.inno.itpd.gateway.core.service.BlockService;
import ru.inno.itpd.gateway.core.service.ModelCatalogService;

@RestController
@Tag(name = "Models", description = "Models available through the gateway")
@SecurityRequirement(name = "HTTPBearer")
public class ModelsController {
    private final ModelCatalogService models;
    private final BlockService blocks;

    public ModelsController(ModelCatalogService models, BlockService blocks) {
        this.models = models;
        this.blocks = blocks;
    }

    @GetMapping("/models")
    @Operation(summary = "List Models", description = "OpenAI-compatible model list (cached upstream response).",
            operationId = "list_models_models_get")
    public ModelList list(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new GatewayException(HttpStatus.UNAUTHORIZED, "unauthorized", "The token has no subject claim");
        }
        blocks.assertNotBlocked(jwt.getSubject());
        return new ModelList("list", models.list());
    }
}
