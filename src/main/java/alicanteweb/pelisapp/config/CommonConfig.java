package alicanteweb.pelisapp.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class CommonConfig {

    @Value("${app.moderation.ollama.timeout:30}")
    private int ollamaTimeoutSeconds;

    // Proveer WebClient.Builder si alguna clase lo inyecta directamente
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    // ObjectMapper personalizado para evitar problemas con fechas y JavaTime
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return om;
    }

    // RestTemplate con timeouts para evitar que hilos HTTP queden bloqueados
    @Bean
    public RestTemplate restTemplate() {
        int timeoutMs = ollamaTimeoutSeconds * 1000;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }
}
