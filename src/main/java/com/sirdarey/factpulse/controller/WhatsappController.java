package com.sirdarey.factpulse.controller;

import com.sirdarey.factpulse.service.WhatsappService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
public class WhatsappController {

    @Value("${VERIFY_TOKEN}")
    private String VERIFY_TOKEN;

    private final WhatsappService whatsappService;



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
        whatsappService.processMessage(payload);
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}