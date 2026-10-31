package ru.inno.itpd.gateway.cucumber;

import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileSystemUtils;

import ru.inno.itpd.gateway.PluginManagerClient;
import ru.inno.itpd.gateway.PluginRepository;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

public class Hooks {
    @Autowired private PluginRepository repository;
    @Autowired private PluginManagerClient pluginManager;

    @Before
    public void resetState() throws IOException {
        repository.deleteAll();
        FileSystemUtils.deleteRecursively(CucumberSpringConfig.PLUGINS_DIR);

        // by default the plugin manager passes everything through unchanged
        reset(pluginManager);
        doAnswer(inv -> inv.getArgument(0)).when(pluginManager).filterRequest(any(), anyList());
        doAnswer(inv -> inv.getArgument(1)).when(pluginManager).filterResponse(any(), any(), anyList());
    }
}