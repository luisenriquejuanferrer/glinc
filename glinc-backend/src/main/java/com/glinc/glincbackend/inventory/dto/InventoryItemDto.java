package com.glinc.glincbackend.inventory.dto;

import com.glinc.glincbackend.inventory.InventoryItemType;
import com.glinc.glincbackend.inventory.InventoryStatus;

import java.time.Instant;

public class InventoryItemDto {

    private InventoryItemType type;
    private String quantity;
    private InventoryStatus status;
    private Instant updatedAt;

    public InventoryItemDto() {
    }

    public InventoryItemDto(InventoryItemType type, String quantity,
                            InventoryStatus status, Instant updatedAt) {
        this.type = type;
        this.quantity = quantity;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public InventoryItemType getType() { return type; }
    public void setType(InventoryItemType type) { this.type = type; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }

    public InventoryStatus getStatus() { return status; }
    public void setStatus(InventoryStatus status) { this.status = status; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
