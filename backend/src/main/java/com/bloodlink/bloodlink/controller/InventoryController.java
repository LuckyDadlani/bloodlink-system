package com.bloodlink.bloodlink.controller;

import com.bloodlink.bloodlink.dto.InventoryCreateRequest;
import com.bloodlink.bloodlink.dto.InventoryItemDto;
import com.bloodlink.bloodlink.dto.InventoryUpdateRequest;
import com.bloodlink.bloodlink.service.AccessService;
import com.bloodlink.bloodlink.service.InventoryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final AccessService accessService;

    public InventoryController(InventoryService inventoryService, AccessService accessService) {
        this.inventoryService = inventoryService;
        this.accessService = accessService;
    }

    @GetMapping("/{bloodBankId}")
    public List<InventoryItemDto> getInventory(@PathVariable UUID bloodBankId,
                                               @RequestHeader("X-User-Id") UUID userId) {
        accessService.requireBloodBankUser(userId);
        return inventoryService.getInventory(bloodBankId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryItemDto createInventory(@Valid @RequestBody InventoryCreateRequest request,
                                            @RequestHeader("X-User-Id") UUID userId) {
        accessService.requireBloodBankUser(userId);
        return inventoryService.createInventory(request);
    }

    @PutMapping("/{inventoryId}")
    public InventoryItemDto updateInventory(@PathVariable UUID inventoryId,
                                            @Valid @RequestBody InventoryUpdateRequest request,
                                            @RequestHeader("X-User-Id") UUID userId) {
        accessService.requireBloodBankUser(userId);
        return inventoryService.updateInventory(inventoryId, request.unitsAvailable(), request.updatedBy());
    }

    @DeleteMapping("/{inventoryId}")
    public Map<String, String> deleteInventory(@PathVariable UUID inventoryId,
                                               @RequestHeader("X-User-Id") UUID userId,
                                               @RequestHeader("X-Blood-Bank-Id") UUID bloodBankId) {
        accessService.requireBloodBankUser(userId);
        inventoryService.deleteInventory(inventoryId, bloodBankId);
        return Map.of("status", "DELETED");
    }
}

