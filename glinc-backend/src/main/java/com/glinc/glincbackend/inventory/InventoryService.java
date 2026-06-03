package com.glinc.glincbackend.inventory;

import com.glinc.glincbackend.inventory.dto.InventoryItemDto;
import com.glinc.glincbackend.inventory.dto.UpdateInventoryRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryService {

    private final InventoryRepository repository;

    public InventoryService(InventoryRepository repository) {
        this.repository = repository;
    }

    // Siempre devuelve los 4 tipos (rellena defaults si no existen) para que el frontend no tenga que diferenciar.
    public List<InventoryItemDto> list(String patientId) {
        List<InventoryItem> filas = repository.findByPatientId(patientId);
        Map<InventoryItemType, InventoryItem> porTipo = new HashMap<>();
        for (InventoryItem fila : filas) {
            porTipo.put(fila.getItemType(), fila);
        }

        List<InventoryItemDto> resultado = new ArrayList<>();
        for (InventoryItemType tipo : InventoryItemType.values()) {
            InventoryItem fila = porTipo.get(tipo);
            if (fila != null) {
                resultado.add(new InventoryItemDto(
                        fila.getItemType(),
                        fila.getQuantity(),
                        fila.getStatus(),
                        fila.getUpdatedAt()));
            } else {
                resultado.add(new InventoryItemDto(
                        tipo, null, InventoryStatus.OK, null));
            }
        }
        return resultado;
    }

    public InventoryItemDto update(String patientId,
                                   InventoryItemType type,
                                   UpdateInventoryRequest dto) {
        InventoryItem fila = repository
                .findByPatientIdAndItemType(patientId, type)
                .orElseGet(() -> new InventoryItem(
                        patientId, type, null, InventoryStatus.OK));

        fila.setQuantity(normalizar(dto.getQuantity()));

        if (dto.getStatus() != null) {
            fila.setStatus(dto.getStatus());
        }

        InventoryItem guardada = repository.save(fila);
        return new InventoryItemDto(
                guardada.getItemType(),
                guardada.getQuantity(),
                guardada.getStatus(),
                guardada.getUpdatedAt());
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}
