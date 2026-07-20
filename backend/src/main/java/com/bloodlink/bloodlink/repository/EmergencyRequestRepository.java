package com.bloodlink.bloodlink.repository;

import com.bloodlink.bloodlink.model.EmergencyRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

public interface EmergencyRequestRepository extends JpaRepository<EmergencyRequest, UUID> {

		List<EmergencyRequest> findTop10ByHospitalIdOrderByCreatedAtDesc(UUID hospitalId);

		List<EmergencyRequest> findByHospitalIdOrderByCreatedAtDesc(UUID hospitalId);

		@Query("""
				select count(er) from EmergencyRequest er
				where er.hospitalId = :hospitalId
					and er.status in ('CREATED', 'CHECKING_BANKS', 'ESCALATED_TO_DONORS', 'DONORS_NOTIFIED', 'PARTIALLY_FULFILLED')
				""")
		long countActiveByHospitalId(@Param("hospitalId") UUID hospitalId);

		@Query("""
				select count(er) from EmergencyRequest er
				where er.hospitalId = :hospitalId
					and er.status in ('DONOR_CONFIRMED', 'FULFILLED_BY_BANK', 'CLOSED')
				""")
		long countFulfilledByHospitalId(@Param("hospitalId") UUID hospitalId);

		@Modifying
		@Query(value = """
				update emergency_requests
				set status = 'DONOR_CONFIRMED',
						fulfilled_by_donor_id = :donorId,
						first_donor_response_at = coalesce(first_donor_response_at, :respondedAt),
						closed_at = :respondedAt,
						units_fulfilled = units_required
				where emergency_id = :emergencyId
					and fulfilled_by_donor_id is null
					and status in ('CREATED', 'ESCALATED_TO_DONORS', 'DONORS_NOTIFIED', 'PARTIALLY_FULFILLED')
				""", nativeQuery = true)
		int markFulfilledByFirstDonor(@Param("emergencyId") UUID emergencyId,
																	@Param("donorId") UUID donorId,
																	@Param("respondedAt") Instant respondedAt);
}