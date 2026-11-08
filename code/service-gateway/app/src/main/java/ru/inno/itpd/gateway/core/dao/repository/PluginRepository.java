package ru.inno.itpd.gateway.core.dao.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.inno.itpd.gateway.core.dao.entity.PluginDocument;

import java.util.List;

public interface PluginRepository extends MongoRepository<PluginDocument, String> {
    List<PluginDocument> findAllByOrderByPositionAsc();
}