package com.glinc.glincbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Mapea las propiedades bridge.* de application.properties.
@ConfigurationProperties(prefix = "bridge")
public class BridgeProperties {

    private String url;
    private String serviceToken;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServiceToken() {
        return serviceToken;
    }

    public void setServiceToken(String serviceToken) {
        this.serviceToken = serviceToken;
    }
}
