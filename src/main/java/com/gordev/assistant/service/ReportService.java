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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final OpenAiProperties prop;

    private final WebClient webClient;
    private final FileProcessingService fileProcessingService;

    @Autowired
    public ReportService(OpenAiProperties prop,
                         @Qualifier("webClientBuilderWithLogging") WebClient.Builder webClientBuilder,
                         FileProcessingService fileProcessingService)
    {
        this.prop = prop;
        this.webClient = webClientBuilder
                .baseUrl(prop.getApiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer "+prop.getApiKey())
                .build();
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
        String requestBody = """
            {
                "model": "gpt-4",
                "messages": [
                    {
                        "role": "user",
                        "content": "%s"
                    }
                ]
            }
            """.formatted(prompt);

        System.out.println("Request Body: " + requestBody);

        return webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + prop.getApiKey())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> System.err.println("Error sending to OpenAI: " + error.getMessage()));
    }
}

