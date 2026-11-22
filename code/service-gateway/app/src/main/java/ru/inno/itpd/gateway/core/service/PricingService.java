package ru.inno.itpd.gateway.core.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.inno.itpd.gateway.api.dto.PricingDtos.PriceInput;
import ru.inno.itpd.gateway.api.dto.PricingDtos.PriceItem;
import ru.inno.itpd.gateway.api.dto.PricingDtos.PricingTable;
import ru.inno.itpd.gateway.config.properties.GatewayProperties;
import ru.inno.itpd.gateway.core.dao.entity.PricingDocument;
import ru.inno.itpd.gateway.core.dao.repository.PricingRepository;
import ru.inno.itpd.gateway.core.exception.GatewayException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PricingService {
    private final PricingRepository repo;
    private final AuditService audit;
    private final GatewayProperties props;

    public PricingService(PricingRepository repo, AuditService audit, GatewayProperties props) {
        this.repo = repo;
        this.audit = audit;
        this.props = props;
    }

    public PricingTable table() {
        List<PricingDocument> docs = repo.findAll().stream()
                .sorted((a, b) -> a.model().compareTo(b.model()))
                .toList();
        return toTable(docs);
    }

    public Optional<PricingDocument> find(String model) {
        return model == null ? Optional.empty() : repo.findById(model);
    }

    public Map<String, PricingDocument> byModel() {
        return repo.findAll().stream().collect(Collectors.toMap(PricingDocument::model, Function.identity()));
    }

    public String currency() {
        return repo.findAll().stream().map(PricingDocument::currency).findFirst()
                .orElse(props.billing().defaultCurrency());
    }

    /**
     * Полная замена таблицы цен (PUT). Модели, которых нет в запросе, удаляются.
     * Одна валюта на всю таблицу, чтобы суммы стоимости были сопоставимы.
     */
    public synchronized PricingTable replace(List<PriceInput> items) {
        Set<String> seen = new HashSet<>();
        for (PriceInput i : items) {
            if (!seen.add(i.model())) {
                throw new GatewayException(HttpStatus.BAD_REQUEST, "duplicate_model",
                        "Model '" + i.model() + "' is listed more than once");
            }
        }
        if (items.stream().map(PriceInput::currency).distinct().count() > 1) {
            throw new GatewayException(HttpStatus.UNPROCESSABLE_ENTITY, "mixed_currencies",
                    "All prices must use the same currency");
        }

        PricingTable before = table();
        Map<String, PricingDocument> existing = byModel();
        Instant now = Instant.now();
        List<PricingDocument> next = items.stream().map(i -> {
            PricingDocument old = existing.get(i.model());
            boolean same = old != null && old.inputPricePer1m() == i.inputPricePer1m()
                    && old.outputPricePer1m() == i.outputPricePer1m() && old.currency().equals(i.currency());
            return new PricingDocument(i.model(), i.inputPricePer1m(), i.outputPricePer1m(), i.currency(),
                    same ? old.updatedAt() : now);
        }).toList();

        repo.deleteAll();
        repo.saveAll(next);

        PricingTable after = table();
        audit.record("pricing.update", "pricing", "pricing", before, after);
        return after;
    }

    private PricingTable toTable(List<PricingDocument> docs) {
        String currency = docs.isEmpty() ? props.billing().defaultCurrency() : docs.get(0).currency();
        return new PricingTable(currency, docs.stream().map(d -> new PriceItem(
                d.model(), d.inputPricePer1m(), d.outputPricePer1m(), d.currency(), d.updatedAt())).toList());
    }
}
