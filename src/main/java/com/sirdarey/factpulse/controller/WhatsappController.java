package com.sirdarey.factpulse.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirdarey.factpulse.service.AIOrchestrationService;
import com.sirdarey.factpulse.service.WhatsappService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
public class WhatsappController {

    @Value("${ACCESS_TOKEN}")
    private String ACCESS_TOKEN;

    @Value("${GRAPH_API_URL}")
    private String GRAPH_API_URL;

    @Value("${PHONE_NUMBER_ID}")
    private String PHONE_NUMBER_ID;

    @Value("${VERIFY_TOKEN}")
    private String VERIFY_TOKEN;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final WhatsappService whatsappService;
    private final AIOrchestrationService aiOrchestrationService;



    // ✅ Webhook verification for WhatsApp Cloud API
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        log.info("Received verification request - mode: {}, token: {}, challenge: {}", mode, token, challenge);

        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            log.info("Webhook verified successfully!");
            return ResponseEntity.ok(challenge);
        } else {
            log.warn("Webhook verification failed. mode={}, token={}", mode, token);
            return ResponseEntity.status(403).body("Verification failed");
        }
    }


    // ✅ Handle Incoming WhatsApp Messages
    @PostMapping
    public ResponseEntity<String> receiveMetaMessage(@RequestBody String payload){
        try {
            JsonNode json = objectMapper.readTree(payload);
            JsonNode messagesNode = json.at("/entry/0/changes/0/value/messages");

            if (!messagesNode.isArray() || messagesNode.isEmpty()) {
                return ResponseEntity.ok("EVENT_RECEIVED");
            }

            JsonNode message = messagesNode.get(0);
            String from = message.get("from").asText();
            String text = message.has("text") ? message.get("text").get("body").asText() : null;
            String messageId = message.has("id") ? message.get("id").asText() : null;

            log.info("💬 Message from {}: {}", from, text);

            if (text == null) {
                return ResponseEntity.ok("EVENT_RECEIVED");
            }

            // Optional: Mark as read
            // whatsappService.markAsRead(messageId);

            // 1. Analyze the prompt
            String aiResponse = aiOrchestrationService.analyze(from, text);
            log.info("Controller[{}] :: AI Response: {}", from, aiResponse);

            // 2. Send the immediate reply
            whatsappService.sendWhatsAppReply(from, aiResponse);

        } catch (Exception e) {
            log.error("❌ WhatsappController Exception :: {}", e.getMessage());
        }

        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}