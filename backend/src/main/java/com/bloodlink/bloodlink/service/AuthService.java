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
import java.util.UUID;
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
        if (request.email() == null || request.email().isBlank() ||
            request.password() == null || request.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email and password are required");
        }

        AppUser user = appUserRepository.findByEmailIgnoreCase(request.email().trim())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is inactive. Please contact system administrator.");
        }

        String incomingHash = sha256Hex(request.password());
        boolean passwordMatches = incomingHash.equalsIgnoreCase(user.getPasswordHash())
                || request.password().equals(user.getPasswordHash());

        if (!passwordMatches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        // Retrieve blood bank profile or fallback gracefully for non-blood-bank role users (HOSPITAL / ADMIN)
        BloodBankProfile profile = bloodBankProfileRepository.findById(user.getUserId()).orElse(null);
        UUID bankId;
        String bankName;
        String city;
        String state;

        if (profile != null) {
            bankId = profile.getBloodBankId();
            bankName = profile.getBloodBankName();
            city = profile.getCity();
            state = profile.getState();
        } else {
            BloodBankProfile defaultBank = bloodBankProfileRepository.findAll().stream().findFirst().orElse(null);
            if (defaultBank != null) {
                bankId = defaultBank.getBloodBankId();
                bankName = defaultBank.getBloodBankName();
                city = defaultBank.getCity();
                state = defaultBank.getState();
            } else {
                bankId = user.getUserId();
                bankName = user.getFullName() != null ? user.getFullName() : "Blood Bank Portal";
                city = "Bengaluru";
                state = "Karnataka";
            }
        }

        appUserRepository.updateLastLoginAt(user.getUserId(), Instant.now());

        return new LoginResponse(
            user.getUserId(),
            user.getFullName() != null ? user.getFullName() : bankName,
            user.getEmail(),
            bankId,
            bankName,
            city,
            state
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
