package com.gordev.assistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.logging.Logger;


@Configuration
public class WebClientLoggingConfig {
    private static final Logger logger = Logger.getLogger(WebClientLoggingConfig.class.getName());

    @Bean(name = "webClientBuilderWithLogging")
    public WebClient.Builder webClientBuilder(){
        return WebClient.builder()
                .filter(logRequest())
                .filter(logResponse());
    }

    private ExchangeFilterFunction logRequest(){
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            logger.info("Request "+ clientRequest.method()+" "+clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse(){
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            logger.info("Response "+ clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}
