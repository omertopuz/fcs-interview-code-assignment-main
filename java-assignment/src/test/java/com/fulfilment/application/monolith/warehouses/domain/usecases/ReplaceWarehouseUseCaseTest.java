package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ReplaceWarehouseUseCaseTest {

  @Test
  void shouldReplaceWarehouseSuccessfully() {
    // Arrange
    var mockLocationResolver = Mockito.mock(LocationResolver.class);
    var mockWarehouseStore = Mockito.mock(WarehouseStore.class);
    var useCase = new ReplaceWarehouseUseCase(mockWarehouseStore, mockLocationResolver);

    var existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "MWH.001";
    existingWarehouse.location = "AMSTERDAM-001";
    existingWarehouse.capacity = 50;
    existingWarehouse.stock = 10;
    existingWarehouse.createdAt = LocalDateTime.now();
    existingWarehouse.archivedAt = null;

    var location = new Location("AMSTERDAM-001", 5, 100);
    Mockito.when(mockWarehouseStore.findByBusinessUnitCode("MWH.001"))
        .thenReturn(existingWarehouse);
    Mockito.when(mockLocationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(location);
    Mockito.when(mockWarehouseStore.countActiveByLocation("AMSTERDAM-001")).thenReturn(1L);

    var newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "AMSTERDAM-001";
    newWarehouse.capacity = 100;
    newWarehouse.stock = 10;

    // Act
    useCase.replace(newWarehouse);

    // Assert
    assertEquals(existingWarehouse.createdAt, newWarehouse.createdAt);
    Mockito.verify(mockWarehouseStore, Mockito.times(1)).update(newWarehouse);
  }

  @Test
  void shouldRejectWhenWarehouseToReplaceNotFound() {
    // Arrange
    var mockLocationResolver = Mockito.mock(LocationResolver.class);
    var mockWarehouseStore = Mockito.mock(WarehouseStore.class);
    var useCase = new ReplaceWarehouseUseCase(mockWarehouseStore, mockLocationResolver);

    Mockito.when(mockWarehouseStore.findByBusinessUnitCode("MWH.999")).thenReturn(null);

    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.999";

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> useCase.replace(warehouse));
  }

  @Test
  void shouldRejectWhenNewCapacityCannotAccommodateStock() {
    // Arrange
    var mockLocationResolver = Mockito.mock(LocationResolver.class);
    var mockWarehouseStore = Mockito.mock(WarehouseStore.class);
    var useCase = new ReplaceWarehouseUseCase(mockWarehouseStore, mockLocationResolver);

    var existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "MWH.001";
    existingWarehouse.location = "AMSTERDAM-001";
    existingWarehouse.stock = 50;
    existingWarehouse.archivedAt = null;

    var location = new Location("AMSTERDAM-001", 5, 100);
    Mockito.when(mockWarehouseStore.findByBusinessUnitCode("MWH.001"))
        .thenReturn(existingWarehouse);
    Mockito.when(mockLocationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(location);

    var newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "AMSTERDAM-001";
    newWarehouse.capacity = 30;
    newWarehouse.stock = 50;

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> useCase.replace(newWarehouse));
  }

  @Test
  void shouldRejectWhenStockDoesNotMatch() {
    // Arrange
    var mockLocationResolver = Mockito.mock(LocationResolver.class);
    var mockWarehouseStore = Mockito.mock(WarehouseStore.class);
    var useCase = new ReplaceWarehouseUseCase(mockWarehouseStore, mockLocationResolver);

    var existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "MWH.001";
    existingWarehouse.location = "AMSTERDAM-001";
    existingWarehouse.stock = 10;
    existingWarehouse.archivedAt = null;

    var location = new Location("AMSTERDAM-001", 5, 100);
    Mockito.when(mockWarehouseStore.findByBusinessUnitCode("MWH.001"))
        .thenReturn(existingWarehouse);
    Mockito.when(mockLocationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(location);

    var newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "AMSTERDAM-001";
    newWarehouse.capacity = 100;
    newWarehouse.stock = 20;

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> useCase.replace(newWarehouse));
  }
}
