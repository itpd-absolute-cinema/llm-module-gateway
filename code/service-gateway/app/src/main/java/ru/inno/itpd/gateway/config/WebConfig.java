package ru.inno.itpd.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ru.inno.itpd.gateway.config.properties.GatewayProperties;

/** SSE и CSV отдаются через StreamingResponseBody: нужен свой executor и таймаут больше дефолтных 30 с Tomcat. */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final GatewayProperties props;

    public WebConfig(GatewayProperties props) {
        this.props = props;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        var executor = new SimpleAsyncTaskExecutor("gateway-stream-");
        executor.setVirtualThreads(true);
        configurer.setTaskExecutor(executor);
        configurer.setDefaultTimeout(props.streaming().timeout().toMillis());
    }
}
