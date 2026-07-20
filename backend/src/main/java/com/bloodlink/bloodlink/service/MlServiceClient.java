package com.bloodlink.bloodlink.service;

import com.bloodlink.bloodlink.dto.RankedDonorDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MlServiceClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String mlBaseUrl;

    public MlServiceClient(@Value("${bloodlink.ml.base-url}") String mlBaseUrl) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.mlBaseUrl = mlBaseUrl;
    }

    public List<RankedDonorDto> rankDonors(UUID emergencyId) {
        String url = mlBaseUrl + "/api/ai/rank-donors";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("emergency_id", emergencyId.toString()), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        List<RankedDonorDto> rankedDonors = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode ranked = root.path("ranked_donors");
            if (ranked.isArray()) {
                for (JsonNode donor : ranked) {
                    String donorId = donor.path("donor_id").asText(null);
                    if (donorId == null) {
                        continue;
                    }
                    BigDecimal score = donor.hasNonNull("score")
                        ? donor.path("score").decimalValue()
                        : donor.hasNonNull("probability") ? donor.path("probability").decimalValue() : BigDecimal.ZERO;
                    rankedDonors.add(new RankedDonorDto(UUID.fromString(donorId), score));
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse ML ranking response", ex);
        }

        return rankedDonors;
    }
}
