package com.bloodlink.bloodlink.repository;

import com.bloodlink.bloodlink.model.BloodBankProfile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloodBankProfileRepository extends JpaRepository<BloodBankProfile, UUID> {
}
