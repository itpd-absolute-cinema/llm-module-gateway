package ru.inno.itpd.gateway.core.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.inno.itpd.gateway.core.dao.repository.BlockedUserRepository;
import ru.inno.itpd.gateway.core.exception.GatewayException;

/** Проверка блокировки на каждый запрос: прямое чтение из Mongo, без кэша — блокировка действует мгновенно. */
@Service
public class BlockService {
    private final BlockedUserRepository repo;

    public BlockService(BlockedUserRepository repo) {
        this.repo = repo;
    }

    public void assertNotBlocked(String userId) {
        if (repo.existsById(userId)) {
            throw new GatewayException(HttpStatus.FORBIDDEN, "user_blocked", "The user is blocked");
        }
    }
}
