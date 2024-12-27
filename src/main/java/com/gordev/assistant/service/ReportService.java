package com.gordev.assistant.service;

import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ReportService {
    private final WebClient webClient;

    public ReportService(@Qualifier("webClientBuilderWithLogging") WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Retry(name = "openai-api", fallbackMethod = "fallbackGenerateReport")
    public Mono<String> generateResponse(String prompt){
        return webClient.post()
                .uri("/completions")
                .bodyValue("{\n  \"model\": \"gpt-4\",\n  \"prompt\": \"" + prompt + "\",\n  \"max_tokens\": 500\n}")
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error-> System.err.println("Error "+ error.getMessage()));
    }

    public Mono<String>fallbackGenerateReport(String report, Throwable t){
        return Mono.just("Fallback response due to error "+ t.getMessage());
    }

}

