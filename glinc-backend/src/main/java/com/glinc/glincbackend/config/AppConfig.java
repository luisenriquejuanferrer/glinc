package com.glinc.glincbackend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(BridgeProperties.class)
public class AppConfig {

    @Bean
    public RestClient bridgeRestClient(BridgeProperties props) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(props.getUrl() + "/v1");

        String token = props.getServiceToken();
        if (token != null && !token.isEmpty()) {
            builder.defaultHeader("Authorization", "Bearer " + token);
        }

        return builder.build();
    }
}
