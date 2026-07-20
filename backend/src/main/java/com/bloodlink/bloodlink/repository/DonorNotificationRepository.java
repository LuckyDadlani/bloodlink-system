package com.bloodlink.bloodlink.repository;

import com.bloodlink.bloodlink.model.DonorNotification;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DonorNotificationRepository extends JpaRepository<DonorNotification, UUID> {

    @Modifying
    @Query("""
        update DonorNotification dn
        set dn.responseReceived = 'YES',
            dn.responseReceivedAt = :respondedAt
        where dn.emergencyId = :emergencyId and dn.donorId = :donorId
        """)
    int markAccepted(@Param("emergencyId") UUID emergencyId,
                     @Param("donorId") UUID donorId,
                     @Param("respondedAt") Instant respondedAt);
}
