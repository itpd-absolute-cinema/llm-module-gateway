package ru.inno.itpd.gateway.core.dao.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.inno.itpd.gateway.core.dao.entity.BlockedUserDocument;

public interface BlockedUserRepository extends MongoRepository<BlockedUserDocument, String> {
}
