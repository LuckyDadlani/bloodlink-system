package com.bloodlink.bloodlink.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "emergency_requests")
public class EmergencyRequest {

    @Id
    @Column(name = "emergency_id")
    private UUID emergencyId;

    @Column(name = "hospital_id")
    private UUID hospitalId;

    // ✅ STRING (IMPORTANT)
    @Column(name = "blood_group_required", columnDefinition = "blood_group_enum")
    @ColumnTransformer(write = "?::blood_group_enum")
    private String bloodGroupRequired;

    @Column(name = "component_required", columnDefinition = "component_type_enum")
    @ColumnTransformer(write = "?::component_type_enum")
    private String componentRequired;

    @Column(name = "units_required")
    private int unitsRequired;

    @Column(name = "urgency_level", columnDefinition = "urgency_level_enum")
    @ColumnTransformer(write = "?::urgency_level_enum")
    private String urgencyLevel;

    @Column(name = "hospital_latitude")
    private BigDecimal hospitalLatitude;

    @Column(name = "hospital_longitude")
    private BigDecimal hospitalLongitude;

    @Column(name = "status", columnDefinition = "emergency_status_enum")
    @ColumnTransformer(write = "?::emergency_status_enum")
    private String status;

    @Column(name = "fulfilled_by_bank_id")
    private UUID fulfilledByBankId;

    @Column(name = "fulfilled_by_donor_id")
    private UUID fulfilledByDonorId;

    @Column(name = "units_fulfilled")
    private int unitsFulfilled;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "donor_notification_started_at")
    private Instant donorNotificationStartedAt;

    @Column(name = "first_donor_response_at")
    private Instant firstDonorResponseAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "total_donors_notified")
    private int totalDonorsNotified;

    @Column(name = "hospital_city")
    private String hospitalCity;

    // GETTERS & SETTERS

    public UUID getEmergencyId() { return emergencyId; }
    public void setEmergencyId(UUID emergencyId) { this.emergencyId = emergencyId; }

    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }

    public String getBloodGroupRequired() { return bloodGroupRequired; }
    public void setBloodGroupRequired(String bloodGroupRequired) { this.bloodGroupRequired = bloodGroupRequired; }

    public String getComponentRequired() { return componentRequired; }
    public void setComponentRequired(String componentRequired) { this.componentRequired = componentRequired; }

    public int getUnitsRequired() { return unitsRequired; }
    public void setUnitsRequired(int unitsRequired) { this.unitsRequired = unitsRequired; }

    public String getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(String urgencyLevel) { this.urgencyLevel = urgencyLevel; }

    public BigDecimal getHospitalLatitude() { return hospitalLatitude; }
    public void setHospitalLatitude(BigDecimal hospitalLatitude) { this.hospitalLatitude = hospitalLatitude; }

    public BigDecimal getHospitalLongitude() { return hospitalLongitude; }
    public void setHospitalLongitude(BigDecimal hospitalLongitude) { this.hospitalLongitude = hospitalLongitude; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public UUID getFulfilledByBankId() { return fulfilledByBankId; }
    public void setFulfilledByBankId(UUID fulfilledByBankId) { this.fulfilledByBankId = fulfilledByBankId; }

    public UUID getFulfilledByDonorId() { return fulfilledByDonorId; }
    public void setFulfilledByDonorId(UUID fulfilledByDonorId) { this.fulfilledByDonorId = fulfilledByDonorId; }

    public int getUnitsFulfilled() { return unitsFulfilled; }
    public void setUnitsFulfilled(int unitsFulfilled) { this.unitsFulfilled = unitsFulfilled; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getDonorNotificationStartedAt() { return donorNotificationStartedAt; }
    public void setDonorNotificationStartedAt(Instant donorNotificationStartedAt) { this.donorNotificationStartedAt = donorNotificationStartedAt; }

    public Instant getFirstDonorResponseAt() { return firstDonorResponseAt; }
    public void setFirstDonorResponseAt(Instant firstDonorResponseAt) { this.firstDonorResponseAt = firstDonorResponseAt; }

    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }

    public int getTotalDonorsNotified() { return totalDonorsNotified; }
    public void setTotalDonorsNotified(int totalDonorsNotified) { this.totalDonorsNotified = totalDonorsNotified; }

    public String getHospitalCity() { return hospitalCity; }
    public void setHospitalCity(String hospitalCity) { this.hospitalCity = hospitalCity; }
}