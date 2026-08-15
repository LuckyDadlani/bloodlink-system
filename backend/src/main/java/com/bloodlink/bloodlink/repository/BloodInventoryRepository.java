package com.bloodlink.bloodlink.repository;

import com.bloodlink.bloodlink.model.BloodInventory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BloodInventoryRepository extends JpaRepository<BloodInventory, UUID> {
    List<BloodInventory> findByBloodBankIdOrderByBloodGroupAscComponentTypeAsc(UUID bloodBankId);

    @Query(value = """
        SELECT * FROM blood_inventory
        WHERE blood_bank_id = :bloodBankId
          AND blood_group = CAST(:bloodGroup AS blood_group_enum)
          AND component_type = CAST(:componentType AS component_type_enum)
        """, nativeQuery = true)
    Optional<BloodInventory> findByBankAndGroupAndComponent(
        @Param("bloodBankId") UUID bloodBankId,
        @Param("bloodGroup") String bloodGroup,
        @Param("componentType") String componentType);
}

