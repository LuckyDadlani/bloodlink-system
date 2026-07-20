package com.bloodlink.bloodlink.service;

import com.bloodlink.bloodlink.exception.ApiException;
import com.bloodlink.bloodlink.model.AppUser;
import com.bloodlink.bloodlink.repository.AppUserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AccessService {

    private final AppUserRepository appUserRepository;

    public AccessService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public AppUser requireBloodBankUser(UUID userId) {
        return appUserRepository.findByUserIdAndRoleAndActiveTrue(userId, "BLOOD_BANK")
            .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Only BLOOD_BANK users can access this endpoint"));
    }
}
