package com.bloodlink.bloodlink.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);
    private static final String TEST_CHAT_ID = "5339809045";

    private final String token;
    private final String defaultChatId;
    private final RestTemplate restTemplate;

    public TelegramService(@Value("${bloodlink.telegram.bot-token}") String token,
                           @Value("${bloodlink.telegram.default-chat-id}") String defaultChatId) {
        this.token = token;
        this.defaultChatId = defaultChatId;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(requestFactory);

        log.info("TelegramService initialized — token present: {}, default chat ID: {}",
                token != null && !token.isBlank(), defaultChatId);
    }

    public void sendMessage(String text) {
        sendMessage(defaultChatId, text);
    }

    public void sendMessage(String chatId, String text) {
        if (token == null || token.isBlank()) {
            log.warn("Telegram bot token is blank — skipping message send");
            return;
        }

        String targetChatId = (chatId != null && !chatId.isBlank()) ? chatId : defaultChatId;
        if (targetChatId == null || targetChatId.isBlank()) {
            targetChatId = TEST_CHAT_ID;
        }
        if (targetChatId == null || targetChatId.isBlank()) {
            log.warn("No valid chat ID available — skipping message send");
            return;
        }

        String url = "https://api.telegram.org/bot" + token + "/sendMessage";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("chat_id", targetChatId);
        body.put("text", text);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            log.info("Sending Telegram message to chat {} (length: {} chars)", targetChatId, text.length());
            String response = restTemplate.postForObject(url, entity, String.class);
            log.info("Telegram API response: {}", response);
        } catch (Exception ex) {
            log.error("Telegram send failed for chat {}: {}", targetChatId, ex.getMessage(), ex);
        }
    }
}