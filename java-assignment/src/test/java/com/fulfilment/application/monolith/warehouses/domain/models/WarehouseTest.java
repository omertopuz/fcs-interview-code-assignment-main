package com.fulfilment.application.monolith.warehouses.domain.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

public class WarehouseTest {

  @Test
  public void testCreateWarehouseWithAllFields() {
    // given
    LocalDateTime now = LocalDateTime.now();

    // when
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 100;
    warehouse.stock = 50;
    warehouse.createdAt = now;
    warehouse.archivedAt = null;

    // then
    assertNotNull(warehouse);
    assertEquals("MWH.001", warehouse.businessUnitCode);
    assertEquals("AMSTERDAM-001", warehouse.location);
    assertEquals(100, warehouse.capacity);
    assertEquals(50, warehouse.stock);
    assertEquals(now, warehouse.createdAt);
    assertNull(warehouse.archivedAt);
  }

  @Test
  public void testWarehouseFieldsAreNullByDefault() {
    // when
    Warehouse warehouse = new Warehouse();

    // then
    assertNull(warehouse.businessUnitCode);
    assertNull(warehouse.location);
    assertNull(warehouse.capacity);
    assertNull(warehouse.stock);
    assertNull(warehouse.createdAt);
    assertNull(warehouse.archivedAt);
  }

  @Test
  public void testArchivedWarehouse() {
    // given
    LocalDateTime createdAt = LocalDateTime.now().minusDays(30);
    LocalDateTime archivedAt = LocalDateTime.now();

    // when
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.002";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 50;
    warehouse.stock = 25;
    warehouse.createdAt = createdAt;
    warehouse.archivedAt = archivedAt;

    // then
    assertNotNull(warehouse.archivedAt);
    assertEquals(archivedAt, warehouse.archivedAt);
  }

  @Test
  public void testWarehouseWithZeroStock() {
    // when
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.003";
    warehouse.location = "TILBURG-001";
    warehouse.capacity = 40;
    warehouse.stock = 0;

    // then
    assertEquals(0, warehouse.stock);
  }

  @Test
  public void testWarehouseWithFullCapacity() {
    // when - stock equals capacity (warehouse is full)
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.004";
    warehouse.location = "EINDHOVEN-001";
    warehouse.capacity = 70;
    warehouse.stock = 70;

    // then
    assertEquals(warehouse.capacity, warehouse.stock);
  }
}
