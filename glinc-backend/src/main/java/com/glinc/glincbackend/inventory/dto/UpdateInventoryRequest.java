package com.glinc.glincbackend.inventory.dto;

import com.glinc.glincbackend.inventory.InventoryStatus;

public class UpdateInventoryRequest {

    private String quantity;
    private InventoryStatus status;

    public UpdateInventoryRequest() {
    }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }

    public InventoryStatus getStatus() { return status; }
    public void setStatus(InventoryStatus status) { this.status = status; }
}
