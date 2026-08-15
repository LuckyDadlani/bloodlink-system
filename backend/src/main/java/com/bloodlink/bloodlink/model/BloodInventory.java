package com.bloodlink.bloodlink.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "blood_inventory")
public class BloodInventory {

    @Id
    @Column(name = "inventory_id")
    private UUID inventoryId;

    @Column(name = "blood_bank_id")
    private UUID bloodBankId;

    @Column(name = "blood_group", columnDefinition = "blood_group_enum")
    @ColumnTransformer(write = "?::blood_group_enum")
    private String bloodGroup;

    @Column(name = "component_type", columnDefinition = "component_type_enum")
    @ColumnTransformer(write = "?::component_type_enum")
    private String componentType;

    @Column(name = "units_available")
    private int unitsAvailable;

    @Column(name = "last_updated_at")
    private Instant lastUpdatedAt;

    @Column(name = "last_updated_by")
    private UUID lastUpdatedBy;

    public UUID getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(UUID inventoryId) {
        this.inventoryId = inventoryId;
    }

    public UUID getBloodBankId() {
        return bloodBankId;
    }

    public void setBloodBankId(UUID bloodBankId) {
        this.bloodBankId = bloodBankId;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public int getUnitsAvailable() {
        return unitsAvailable;
    }

    public void setUnitsAvailable(int unitsAvailable) {
        this.unitsAvailable = unitsAvailable;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public UUID getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(UUID lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }
}
