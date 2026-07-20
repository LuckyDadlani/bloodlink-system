package com.bloodlink.bloodlink.repository;

import com.bloodlink.bloodlink.model.HospitalProfile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalProfileRepository extends JpaRepository<HospitalProfile, UUID> {
}
