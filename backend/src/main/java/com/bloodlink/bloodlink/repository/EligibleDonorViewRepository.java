package com.bloodlink.bloodlink.repository;

import com.bloodlink.bloodlink.model.EligibleDonorView;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EligibleDonorViewRepository extends JpaRepository<EligibleDonorView, UUID> {
    @Query(value = """
    SELECT * FROM vw_eligible_donors
    WHERE blood_group = CAST(:bloodGroup AS blood_group_enum)
""", nativeQuery = true)
List<EligibleDonorView> findByBloodGroup(@Param("bloodGroup") String bloodGroup);
}
