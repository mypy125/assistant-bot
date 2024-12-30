package com.gordev.assistant.controller;

import com.gordev.assistant.service.ReportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/report")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/generate")
    public Mono<String> generateReport(@RequestPart(value = "report", required = false) String report,
                                        @RequestPart(value = "file", required = false) MultipartFile file)
    {
        return reportService.generateResponse(report, file);
    }
}
