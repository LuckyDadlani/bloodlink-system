package com.bloodlink.bloodlink.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "donor_notifications")
public class DonorNotification {

    @Id
    @Column(name = "notification_id")
    private UUID notificationId;

    @Column(name = "emergency_id")
    private UUID emergencyId;

    @Column(name = "donor_id")
    private UUID donorId;

    @Column(name = "batch_number")
    private int batchNumber;

    @Column(name = "ai_rank_at_time")
    private Integer aiRankAtTime;

    @Column(name = "ai_probability_score")
    private BigDecimal aiProbabilityScore;

    @Column(name = "whatsapp_message_id")
    private String whatsappMessageId;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivery_status", columnDefinition = "delivery_status_enum")
    @ColumnTransformer(write = "?::delivery_status_enum")
    private String deliveryStatus;

    @Column(name = "response_received", columnDefinition = "response_enum")
    @ColumnTransformer(write = "?::response_enum")
    private String responseReceived;

    @Column(name = "response_received_at")
    private Instant responseReceivedAt;

    @Column(name = "response_time_minutes")
    private BigDecimal responseTimeMinutes;

    @Column(name = "was_selected")
    private boolean wasSelected;

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    public UUID getEmergencyId() {
        return emergencyId;
    }

    public void setEmergencyId(UUID emergencyId) {
        this.emergencyId = emergencyId;
    }

    public UUID getDonorId() {
        return donorId;
    }

    public void setDonorId(UUID donorId) {
        this.donorId = donorId;
    }

    public int getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(int batchNumber) {
        this.batchNumber = batchNumber;
    }

    public Integer getAiRankAtTime() {
        return aiRankAtTime;
    }

    public void setAiRankAtTime(Integer aiRankAtTime) {
        this.aiRankAtTime = aiRankAtTime;
    }

    public BigDecimal getAiProbabilityScore() {
        return aiProbabilityScore;
    }

    public void setAiProbabilityScore(BigDecimal aiProbabilityScore) {
        this.aiProbabilityScore = aiProbabilityScore;
    }

    public String getWhatsappMessageId() {
        return whatsappMessageId;
    }

    public void setWhatsappMessageId(String whatsappMessageId) {
        this.whatsappMessageId = whatsappMessageId;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public String getResponseReceived() {
        return responseReceived;
    }

    public void setResponseReceived(String responseReceived) {
        this.responseReceived = responseReceived;
    }

    public Instant getResponseReceivedAt() {
        return responseReceivedAt;
    }

    public void setResponseReceivedAt(Instant responseReceivedAt) {
        this.responseReceivedAt = responseReceivedAt;
    }

    public BigDecimal getResponseTimeMinutes() {
        return responseTimeMinutes;
    }

    public void setResponseTimeMinutes(BigDecimal responseTimeMinutes) {
        this.responseTimeMinutes = responseTimeMinutes;
    }

    public boolean isWasSelected() {
        return wasSelected;
    }

    public void setWasSelected(boolean wasSelected) {
        this.wasSelected = wasSelected;
    }
}
