package com.glinc.glincbackend.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByPatientId(String patientId);

    Optional<InventoryItem> findByPatientIdAndItemType(
            String patientId, InventoryItemType itemType);
}
