package com.fulfilment.application.monolith.warehouses.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class WarehouseNotFoundExceptionTest {

  @Test
  public void testExceptionWithMessage() {
    // when
    WarehouseNotFoundException exception =
        new WarehouseNotFoundException("Warehouse not found");

    // then
    assertNotNull(exception);
    assertEquals("Warehouse not found", exception.getMessage());
  }

  @Test
  public void testExceptionWithDetailedMessage() {
    // when
    String detailedMessage = "Warehouse not found with business unit code: NON-EXISTENT";
    WarehouseNotFoundException exception = new WarehouseNotFoundException(detailedMessage);

    // then
    assertEquals(detailedMessage, exception.getMessage());
  }
}
