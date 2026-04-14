package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReplaceWarehouseUseCaseTest {

  private WarehouseRepository warehouseRepository;
  private LocationResolver locationResolver;
  private ArchiveWarehouseOperation archiveWarehouseOperation;
  private ReplaceWarehouseUseCase replaceWarehouseUseCase;

  @BeforeEach
  public void setUp() {
    warehouseRepository = mock(WarehouseRepository.class);
    locationResolver = mock(LocationResolver.class);
    archiveWarehouseOperation = mock(ArchiveWarehouseOperation.class);
    replaceWarehouseUseCase =
        new ReplaceWarehouseUseCase(warehouseRepository, locationResolver, archiveWarehouseOperation);
  }

  @Test
  public void testReplaceWarehouse_Success() {
    // given
    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "MWH.001";
    existingWarehouse.location = "ZWOLLE-001";
    existingWarehouse.capacity = 100;
    existingWarehouse.stock = 10;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "AMSTERDAM-001";
    newWarehouse.capacity = 50;
    newWarehouse.stock = 10; // matches the stock of the existing warehouse

    when(warehouseRepository.findByBusinessUnitCode("MWH.001")).thenReturn(existingWarehouse);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseRepository.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(Collections.emptyList());

    // when
    replaceWarehouseUseCase.replace(newWarehouse);

    // then
    verify(archiveWarehouseOperation).archive(existingWarehouse);
    verify(warehouseRepository).create(newWarehouse);
  }

  @Test
  public void testReplaceWarehouse_NotFound_ShouldThrow() {
    // given
    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "NON-EXISTENT";
    newWarehouse.location = "AMSTERDAM-001";
    newWarehouse.capacity = 50;

    when(warehouseRepository.findByBusinessUnitCode("NON-EXISTENT")).thenReturn(null);

    // when/then
    assertThrows(WarehouseNotFoundException.class, () -> replaceWarehouseUseCase.replace(newWarehouse));
    verify(warehouseRepository, never()).create(any());
    verify(archiveWarehouseOperation, never()).archive(any());
  }

  @Test
  public void testReplaceWarehouse_InvalidLocation_ShouldThrow() {
    // given
    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "MWH.001";
    existingWarehouse.stock = 10;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "INVALID-LOCATION";
    newWarehouse.capacity = 50;

    when(warehouseRepository.findByBusinessUnitCode("MWH.001")).thenReturn(existingWarehouse);
    when(locationResolver.resolveByIdentifier("INVALID-LOCATION")).thenReturn(null);

    // when/then
    assertThrows(WarehouseValidationException.class, () -> replaceWarehouseUseCase.replace(newWarehouse));
    verify(warehouseRepository, never()).create(any());
    verify(archiveWarehouseOperation, never()).archive(any());
  }

  @Test
  public void testReplaceWarehouse_CapacityCannotAccommodateStock_ShouldThrow() {
    // given
    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "MWH.001";
    existingWarehouse.stock = 50;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "AMSTERDAM-001";
    newWarehouse.capacity = 30; // less than existing stock

    when(warehouseRepository.findByBusinessUnitCode("MWH.001")).thenReturn(existingWarehouse);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));

    // when/then
    assertThrows(WarehouseValidationException.class, () -> replaceWarehouseUseCase.replace(newWarehouse));
    verify(warehouseRepository, never()).create(any());
    verify(archiveWarehouseOperation, never()).archive(any());
  }

  @Test
  public void testReplaceWarehouse_StockMismatch_ShouldThrow() {
    // given
    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "MWH.001";
    existingWarehouse.stock = 10;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "AMSTERDAM-001";
    newWarehouse.capacity = 50;
    newWarehouse.stock = 20; // doesn't match existing stock

    when(warehouseRepository.findByBusinessUnitCode("MWH.001")).thenReturn(existingWarehouse);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));

    // when/then
    assertThrows(WarehouseValidationException.class, () -> replaceWarehouseUseCase.replace(newWarehouse));
    verify(warehouseRepository, never()).create(any());
    verify(archiveWarehouseOperation, never()).archive(any());
  }

  @Test
  public void testReplaceWarehouse_ExceedsLocationCapacity_ShouldThrow() {
    // given
    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "MWH.001";
    existingWarehouse.stock = 10;

    Warehouse otherWarehouse = new Warehouse();
    otherWarehouse.businessUnitCode = "MWH.002";
    otherWarehouse.capacity = 80;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "AMSTERDAM-001";
    newWarehouse.capacity = 30; // 80 + 30 = 110 > 100
    newWarehouse.stock = 10;

    when(warehouseRepository.findByBusinessUnitCode("MWH.001")).thenReturn(existingWarehouse);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100)); // max capacity 100
    when(warehouseRepository.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(List.of(otherWarehouse)); // 80 already used

    // when/then
    assertThrows(WarehouseValidationException.class, () -> replaceWarehouseUseCase.replace(newWarehouse));
    verify(warehouseRepository, never()).create(any());
    verify(archiveWarehouseOperation, never()).archive(any());
  }

  @Test
  public void testReplaceWarehouse_NullStockInNewWarehouse_ShouldInheritStock() {
    // given - when new warehouse stock is null, it should inherit from the existing warehouse
    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "MWH.001";
    existingWarehouse.location = "ZWOLLE-001";
    existingWarehouse.capacity = 30;
    existingWarehouse.stock = 15;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "AMSTERDAM-001";
    newWarehouse.capacity = 50;
    newWarehouse.stock = null; // null stock should be inherited from existing

    when(warehouseRepository.findByBusinessUnitCode("MWH.001")).thenReturn(existingWarehouse);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseRepository.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(Collections.emptyList());

    // when
    replaceWarehouseUseCase.replace(newWarehouse);

    // then
    verify(archiveWarehouseOperation).archive(existingWarehouse);
    verify(warehouseRepository).create(newWarehouse);
    // Stock should be set to existing warehouse's stock
    assert newWarehouse.stock.equals(15);
  }

  @Test
  public void testReplaceWarehouse_SameLocation_ShouldSucceed() {
    // given - replacing warehouse at the same location
    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "MWH.001";
    existingWarehouse.location = "AMSTERDAM-001";
    existingWarehouse.capacity = 30;
    existingWarehouse.stock = 10;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "AMSTERDAM-001"; // same location
    newWarehouse.capacity = 50; // larger capacity
    newWarehouse.stock = 10;

    when(warehouseRepository.findByBusinessUnitCode("MWH.001")).thenReturn(existingWarehouse);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseRepository.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(List.of(existingWarehouse)); // only the warehouse being replaced

    // when
    replaceWarehouseUseCase.replace(newWarehouse);

    // then
    verify(archiveWarehouseOperation).archive(existingWarehouse);
    verify(warehouseRepository).create(newWarehouse);
  }

  @Test
  public void testReplaceWarehouse_CapacityExactlyMatchesStock_ShouldSucceed() {
    // given - new capacity exactly matches existing stock
    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "MWH.001";
    existingWarehouse.location = "ZWOLLE-001";
    existingWarehouse.capacity = 100;
    existingWarehouse.stock = 25;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "AMSTERDAM-001";
    newWarehouse.capacity = 25; // exactly matches existing stock
    newWarehouse.stock = 25;

    when(warehouseRepository.findByBusinessUnitCode("MWH.001")).thenReturn(existingWarehouse);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseRepository.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(Collections.emptyList());

    // when
    replaceWarehouseUseCase.replace(newWarehouse);

    // then
    verify(archiveWarehouseOperation).archive(existingWarehouse);
    verify(warehouseRepository).create(newWarehouse);
  }

  @Test
  public void testReplaceWarehouse_ZeroStock_ShouldSucceed() {
    // given - existing warehouse with zero stock
    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "MWH.001";
    existingWarehouse.location = "ZWOLLE-001";
    existingWarehouse.capacity = 30;
    existingWarehouse.stock = 0;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "AMSTERDAM-001";
    newWarehouse.capacity = 50;
    newWarehouse.stock = 0;

    when(warehouseRepository.findByBusinessUnitCode("MWH.001")).thenReturn(existingWarehouse);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseRepository.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(Collections.emptyList());

    // when
    replaceWarehouseUseCase.replace(newWarehouse);

    // then
    verify(archiveWarehouseOperation).archive(existingWarehouse);
    verify(warehouseRepository).create(newWarehouse);
  }

  @Test
  public void testReplaceWarehouse_MaxCapacityLocationWithExistingWarehouses_ShouldExcludeReplacedWarehouse() {
    // given - location with existing warehouses where the one being replaced is not counted twice
    Warehouse existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "MWH.001";
    existingWarehouse.location = "AMSTERDAM-001";
    existingWarehouse.capacity = 40;
    existingWarehouse.stock = 10;

    Warehouse otherWarehouse = new Warehouse();
    otherWarehouse.businessUnitCode = "MWH.002";
    otherWarehouse.capacity = 50;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "AMSTERDAM-001";
    newWarehouse.capacity = 50; // 50 (other) + 50 (new) = 100, exactly at max
    newWarehouse.stock = 10;

    when(warehouseRepository.findByBusinessUnitCode("MWH.001")).thenReturn(existingWarehouse);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    // Returns both the existing and other warehouse, but existing should be excluded from capacity calc
    when(warehouseRepository.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(List.of(existingWarehouse, otherWarehouse));

    // when
    replaceWarehouseUseCase.replace(newWarehouse);

    // then
    verify(archiveWarehouseOperation).archive(existingWarehouse);
    verify(warehouseRepository).create(newWarehouse);
  }
}
