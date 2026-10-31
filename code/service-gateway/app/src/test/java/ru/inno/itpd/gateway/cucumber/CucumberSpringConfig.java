package ru.inno.itpd.gateway.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.mongodb.MongoDBContainer;

import ru.inno.itpd.gateway.PluginManagerClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "gateway.admin-api-key=" + CucumberSpringConfig.ADMIN_KEY,
        "gateway.llm.provider=stub"
})
public class CucumberSpringConfig {
    public static final String ADMIN_KEY = "test-key";
    public static final Path PLUGINS_DIR = createTempDir();

    private static final MongoDBContainer MONGO = new MongoDBContainer("mongo:8");

    static {
        MONGO.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> MONGO.getReplicaSetUrl("gateway"));
        registry.add("gateway.plugins-dir", PLUGINS_DIR::toString);
    }

    /** The Plugin Manager is an external service: tests replace it with a mock. */
    @MockitoBean
    PluginManagerClient pluginManager;

    private static Path createTempDir() {
        try {
            return Files.createTempDirectory("gateway-plugins-it");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}