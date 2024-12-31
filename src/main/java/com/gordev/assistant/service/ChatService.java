package com.gordev.assistant.service;

import com.gordev.assistant.config.OpenAiProperties;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final OpenAiProperties prop;
    private final WebClient webClient;

    @Autowired
    public ChatService(OpenAiProperties prop,
                       @Qualifier("webClientBuilderWithLogging") WebClient.Builder webClientBuilder)
    {
        this.prop = prop;
        this.webClient = webClientBuilder
                .baseUrl(prop.getApiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer "+prop.getApiKey())
                .build();
    }

    @Retry(name = "openai-api", fallbackMethod = "fallbackChatResponse")
    public Mono<String> generateChatResponse(String userMessage) {
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

        System.out.println("Request Body: " + requestBody);

        return webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + prop.getApiKey())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> System.err.println("Error: " + error.getMessage()));
    }

    public Mono<String> fallbackChatResponse(String userMessage, Throwable t) {
        return Mono.just("I'm unable to respond right now. Please try again later.");
    }
}
