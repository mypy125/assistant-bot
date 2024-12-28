package com.gordev.assistant.controller;

import com.gordev.assistant.service.ChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/respond")
    public Mono<String> getChatResponse(@RequestBody String userMessage) {
        return chatService.generateChatResponse(userMessage);
    }
}
