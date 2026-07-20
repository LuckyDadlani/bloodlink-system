package com.bloodlink.bloodlink.service;

import com.bloodlink.bloodlink.dto.LoginRequest;
import com.bloodlink.bloodlink.dto.LoginResponse;
import com.bloodlink.bloodlink.exception.ApiException;
import com.bloodlink.bloodlink.model.AppUser;
import com.bloodlink.bloodlink.model.BloodBankProfile;
import com.bloodlink.bloodlink.repository.AppUserRepository;
import com.bloodlink.bloodlink.repository.BloodBankProfileRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final BloodBankProfileRepository bloodBankProfileRepository;

    public AuthService(AppUserRepository appUserRepository,
                       BloodBankProfileRepository bloodBankProfileRepository) {
        this.appUserRepository = appUserRepository;
        this.bloodBankProfileRepository = bloodBankProfileRepository;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!user.isActive() || !"BLOOD_BANK".equals(user.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only active BLOOD_BANK users can log in");
        }

        String incomingHash = sha256Hex(request.password());
        if (!incomingHash.equalsIgnoreCase(user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        BloodBankProfile profile = bloodBankProfileRepository.findById(user.getUserId())
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Blood bank profile missing for user"));

        appUserRepository.updateLastLoginAt(user.getUserId(), Instant.now());

        return new LoginResponse(
            user.getUserId(),
            user.getFullName(),
            user.getEmail(),
            profile.getBloodBankId(),
            profile.getBloodBankName(),
            profile.getCity(),
            profile.getState()
        );
    }

    private String sha256Hex(String plainText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }
}
