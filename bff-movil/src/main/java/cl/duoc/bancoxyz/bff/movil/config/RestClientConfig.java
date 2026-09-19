package cl.duoc.bancoxyz.bff.movil.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${bank.core.url:http://localhost:8080/api/core}")
    private String coreUrl;

    @Bean
    public RestClient coreRestClient() {
        return RestClient.builder()
                .baseUrl(coreUrl)
                .build();
    }
}
