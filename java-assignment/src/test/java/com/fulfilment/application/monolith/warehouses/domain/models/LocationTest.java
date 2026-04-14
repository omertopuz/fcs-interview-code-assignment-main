package com.fulfilment.application.monolith.warehouses.domain.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class LocationTest {

  @Test
  public void testCreateLocationWithAllFields() {
    // when
    Location location = new Location("AMSTERDAM-001", 5, 100);

    // then
    assertNotNull(location);
    assertEquals("AMSTERDAM-001", location.identification);
    assertEquals(5, location.maxNumberOfWarehouses);
    assertEquals(100, location.maxCapacity);
  }

  @Test
  public void testCreateLocationWithMinimumValues() {
    // when
    Location location = new Location("SMALL-001", 1, 10);

    // then
    assertNotNull(location);
    assertEquals("SMALL-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(10, location.maxCapacity);
  }

  @Test
  public void testCreateLocationWithLargeValues() {
    // when
    Location location = new Location("LARGE-001", 100, 10000);

    // then
    assertNotNull(location);
    assertEquals("LARGE-001", location.identification);
    assertEquals(100, location.maxNumberOfWarehouses);
    assertEquals(10000, location.maxCapacity);
  }
}
