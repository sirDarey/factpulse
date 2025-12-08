package com.sirdarey.factpulse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsappService {

    @Value("${ACCESS_TOKEN}")
    private String ACCESS_TOKEN;

    @Value("${GRAPH_API_URL}")
    private String GRAPH_API_URL;

    @Value("${PHONE_NUMBER_ID}")
    private String PHONE_NUMBER_ID;


    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OpenAIService openAIService;



    @PostMapping
    public void processMessage(@RequestBody String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            JsonNode messagesNode = json.at("/entry/0/changes/0/value/messages");

            if (!messagesNode.isArray() || messagesNode.isEmpty()) {
                log.error("\nprocessMessage :: Invalid messagesNode :: {}", messagesNode);
                return;
            }

            JsonNode message = messagesNode.get(0);
            String from = message.get("from").asText();
            String text = message.has("text") ? message.get("text").get("body").asText() : null;
            String messageId = message.has("id") ? message.get("id").asText() : null;

            log.info("💬 Message from {}: {}", from, text);
            log.info("💬 MESSAGE: {}", message);

            if (text == null) {
                log.error("\nprocessMessage :: Invalid message! text IS NULL");
//                    sendWhatsAppReply(from, AppConfig.APOLOGY_RESPONSE);
                return ;
            }

            // ✅ Step 1: Mark message as read & show typing bubble
            if (messageId != null) {
//                    markAsRead(messageId);
//                    sendTypingIndicator(from);
            }

            // ✅ Step 2: Process message using AI orchestration logic
//                String aiResponse = openAIService.analyze(text);
            String aiResponse = String.valueOf(openAIService.extractUserNameOrAsk(text));

            // ✅ Step 3: Send response back to user
//                sendWhatsAppReply(from, aiResponse);

        } catch (Exception e) {
            log.error("❌ processMessage Exception :: {}", e.getMessage());
        }
    }

    private void sendWhatsAppReply(String recipient, String messageText) {
        try {
            String url = GRAPH_API_URL + "/" + PHONE_NUMBER_ID + "/messages";

            ObjectNode bodyNode = objectMapper.createObjectNode();
            bodyNode.put("messaging_product", "whatsapp");
            bodyNode.put("to", recipient);
            bodyNode.put("type", "text");

            ObjectNode textNode = objectMapper.createObjectNode();
            textNode.put("body", messageText);
            bodyNode.set("text", textNode);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(ACCESS_TOKEN);

            HttpEntity<String> entity = new HttpEntity<>(bodyNode.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            log.info("📤 Reply sent to {}: {}", recipient, response.getStatusCode());
        } catch (Exception e) {
            log.error("❌ Error sending WhatsApp reply: {}", e.getMessage(), e);
        }
    }

    // ✅ Step 1: Mark incoming message as read
    private void markAsRead(String messageId) {
        try {
            String url = GRAPH_API_URL + "/" + PHONE_NUMBER_ID + "/messages";
            String body = """
                {
                  "messaging_product": "whatsapp",
                  "status": "read",
                  "message_id": "%s"
                }
            """.formatted(messageId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(ACCESS_TOKEN);

            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            log.info("✅ Marked message {} as read", messageId);
        } catch (Exception e) {
            log.error("⚠️ Error marking message as read: {}", e.getMessage(), e);
        }
    }

    // ✅ Step 2: Official Typing Indicator API Call (from Meta docs)
    // ✅ Updated: sendTypingIndicator() - fallback if sandbox rejects typing indicator
    private void sendTypingIndicator(String recipient) {
        try {
            String url = GRAPH_API_URL + "/" + PHONE_NUMBER_ID + "/messages";

            String body = """
            {
              "messaging_product": "whatsapp",
              "to": "%s",
              "type": "status",
              "status": { "typing": "on" }
            }
        """.formatted(recipient);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(ACCESS_TOKEN);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);

            log.info("⌛ Typing indicator sent to {}: {}", recipient, response.getStatusCode());
        } catch (Exception e) {
            // 🧩 Sandbox will throw 400; log and continue
            log.warn("⚠️ Typing indicator not supported in sandbox. Continuing... {}", e.getMessage());
        }
    }
}