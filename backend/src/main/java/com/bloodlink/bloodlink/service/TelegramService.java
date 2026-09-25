package com.bloodlink.bloodlink.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);
    private static final String DEFAULT_FALLBACK_CHAT_ID = "5339809045";

    private final String token;
    private final String defaultChatId;
    private final RestTemplate restTemplate;

    public TelegramService(@Value("${bloodlink.telegram.bot-token:}") String token,
                           @Value("${bloodlink.telegram.default-chat-id:}") String defaultChatId) {
        this.token = token != null ? token.trim() : "";
        this.defaultChatId = (defaultChatId != null && !defaultChatId.isBlank()) ? defaultChatId.trim() : DEFAULT_FALLBACK_CHAT_ID;
        
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(requestFactory);

        log.info("TelegramService initialized — token present: {}, default chat ID: {}",
                !this.token.isBlank(), this.defaultChatId);
    }

    public boolean sendMessage(String text) {
        return sendMessage(defaultChatId, text);
    }

    public boolean sendMessage(String chatId, String text) {
        if (token == null || token.isBlank()) {
            log.warn("Telegram bot token is blank — skipping message send");
            return false;
        }

        String targetChatId = (chatId != null && !chatId.isBlank()) ? chatId.trim() : defaultChatId;
        if (targetChatId == null || targetChatId.isBlank()) {
            targetChatId = DEFAULT_FALLBACK_CHAT_ID;
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
            return true;
        } catch (Exception ex) {
            log.error("Telegram send failed for chat {}: {}", targetChatId, ex.getMessage(), ex);
            return false;
        }
    }
}