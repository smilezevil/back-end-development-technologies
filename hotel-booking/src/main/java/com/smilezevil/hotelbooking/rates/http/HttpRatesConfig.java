package com.smilezevil.hotelbooking.rates.http;

import com.smilezevil.hotelbooking.rates.RatesProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;

@Configuration
@ConditionalOnProperty(name = "rates.client", havingValue = "http")
@EnableConfigurationProperties(RatesProperties.class)
public class HttpRatesConfig {

    @Bean
    public HttpRatesAdapter httpRatesAdapter(RatesProperties properties) {
        return new HttpRatesAdapter(createClient(properties.rest()));
    }

    public static ExchangeRateHttpClient createClient(RatesProperties.Rest rest) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(rest.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(rest.readTimeout());
        RestClient restClient = RestClient.builder()
                .baseUrl(rest.url())
                .requestFactory(requestFactory)
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(ExchangeRateHttpClient.class);
    }
}