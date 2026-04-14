package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateWarehouseUseCaseTest {

  private WarehouseRepository warehouseRepository;
  private LocationResolver locationResolver;
  private CreateWarehouseUseCase createWarehouseUseCase;

  @BeforeEach
  public void setUp() {
    warehouseRepository = mock(WarehouseRepository.class);
    locationResolver = mock(LocationResolver.class);
    createWarehouseUseCase = new CreateWarehouseUseCase(warehouseRepository, locationResolver);
  }

  @Test
  public void testCreateWarehouse_Success() {
    // given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "NEW-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 50;
    warehouse.stock = 10;

    when(warehouseRepository.findAnyByBusinessUnitCode("NEW-001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseRepository.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(Collections.emptyList());

    // when
    createWarehouseUseCase.create(warehouse);

    // then
    verify(warehouseRepository).create(warehouse);
  }

  @Test
  public void testCreateWarehouse_DuplicateBusinessUnitCode_ShouldThrow() {
    // given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "EXISTING-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 50;
    warehouse.stock = 10;

    when(warehouseRepository.findAnyByBusinessUnitCode("EXISTING-001"))
        .thenReturn(new Warehouse());

    // when/then
    assertThrows(WarehouseValidationException.class, () -> createWarehouseUseCase.create(warehouse));
    verify(warehouseRepository, never()).create(any());
  }

  @Test
  public void testCreateWarehouse_InvalidLocation_ShouldThrow() {
    // given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "NEW-001";
    warehouse.location = "INVALID-LOCATION";
    warehouse.capacity = 50;
    warehouse.stock = 10;

    when(warehouseRepository.findAnyByBusinessUnitCode("NEW-001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("INVALID-LOCATION")).thenReturn(null);

    // when/then
    assertThrows(WarehouseValidationException.class, () -> createWarehouseUseCase.create(warehouse));
    verify(warehouseRepository, never()).create(any());
  }

  @Test
  public void testCreateWarehouse_MaxWarehousesReached_ShouldThrow() {
    // given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "NEW-001";
    warehouse.location = "TILBURG-001";
    warehouse.capacity = 20;
    warehouse.stock = 10;

    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "EXISTING-001";
    existingWarehouse.capacity = 20;

    when(warehouseRepository.findAnyByBusinessUnitCode("NEW-001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("TILBURG-001"))
        .thenReturn(new Location("TILBURG-001", 1, 40)); // max 1 warehouse
    when(warehouseRepository.findActiveByLocation("TILBURG-001"))
        .thenReturn(List.of(existingWarehouse)); // already has 1

    // when/then
    assertThrows(WarehouseValidationException.class, () -> createWarehouseUseCase.create(warehouse));
    verify(warehouseRepository, never()).create(any());
  }

  @Test
  public void testCreateWarehouse_ExceedsCapacity_ShouldThrow() {
    // given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "NEW-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 60;
    warehouse.stock = 10;

    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "EXISTING-001";
    existingWarehouse.capacity = 50;

    when(warehouseRepository.findAnyByBusinessUnitCode("NEW-001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100)); // max capacity 100
    when(warehouseRepository.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(List.of(existingWarehouse)); // 50 used, only 50 available

    // when/then
    assertThrows(WarehouseValidationException.class, () -> createWarehouseUseCase.create(warehouse));
    verify(warehouseRepository, never()).create(any());
  }

  @Test
  public void testCreateWarehouse_StockExceedsCapacity_ShouldThrow() {
    // given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "NEW-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 30;
    warehouse.stock = 50; // stock > capacity

    when(warehouseRepository.findAnyByBusinessUnitCode("NEW-001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseRepository.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(Collections.emptyList());

    // when/then
    assertThrows(WarehouseValidationException.class, () -> createWarehouseUseCase.create(warehouse));
    verify(warehouseRepository, never()).create(any());
  }

  @Test
  public void testCreateWarehouse_ExactlyAtMaxCapacity_ShouldSucceed() {
    // given - create warehouse that fills exactly the remaining capacity
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "NEW-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 50; // exactly fills remaining capacity
    warehouse.stock = 10;

    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "EXISTING-001";
    existingWarehouse.capacity = 50; // 50 used, 50 remaining

    when(warehouseRepository.findAnyByBusinessUnitCode("NEW-001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100)); // max capacity 100
    when(warehouseRepository.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(List.of(existingWarehouse));

    // when
    createWarehouseUseCase.create(warehouse);

    // then
    verify(warehouseRepository).create(warehouse);
  }

  @Test
  public void testCreateWarehouse_AtMaxWarehouseCount_ShouldSucceed() {
    // given - create warehouse where we're at maxNumberOfWarehouses - 1 (so this creates the last one)
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "NEW-001";
    warehouse.location = "ZWOLLE-002"; // max 2 warehouses
    warehouse.capacity = 20;
    warehouse.stock = 5;

    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "EXISTING-001";
    existingWarehouse.capacity = 20;

    when(warehouseRepository.findAnyByBusinessUnitCode("NEW-001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-002"))
        .thenReturn(new Location("ZWOLLE-002", 2, 50)); // max 2 warehouses
    when(warehouseRepository.findActiveByLocation("ZWOLLE-002"))
        .thenReturn(List.of(existingWarehouse)); // 1 existing, can add 1 more

    // when
    createWarehouseUseCase.create(warehouse);

    // then
    verify(warehouseRepository).create(warehouse);
  }

  @Test
  public void testCreateWarehouse_NullStock_ShouldSucceed() {
    // given - stock is optional and can be null
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "NEW-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 50;
    warehouse.stock = null;

    when(warehouseRepository.findAnyByBusinessUnitCode("NEW-001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseRepository.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(Collections.emptyList());

    // when
    createWarehouseUseCase.create(warehouse);

    // then
    verify(warehouseRepository).create(warehouse);
  }

  @Test
  public void testCreateWarehouse_ZeroStock_ShouldSucceed() {
    // given - zero stock is valid
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "NEW-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 50;
    warehouse.stock = 0;

    when(warehouseRepository.findAnyByBusinessUnitCode("NEW-001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseRepository.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(Collections.emptyList());

    // when
    createWarehouseUseCase.create(warehouse);

    // then
    verify(warehouseRepository).create(warehouse);
  }

  @Test
  public void testCreateWarehouse_StockEqualsCapacity_ShouldSucceed() {
    // given - stock exactly equal to capacity is valid
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "NEW-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 50;
    warehouse.stock = 50; // equals capacity

    when(warehouseRepository.findAnyByBusinessUnitCode("NEW-001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseRepository.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(Collections.emptyList());

    // when
    createWarehouseUseCase.create(warehouse);

    // then
    verify(warehouseRepository).create(warehouse);
  }

  @Test
  public void testCreateWarehouse_MultipleExistingWarehouses_ShouldValidateTotalCapacity() {
    // given - multiple existing warehouses that use up most capacity
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "NEW-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 30;
    warehouse.stock = 10;

    Warehouse existing1 = new Warehouse();
    existing1.businessUnitCode = "EXISTING-001";
    existing1.capacity = 40;

    Warehouse existing2 = new Warehouse();
    existing2.businessUnitCode = "EXISTING-002";
    existing2.capacity = 35; // 40 + 35 = 75, only 25 remaining

    when(warehouseRepository.findAnyByBusinessUnitCode("NEW-001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseRepository.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(List.of(existing1, existing2));

    // when/then - 30 > 25 remaining, should fail
    assertThrows(WarehouseValidationException.class, () -> createWarehouseUseCase.create(warehouse));
    verify(warehouseRepository, never()).create(any());
  }
}
