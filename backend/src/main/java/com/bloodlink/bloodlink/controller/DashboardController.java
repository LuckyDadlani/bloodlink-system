package com.bloodlink.bloodlink.controller;

import com.bloodlink.bloodlink.dto.DashboardResponse;
import com.bloodlink.bloodlink.service.AccessService;
import com.bloodlink.bloodlink.service.DashboardService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AccessService accessService;

    public DashboardController(DashboardService dashboardService, AccessService accessService) {
        this.dashboardService = dashboardService;
        this.accessService = accessService;
    }

    @GetMapping
    public DashboardResponse getDashboard(@RequestParam UUID bloodBankId,
                                          @RequestParam UUID creatorHospitalId,
                                          @RequestHeader("X-User-Id") UUID userId) {
        accessService.requireBloodBankUser(userId);
        return dashboardService.getDashboard(bloodBankId, creatorHospitalId);
    }
}
