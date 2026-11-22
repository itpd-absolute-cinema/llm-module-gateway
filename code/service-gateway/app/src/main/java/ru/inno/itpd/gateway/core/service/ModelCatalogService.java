package ru.inno.itpd.gateway.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.inno.itpd.gateway.api.dto.ModelDtos.AdminModel;
import ru.inno.itpd.gateway.api.dto.ModelDtos.ModelInfo;
import ru.inno.itpd.gateway.api.dto.ModelDtos.ModelPricing;
import ru.inno.itpd.gateway.config.properties.GatewayProperties;
import ru.inno.itpd.gateway.core.client.llm.LlmClient;
import ru.inno.itpd.gateway.core.dao.entity.PricingDocument;
import ru.inno.itpd.gateway.core.exception.GatewayException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Список моделей апстрима с коротким TTL-кэшем; при недоступности апстрима отдаёт устаревший кэш. */
@Service
public class ModelCatalogService {
    private static final Logger log = LoggerFactory.getLogger(ModelCatalogService.class);

    private record Cached(List<ModelInfo> models, Instant fetchedAt) {
    }

    private final LlmClient llm;
    private final PricingService pricing;
    private final Duration ttl;
    private volatile Cached cache;

    public ModelCatalogService(LlmClient llm, PricingService pricing, GatewayProperties props) {
        this.llm = llm;
        this.pricing = pricing;
        this.ttl = props.models().cacheTtl();
    }

    public List<ModelInfo> list() {
        Cached c = cache;
        if (c != null && Instant.now().isBefore(c.fetchedAt().plus(ttl))) return c.models();
        synchronized (this) {
            c = cache;
            if (c != null && Instant.now().isBefore(c.fetchedAt().plus(ttl))) return c.models();
            try {
                List<ModelInfo> fresh = List.copyOf(llm.listModels());
                cache = new Cached(fresh, Instant.now());
                return fresh;
            } catch (GatewayException e) {
                if (c != null) {
                    log.warn("Could not refresh the model list ({}), serving a stale copy", e.getMessage());
                    return c.models();
                }
                throw e;
            }
        }
    }

    public List<AdminModel> listWithPricing() {
        Map<String, PricingDocument> prices = pricing.byModel();
        return list().stream().map(m -> {
            PricingDocument p = prices.get(m.id());
            ModelPricing mp = p == null ? null
                    : new ModelPricing(p.inputPricePer1m(), p.outputPricePer1m(), p.currency());
            return new AdminModel(m.id(), m.created(), m.ownedBy(), mp);
        }).toList();
    }
}
