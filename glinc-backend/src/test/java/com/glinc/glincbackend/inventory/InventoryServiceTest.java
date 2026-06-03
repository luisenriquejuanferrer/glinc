package com.glinc.glincbackend.inventory;

import com.glinc.glincbackend.inventory.dto.InventoryItemDto;
import com.glinc.glincbackend.inventory.dto.UpdateInventoryRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    private static final String PATIENT_ID = "pat-1";

    @Mock
    private InventoryRepository repository;

    @InjectMocks
    private InventoryService service;

    @Test
    void list_sinFilas_devuelve4DefaultsConStatusOK() {
        when(repository.findByPatientId(PATIENT_ID)).thenReturn(List.of());

        List<InventoryItemDto> resultado = service.list(PATIENT_ID);

        assertThat(resultado).hasSize(4);
        assertThat(resultado).extracting(InventoryItemDto::getType)
                .containsExactly(InventoryItemType.SENSORS, InventoryItemType.INSULIN_FAST,
                        InventoryItemType.INSULIN_SLOW, InventoryItemType.GLUCAGON);
        assertThat(resultado).extracting(InventoryItemDto::getStatus)
                .containsOnly(InventoryStatus.OK);
        assertThat(resultado).allSatisfy(item -> {
            assertThat(item.getQuantity()).isNull();
            assertThat(item.getUpdatedAt()).isNull();
        });
    }

    @Test
    void list_filasParciales_rellenaTiposFaltantesConDefault() {
        InventoryItem sensors = new InventoryItem(PATIENT_ID,
                InventoryItemType.SENSORS, "4 unidades", InventoryStatus.WARN);
        InventoryItem glucagon = new InventoryItem(PATIENT_ID,
                InventoryItemType.GLUCAGON, "1 vial", InventoryStatus.OK);
        when(repository.findByPatientId(PATIENT_ID)).thenReturn(List.of(sensors, glucagon));

        List<InventoryItemDto> resultado = service.list(PATIENT_ID);

        assertThat(resultado).hasSize(4);
        assertThat(resultado.get(0).getType()).isEqualTo(InventoryItemType.SENSORS);
        assertThat(resultado.get(0).getQuantity()).isEqualTo("4 unidades");
        assertThat(resultado.get(0).getStatus()).isEqualTo(InventoryStatus.WARN);

        assertThat(resultado.get(1).getType()).isEqualTo(InventoryItemType.INSULIN_FAST);
        assertThat(resultado.get(1).getQuantity()).isNull();
        assertThat(resultado.get(1).getStatus()).isEqualTo(InventoryStatus.OK);

        assertThat(resultado.get(3).getType()).isEqualTo(InventoryItemType.GLUCAGON);
        assertThat(resultado.get(3).getQuantity()).isEqualTo("1 vial");
    }

    @Test
    void update_filaExistente_actualizaQuantityYStatus() {
        InventoryItem existente = new InventoryItem(PATIENT_ID,
                InventoryItemType.SENSORS, "3 unidades", InventoryStatus.OK);
        when(repository.findByPatientIdAndItemType(PATIENT_ID, InventoryItemType.SENSORS))
                .thenReturn(Optional.of(existente));
        when(repository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateInventoryRequest dto = new UpdateInventoryRequest();
        dto.setQuantity("  5 unidades  ");
        dto.setStatus(InventoryStatus.DANGER);

        InventoryItemDto resultado = service.update(PATIENT_ID, InventoryItemType.SENSORS, dto);

        assertThat(resultado.getQuantity()).isEqualTo("5 unidades");
        assertThat(resultado.getStatus()).isEqualTo(InventoryStatus.DANGER);
        assertThat(existente.getQuantity()).isEqualTo("5 unidades");
        assertThat(existente.getStatus()).isEqualTo(InventoryStatus.DANGER);
    }

    @Test
    void update_filaNoExistente_creaNuevaConValoresDelRequest() {
        when(repository.findByPatientIdAndItemType(PATIENT_ID, InventoryItemType.INSULIN_FAST))
                .thenReturn(Optional.empty());
        when(repository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateInventoryRequest dto = new UpdateInventoryRequest();
        dto.setQuantity("2 plumas");
        dto.setStatus(InventoryStatus.WARN);

        service.update(PATIENT_ID, InventoryItemType.INSULIN_FAST, dto);

        ArgumentCaptor<InventoryItem> captor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(repository).save(captor.capture());

        InventoryItem guardada = captor.getValue();
        assertThat(guardada.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(guardada.getItemType()).isEqualTo(InventoryItemType.INSULIN_FAST);
        assertThat(guardada.getQuantity()).isEqualTo("2 plumas");
        assertThat(guardada.getStatus()).isEqualTo(InventoryStatus.WARN);
    }

    @Test
    void update_quantityVacio_seGuardaComoNull() {
        when(repository.findByPatientIdAndItemType(PATIENT_ID, InventoryItemType.GLUCAGON))
                .thenReturn(Optional.empty());
        when(repository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateInventoryRequest dto = new UpdateInventoryRequest();
        dto.setQuantity("   ");
        dto.setStatus(InventoryStatus.OK);

        InventoryItemDto resultado = service.update(PATIENT_ID, InventoryItemType.GLUCAGON, dto);

        assertThat(resultado.getQuantity()).isNull();
    }

    @Test
    void update_statusNull_mantieneStatusAnterior() {
        InventoryItem existente = new InventoryItem(PATIENT_ID,
                InventoryItemType.SENSORS, "3 unidades", InventoryStatus.DANGER);
        when(repository.findByPatientIdAndItemType(PATIENT_ID, InventoryItemType.SENSORS))
                .thenReturn(Optional.of(existente));
        when(repository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateInventoryRequest dto = new UpdateInventoryRequest();
        dto.setQuantity("5 unidades");
        dto.setStatus(null);

        InventoryItemDto resultado = service.update(PATIENT_ID, InventoryItemType.SENSORS, dto);

        assertThat(resultado.getStatus()).isEqualTo(InventoryStatus.DANGER);
    }
}
