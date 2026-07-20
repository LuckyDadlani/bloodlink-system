package com.bloodlink.bloodlink.service;

import com.bloodlink.bloodlink.dto.InventoryItemDto;
import com.bloodlink.bloodlink.exception.ApiException;
import com.bloodlink.bloodlink.model.BloodInventory;
import com.bloodlink.bloodlink.repository.BloodInventoryRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final BloodInventoryRepository bloodInventoryRepository;

    public InventoryService(BloodInventoryRepository bloodInventoryRepository) {
        this.bloodInventoryRepository = bloodInventoryRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryItemDto> getInventory(UUID bloodBankId) {
        return bloodInventoryRepository.findByBloodBankIdOrderByBloodGroupAscComponentTypeAsc(bloodBankId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public InventoryItemDto updateInventory(UUID inventoryId, int unitsAvailable, UUID updatedBy) {
        BloodInventory inventory = bloodInventoryRepository.findById(inventoryId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Inventory record not found"));

        inventory.setUnitsAvailable(unitsAvailable);
        inventory.setLastUpdatedAt(Instant.now());
        inventory.setLastUpdatedBy(updatedBy);

        return toDto(bloodInventoryRepository.save(inventory));
    }

    private InventoryItemDto toDto(BloodInventory inventory) {
        return new InventoryItemDto(
            inventory.getInventoryId(),
            inventory.getBloodGroup(),
            inventory.getComponentType(),
            inventory.getUnitsAvailable(),
            inventory.getLastUpdatedAt(),
            inventory.getLastUpdatedBy()
        );
    }
}
