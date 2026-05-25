package com.glinc.glincbackend.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByUserEmailAndPatientId(String userEmail, String patientId);

    Optional<InventoryItem> findByUserEmailAndPatientIdAndItemType(
            String userEmail, String patientId, InventoryItemType itemType);
}
