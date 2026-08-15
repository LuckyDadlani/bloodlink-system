package com.bloodlink.bloodlink.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blood_bank_request_dismissals")
public class BankRequestDismissal {

    @Id
    @Column(name = "dismissal_id")
    private UUID dismissalId;

    @Column(name = "blood_bank_id")
    private UUID bloodBankId;

    @Column(name = "emergency_id")
    private UUID emergencyId;

    @Column(name = "dismissed_at")
    private Instant dismissedAt;

    public UUID getDismissalId() {
        return dismissalId;
    }

    public void setDismissalId(UUID dismissalId) {
        this.dismissalId = dismissalId;
    }

    public UUID getBloodBankId() {
        return bloodBankId;
    }

    public void setBloodBankId(UUID bloodBankId) {
        this.bloodBankId = bloodBankId;
    }

    public UUID getEmergencyId() {
        return emergencyId;
    }

    public void setEmergencyId(UUID emergencyId) {
        this.emergencyId = emergencyId;
    }

    public Instant getDismissedAt() {
        return dismissedAt;
    }

    public void setDismissedAt(Instant dismissedAt) {
        this.dismissedAt = dismissedAt;
    }
}
