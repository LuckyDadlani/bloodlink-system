package com.bloodlink.bloodlink.repository;

import com.bloodlink.bloodlink.model.BankRequestDismissal;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankRequestDismissalRepository extends JpaRepository<BankRequestDismissal, UUID> {
    boolean existsByBloodBankIdAndEmergencyId(UUID bloodBankId, UUID emergencyId);
}
