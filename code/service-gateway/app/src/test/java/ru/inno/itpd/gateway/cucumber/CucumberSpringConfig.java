package ru.inno.itpd.gateway.cucumber;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.inno.itpd.gateway.PluginManagerClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
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

    private static final MongoServer MONGO = new MongoServer(new MemoryBackend());
    private static final InetSocketAddress MONGO_ADDRESS = MONGO.bind();   // random free port on localhost

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () ->
                "mongodb://" + MONGO_ADDRESS.getHostString() + ":" + MONGO_ADDRESS.getPort() + "/gateway");
        registry.add("gateway.plugins-dir", PLUGINS_DIR::toString);
    }

    /**
     * The Plugin Manager is an external service: tests replace it with a mock.
     */
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