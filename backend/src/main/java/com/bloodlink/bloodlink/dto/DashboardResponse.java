package com.bloodlink.bloodlink.dto;

import java.util.List;

public record DashboardResponse(
    String bloodBankName,
    String city,
    String state,
    int totalUnits,
    long activeRequests,
    long fulfilledRequests,
    List<ActivityItem> recentActivity
) {
    public record ActivityItem(
        String emergencyId,
        String bloodGroup,
        int unitsRequired,
        String status,
        String createdAt
    ) {
    }
}
