package com.bloodlink.bloodlink.repository;

import com.bloodlink.bloodlink.model.AppUser;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    @Query(value = """
        select * from users
        where user_id = :userId
          and role = cast(:role as user_role_enum)
          and is_active = true
        """, nativeQuery = true)
    Optional<AppUser> findByUserIdAndRoleAndActiveTrue(@Param("userId") UUID userId, @Param("role") String role);
    Optional<AppUser> findByEmailIgnoreCase(String email);

    @Modifying
    @Query("update AppUser u set u.lastLoginAt = :lastLoginAt where u.userId = :userId")
    int updateLastLoginAt(@Param("userId") UUID userId, @Param("lastLoginAt") Instant lastLoginAt);
}
