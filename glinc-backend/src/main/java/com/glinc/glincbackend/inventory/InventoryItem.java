package com.glinc.glincbackend.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

// Desde V7 el inventario es per-paciente (no per cuidador+paciente). Si dos cuidadores
// gestionan el mismo paciente, comparten inventario. La unicidad la impone (patient_id, item_type).
@Entity
@Table(name = "patient_inventory")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false, length = 255)
    private String patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 40)
    private InventoryItemType itemType;

    @Column(name = "quantity", length = 60)
    private String quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private InventoryStatus status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public InventoryItem() {
    }

    public InventoryItem(String patientId, InventoryItemType itemType,
                         String quantity, InventoryStatus status) {
        this.patientId = patientId;
        this.itemType = itemType;
        this.quantity = quantity;
        this.status = status;
    }

    @PrePersist
    @PreUpdate
    void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getPatientId() { return patientId; }
    public InventoryItemType getItemType() { return itemType; }
    public String getQuantity() { return quantity; }
    public InventoryStatus getStatus() { return status; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setQuantity(String quantity) { this.quantity = quantity; }
    public void setStatus(InventoryStatus status) { this.status = status; }
}
