package com.bloodlink.bloodlink.repository;

import com.bloodlink.bloodlink.model.BloodInventory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloodInventoryRepository extends JpaRepository<BloodInventory, UUID> {
    List<BloodInventory> findByBloodBankIdOrderByBloodGroupAscComponentTypeAsc(UUID bloodBankId);
}
