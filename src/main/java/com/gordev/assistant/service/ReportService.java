package com.gordev.assistant.service;

import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;

@Service
public class ReportService {
    private final WebClient webClient;
    private final FileProcessingService fileProcessingService;

    public ReportService(@Qualifier("webClientBuilderWithLogging") WebClient.Builder webClientBuilder, FileProcessingService fileProcessingService) {
        this.webClient = webClientBuilder.build();
        this.fileProcessingService = fileProcessingService;
    }

    @Retry(name = "openai-api", fallbackMethod = "fallbackGenerateReport")
    public Mono<String> generateResponse(String prompt, MultipartFile file){
        if (file != null) {
            return processFile(file)
             .flatMap(fileContent -> {
                String combinedPrompt = prompt + "\n" + "File content: " + fileContent;
                return sendToOpenAI(combinedPrompt);
            });
        }
        return sendToOpenAI(prompt);
    }

    public Mono<String>fallbackGenerateReport(String report, Throwable t){
        return Mono.just("Fallback response due to error "+ t.getMessage());
    }

    private Mono<String> processFile(MultipartFile file) {
        String contentType = file.getContentType();
        try (InputStream inputStream = file.getInputStream()) {
            assert contentType != null;
            return switch (contentType) {
                case "text/csv" -> fileProcessingService.processCsv(inputStream);
                case "application/pdf" -> fileProcessingService.processPdf(inputStream);
                case "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                        fileProcessingService.processDoc(inputStream);
                default -> Mono.error(new RuntimeException("Unsupported file format " + contentType));
            };
        } catch (IOException e) {
            return Mono.error(new RuntimeException("Error reading file " + e.getMessage(), e));
        }
    }

    private Mono<String> sendToOpenAI(String prompt) {
        return webClient.post()
                .uri("/completions")
                .bodyValue("{\n  \"model\": \"gpt-4\",\n  \"prompt\": \"" + prompt + "\",\n  \"max_tokens\": 500\n}")
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> System.err.println("Error sending to OpenAI " + error.getMessage()));
    }

}

