package com.gordev.assistant.service;

import reactor.core.publisher.Mono;
import java.io.InputStream;

public interface FileProcessingService {
    Mono<String> processCsv(InputStream inputStream);
    Mono<String> processPdf(InputStream inputStream);
    Mono<String> processDoc(InputStream inputStream);
}
