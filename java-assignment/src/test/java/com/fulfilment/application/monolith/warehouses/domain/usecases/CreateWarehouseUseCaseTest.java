package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class CreateWarehouseUseCaseTest {

  @Test
  void shouldCreateWarehouseWithValidData() {
    // Arrange
    var mockLocationResolver = Mockito.mock(LocationResolver.class);
    var mockWarehouseStore = Mockito.mock(WarehouseStore.class);
    var useCase = new CreateWarehouseUseCase(mockWarehouseStore, mockLocationResolver);

    var location = new Location("AMSTERDAM-001", 5, 100);
    Mockito.when(mockLocationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(location);
    Mockito.when(mockWarehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(null);
    Mockito.when(mockWarehouseStore.countActiveByLocation("AMSTERDAM-001")).thenReturn(0L);

    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 50;
    warehouse.stock = 10;

    // Act
    useCase.create(warehouse);

    // Assert
    assertNotNull(warehouse.createdAt);
    Mockito.verify(mockWarehouseStore, Mockito.times(1)).create(warehouse);
  }

  @Test
  void shouldRejectDuplicateBusinessUnitCode() {
    // Arrange
    var mockLocationResolver = Mockito.mock(LocationResolver.class);
    var mockWarehouseStore = Mockito.mock(WarehouseStore.class);
    var useCase = new CreateWarehouseUseCase(mockWarehouseStore, mockLocationResolver);

    var existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "MWH.001";
    existingWarehouse.archivedAt = null;

    Mockito.when(mockWarehouseStore.findByBusinessUnitCode("MWH.001"))
        .thenReturn(existingWarehouse);

    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
  }

  @Test
  void shouldRejectInvalidLocation() {
    // Arrange
    var mockLocationResolver = Mockito.mock(LocationResolver.class);
    var mockWarehouseStore = Mockito.mock(WarehouseStore.class);
    var useCase = new CreateWarehouseUseCase(mockWarehouseStore, mockLocationResolver);

    Mockito.when(mockLocationResolver.resolveByIdentifier("INVALID")).thenReturn(null);
    Mockito.when(mockWarehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(null);

    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "INVALID";

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
  }

  @Test
  void shouldRejectWhenLocationAtMaxWarehouses() {
    // Arrange
    var mockLocationResolver = Mockito.mock(LocationResolver.class);
    var mockWarehouseStore = Mockito.mock(WarehouseStore.class);
    var useCase = new CreateWarehouseUseCase(mockWarehouseStore, mockLocationResolver);

    var location = new Location("ZWOLLE-001", 1, 40);
    Mockito.when(mockLocationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);
    Mockito.when(mockWarehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(null);
    Mockito.when(mockWarehouseStore.countActiveByLocation("ZWOLLE-001")).thenReturn(1L);

    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 30;

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
  }

  @Test
  void shouldRejectCapacityExceedingLocationMax() {
    // Arrange
    var mockLocationResolver = Mockito.mock(LocationResolver.class);
    var mockWarehouseStore = Mockito.mock(WarehouseStore.class);
    var useCase = new CreateWarehouseUseCase(mockWarehouseStore, mockLocationResolver);

    var location = new Location("AMSTERDAM-001", 5, 100);
    Mockito.when(mockLocationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(location);
    Mockito.when(mockWarehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(null);
    Mockito.when(mockWarehouseStore.countActiveByLocation("AMSTERDAM-001")).thenReturn(0L);

    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 150;

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
  }

  @Test
  void shouldRejectStockExceedingCapacity() {
    // Arrange
    var mockLocationResolver = Mockito.mock(LocationResolver.class);
    var mockWarehouseStore = Mockito.mock(WarehouseStore.class);
    var useCase = new CreateWarehouseUseCase(mockWarehouseStore, mockLocationResolver);

    var location = new Location("AMSTERDAM-001", 5, 100);
    Mockito.when(mockLocationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(location);
    Mockito.when(mockWarehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(null);
    Mockito.when(mockWarehouseStore.countActiveByLocation("AMSTERDAM-001")).thenReturn(0L);

    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 50;
    warehouse.stock = 100;

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
  }
}
