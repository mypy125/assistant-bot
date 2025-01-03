package com.gordev.assistant.service.impl;

import com.gordev.assistant.config.OpenAiProperties;
import com.gordev.assistant.service.FileProcessingService;
import com.opencsv.CSVReader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileProcessingServiceImpl implements FileProcessingService {
    private final WebClient webClient;
    private final OpenAiProperties prop;

    @Autowired
    public FileProcessingServiceImpl(@Qualifier("webClientBuilderWithLogging") WebClient.Builder webClientBuilder, OpenAiProperties prop) {
       this.webClient = webClientBuilder
               .baseUrl(prop.getApiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer "+prop.getApiKey())
                .build();
        this.prop = prop;
    }

    @Override
    public Mono<String> processCsv(InputStream inputStream) {
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream))) {

            List<String[]> rows = csvReader.readAll();
            String prompt = rows.stream()
                    .map(row -> String.join(", ", row))
                    .collect(Collectors.joining("\n"));

            return sendToOpenAI("Processed CSV data:\n" + prompt);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Error processing CSV " + e.getMessage(), e));
        }
    }

    @Override
    public Mono<String> processPdf(InputStream inputStream) {
        try (PDDocument document = PDDocument.load(inputStream)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            return sendToOpenAI("Processed PDF data:\n" + text);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Error processing PDF: " + e.getMessage(), e));
        }
    }

    @Override
    public Mono<String> processDoc(InputStream inputStream) {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            String text = document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));

            return sendToOpenAI("Processed DOC/DOCX data:\n" + text);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Error processing DOC/DOCX: " + e.getMessage(), e));
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
