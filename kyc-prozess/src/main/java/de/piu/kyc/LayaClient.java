package de.piu.kyc;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/** REST-Client für den Laya-Dienst (laya_kyc/laya_service.py). URL: laya.url, siehe {@link LayaClientConfig}. */
@HttpExchange(contentType = MediaType.APPLICATION_JSON_VALUE, accept = MediaType.APPLICATION_JSON_VALUE)
public interface LayaClient {

    @PostExchange("/assessment")
    LlmResult assess(@RequestBody Map<String, Object> request);

    @GetExchange("/health")
    Map<String, Object> health();
}
