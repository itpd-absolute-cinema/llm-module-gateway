package ru.inno.itpd.gateway;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PluginRepository extends MongoRepository<PluginDocument, String> {
    List<PluginDocument> findAllByOrderByPositionAsc();
}