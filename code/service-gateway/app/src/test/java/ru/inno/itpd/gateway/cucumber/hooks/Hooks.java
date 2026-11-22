package ru.inno.itpd.gateway.cucumber.hooks;

import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.FileSystemUtils;

import ru.inno.itpd.gateway.core.client.keycloak.KeycloakAdminClient;
import ru.inno.itpd.gateway.core.client.keycloak.KeycloakAdminClient.KcUser;
import ru.inno.itpd.gateway.core.client.pluginmanager.PluginManagerClient;
import ru.inno.itpd.gateway.core.dao.repository.PluginRepository;
import ru.inno.itpd.gateway.cucumber.config.CucumberSpringConfig;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class Hooks {
    @Autowired private PluginRepository repository;
    @Autowired private PluginManagerClient pluginManager;
    @Autowired private KeycloakAdminClient keycloak;
    @Autowired private MongoTemplate mongo;

    @Before
    public void resetState() throws IOException {
        repository.deleteAll();
        for (String collection : List.of("blocked_users", "usage_events", "pricing", "audit_log")) {
            mongo.remove(new Query(), collection);
        }
        FileSystemUtils.deleteRecursively(CucumberSpringConfig.PLUGINS_DIR);

        // by default the plugin manager passes everything through unchanged
        reset(pluginManager);
        doAnswer(inv -> inv.getArgument(0)).when(pluginManager).filterRequest(any(), anyList());
        doAnswer(inv -> inv.getArgument(1)).when(pluginManager).filterResponse(any(), any(), anyList());

        // by default Keycloak is configured and knows every user as an enabled account
        reset(keycloak);
        when(keycloak.isConfigured()).thenReturn(true);
        when(keycloak.find(anyString())).thenAnswer(inv -> {
            String id = inv.getArgument(0);
            return Optional.of(new KcUser(id, "name-" + id, id + "@example.test", null, null, true));
        });
    }
}
