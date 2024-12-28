package com.gordev.assistant.service;

import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ChatService {
    private final WebClient webClient;

    public ChatService(@Qualifier("webClientBuilderWithLogging") WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Retry(name = "openai-api", fallbackMethod = "fallbackChatResponse")
    public Mono<String> generateChatResponse(String userMessage){
        String requestBody = """
            {
              "model": "gpt-4",
              "messages": [
                {"role": "system", "content": "You are a helpful assistant."},
                {"role": "user", "content": "%s"}
              ],
              "max_tokens": 1000
            }
            """.formatted(userMessage);

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> System.err.println("Error: " + error.getMessage()));
    }

    public Mono<String> fallbackChatResponse(String userMessage, Throwable t) {
        return Mono.just("I'm unable to respond right now. Please try again later.");
    }
}
