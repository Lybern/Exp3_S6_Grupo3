package cl.duoc.bancoxyz.bff.cajero.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    @Value("${bank.core.url:http://core-service/api/core}")
    private String coreUrl;

    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RestClient coreRestClient(RestTemplate loadBalancedRestTemplate) {
        return RestClient.builder(loadBalancedRestTemplate)
                .baseUrl(coreUrl)
                .build();
    }
}
