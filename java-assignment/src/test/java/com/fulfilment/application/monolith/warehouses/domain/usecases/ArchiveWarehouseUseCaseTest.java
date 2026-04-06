package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ArchiveWarehouseUseCaseTest {

  @Test
  void shouldArchiveExistingWarehouse() {
    // Arrange
    var mockWarehouseStore = Mockito.mock(WarehouseStore.class);
    var useCase = new ArchiveWarehouseUseCase(mockWarehouseStore);

    var existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "MWH.001";
    existingWarehouse.location = "AMSTERDAM-001";
    existingWarehouse.capacity = 50;
    existingWarehouse.stock = 10;
    existingWarehouse.createdAt = LocalDateTime.now();
    existingWarehouse.archivedAt = null;

    Mockito.when(mockWarehouseStore.findByBusinessUnitCode("MWH.001"))
        .thenReturn(existingWarehouse);

    var warehouseToArchive = new Warehouse();
    warehouseToArchive.businessUnitCode = "MWH.001";

    // Act
    useCase.archive(warehouseToArchive);

    // Assert
    assertNotNull(warehouseToArchive.archivedAt);
    assertEquals(existingWarehouse.createdAt, warehouseToArchive.createdAt);
    assertEquals(existingWarehouse.location, warehouseToArchive.location);
    Mockito.verify(mockWarehouseStore, Mockito.times(1)).update(warehouseToArchive);
  }

  @Test
  void shouldRejectArchivingNonExistentWarehouse() {
    // Arrange
    var mockWarehouseStore = Mockito.mock(WarehouseStore.class);
    var useCase = new ArchiveWarehouseUseCase(mockWarehouseStore);

    Mockito.when(mockWarehouseStore.findByBusinessUnitCode("MWH.999")).thenReturn(null);

    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.999";

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> useCase.archive(warehouse));
  }

  @Test
  void shouldRejectArchivingAlreadyArchivedWarehouse() {
    // Arrange
    var mockWarehouseStore = Mockito.mock(WarehouseStore.class);
    var useCase = new ArchiveWarehouseUseCase(mockWarehouseStore);

    var archivedWarehouse = new Warehouse();
    archivedWarehouse.businessUnitCode = "MWH.001";
    archivedWarehouse.archivedAt = LocalDateTime.now();

    Mockito.when(mockWarehouseStore.findByBusinessUnitCode("MWH.001"))
        .thenReturn(archivedWarehouse);

    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> useCase.archive(warehouse));
  }
}
