package com.bloodlink.bloodlink.service;

import com.bloodlink.bloodlink.dto.InventoryCreateRequest;
import com.bloodlink.bloodlink.dto.InventoryItemDto;
import com.bloodlink.bloodlink.exception.ApiException;
import com.bloodlink.bloodlink.model.BloodInventory;
import com.bloodlink.bloodlink.repository.BloodBankProfileRepository;
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
    private final BloodBankProfileRepository bloodBankProfileRepository;

    public InventoryService(BloodInventoryRepository bloodInventoryRepository,
                            BloodBankProfileRepository bloodBankProfileRepository) {
        this.bloodInventoryRepository = bloodInventoryRepository;
        this.bloodBankProfileRepository = bloodBankProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryItemDto> getInventory(UUID bloodBankId) {
        return bloodInventoryRepository.findByBloodBankIdOrderByBloodGroupAscComponentTypeAsc(bloodBankId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public InventoryItemDto createInventory(InventoryCreateRequest request) {
        // Validate blood bank exists
        bloodBankProfileRepository.findById(request.bloodBankId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Blood bank not found"));

        // Check for duplicate (same bank + blood group + component)
        bloodInventoryRepository.findByBankAndGroupAndComponent(
            request.bloodBankId(), request.bloodGroup(), request.componentType()
        ).ifPresent(existing -> {
            throw new ApiException(HttpStatus.CONFLICT,
                "Inventory row already exists for " + request.bloodGroup() + " / " + request.componentType()
                + ". Use update instead of creating a duplicate.");
        });

        BloodInventory inventory = new BloodInventory();
        inventory.setInventoryId(UUID.randomUUID());
        inventory.setBloodBankId(request.bloodBankId());
        inventory.setBloodGroup(request.bloodGroup());
        inventory.setComponentType(request.componentType());
        inventory.setUnitsAvailable(request.unitsAvailable());
        inventory.setLastUpdatedAt(Instant.now());
        inventory.setLastUpdatedBy(request.createdBy());

        return toDto(bloodInventoryRepository.save(inventory));
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

    @Transactional
    public void deleteInventory(UUID inventoryId, UUID bloodBankId) {
        BloodInventory inventory = bloodInventoryRepository.findById(inventoryId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Inventory record not found"));

        if (!inventory.getBloodBankId().equals(bloodBankId)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                "You can only delete inventory belonging to your own blood bank");
        }

        bloodInventoryRepository.delete(inventory);
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

