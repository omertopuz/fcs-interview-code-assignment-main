package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ArchiveWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private ArchiveWarehouseUseCase archiveWarehouseUseCase;

  @BeforeEach
  public void setUp() {
    warehouseStore = mock(WarehouseStore.class);
    archiveWarehouseUseCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  @Test
  public void testArchiveWarehouse_Success() {
    // given
    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "MWH.001";
    existingWarehouse.location = "ZWOLLE-001";
    existingWarehouse.capacity = 100;
    existingWarehouse.stock = 10;

    Warehouse warehouseToArchive = new Warehouse();
    warehouseToArchive.businessUnitCode = "MWH.001";

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existingWarehouse);

    // when
    archiveWarehouseUseCase.archive(warehouseToArchive);

    // then
    ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
    verify(warehouseStore).update(captor.capture());

    Warehouse updatedWarehouse = captor.getValue();
    assertNotNull(updatedWarehouse.archivedAt);
  }

  @Test
  public void testArchiveWarehouse_NotFound_ShouldThrow() {
    // given
    Warehouse warehouseToArchive = new Warehouse();
    warehouseToArchive.businessUnitCode = "NON-EXISTENT";

    when(warehouseStore.findByBusinessUnitCode("NON-EXISTENT")).thenReturn(null);

    // when/then
    assertThrows(
        WarehouseNotFoundException.class, () -> archiveWarehouseUseCase.archive(warehouseToArchive));
    verify(warehouseStore, never()).update(argThat(w -> w != null));
  }

  @Test
  public void testArchiveWarehouse_PreservesOriginalData() {
    // given - verify that archive preserves all original warehouse data
    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "MWH.001";
    existingWarehouse.location = "ZWOLLE-001";
    existingWarehouse.capacity = 100;
    existingWarehouse.stock = 50;
    existingWarehouse.archivedAt = null;

    Warehouse warehouseToArchive = new Warehouse();
    warehouseToArchive.businessUnitCode = "MWH.001";

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existingWarehouse);

    // when
    archiveWarehouseUseCase.archive(warehouseToArchive);

    // then
    ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
    verify(warehouseStore).update(captor.capture());

    Warehouse updatedWarehouse = captor.getValue();
    assertEquals("MWH.001", updatedWarehouse.businessUnitCode);
    assertEquals("ZWOLLE-001", updatedWarehouse.location);
    assertEquals(100, updatedWarehouse.capacity);
    assertEquals(50, updatedWarehouse.stock);
    assertNotNull(updatedWarehouse.archivedAt);
  }

  @Test
  public void testArchiveWarehouse_SetsArchivedAtTimestamp() {
    // given
    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "MWH.002";
    existingWarehouse.location = "AMSTERDAM-001";
    existingWarehouse.archivedAt = null;

    Warehouse warehouseToArchive = new Warehouse();
    warehouseToArchive.businessUnitCode = "MWH.002";

    when(warehouseStore.findByBusinessUnitCode("MWH.002")).thenReturn(existingWarehouse);

    // when
    archiveWarehouseUseCase.archive(warehouseToArchive);

    // then
    ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
    verify(warehouseStore).update(captor.capture());

    Warehouse updatedWarehouse = captor.getValue();
    assertNotNull(updatedWarehouse.archivedAt);
    // Timestamp should be recent (within last second)
    java.time.LocalDateTime now = java.time.LocalDateTime.now();
    assertTrue(updatedWarehouse.archivedAt.isAfter(now.minusSeconds(1)));
    assertTrue(updatedWarehouse.archivedAt.isBefore(now.plusSeconds(1)));
  }
}
