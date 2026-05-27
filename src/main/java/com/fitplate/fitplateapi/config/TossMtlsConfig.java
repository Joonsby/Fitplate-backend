package com.fitplate.fitplateapi.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.security.KeyStore;

@Configuration
public class TossMtlsConfig {

    @Bean(name = "tossRestClient")
    public RestClient tossRestClient(
            @Value("${toss.api.base-url}") String baseUrl,
            @Value("${toss.mtls.cert-path}") Resource certResource,
            @Value("${toss.mtls.cert-password}") String certPassword
    ) throws Exception {

        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (var inputStream = certResource.getInputStream()) {
            keyStore.load(inputStream, certPassword.toCharArray());
        }

        SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(keyStore, certPassword.toCharArray())
                .build();

        SSLConnectionSocketFactory sslSocketFactory =
                SSLConnectionSocketFactoryBuilder.create()
                        .setSslContext(sslContext)
                        .build();

        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(sslSocketFactory)
                        .build();

        HttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .evictExpiredConnections()
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}