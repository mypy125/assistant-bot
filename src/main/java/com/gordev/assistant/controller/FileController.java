package com.gordev.assistant.controller;

import com.gordev.assistant.service.FileProcessingService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/file")
public class FileController {
    private final FileProcessingService fileProcessingService;

    public FileController(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> processFile(@RequestPart("file") MultipartFile file) {
        String contentType = file.getContentType();

        try {
            InputStream inputStream = file.getInputStream();
            return switch (contentType) {
                case "text/csv" -> fileProcessingService.processCsv(inputStream);
                case "application/pdf" -> fileProcessingService.processPdf(inputStream);
                case "application/msword",
                     "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                        fileProcessingService.processDoc(inputStream);
                default -> Mono.error(new RuntimeException("Unsupported file format " + contentType));
            };
        } catch (IOException e) {
            return Mono.error(new RuntimeException("Error reading file " + e.getMessage(), e));
        }
    }
}
