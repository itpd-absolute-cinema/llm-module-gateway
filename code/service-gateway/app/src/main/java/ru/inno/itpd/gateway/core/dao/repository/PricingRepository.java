package ru.inno.itpd.gateway.core.dao.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.inno.itpd.gateway.core.dao.entity.PricingDocument;

public interface PricingRepository extends MongoRepository<PricingDocument, String> {
}
