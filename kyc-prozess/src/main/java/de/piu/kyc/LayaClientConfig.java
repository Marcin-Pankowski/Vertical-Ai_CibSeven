package de.piu.kyc;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class LayaClientConfig {

    @Bean
    LayaClient layaClient(RestClient.Builder builder,
                          @Value("${laya.url}") String url,
                          @Value("${laya.connect-timeout}") Duration connectTimeout,
                          @Value("${laya.read-timeout}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        RestClient client = builder.baseUrl(url).requestFactory(requestFactory).build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(client)).build().createClient(LayaClient.class);
    }
}
