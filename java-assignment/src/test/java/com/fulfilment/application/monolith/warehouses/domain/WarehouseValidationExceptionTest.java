package com.fulfilment.application.monolith.warehouses.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class WarehouseValidationExceptionTest {

  @Test
  public void testExceptionWithMessage() {
    // when
    WarehouseValidationException exception =
        new WarehouseValidationException("Validation failed");

    // then
    assertNotNull(exception);
    assertEquals("Validation failed", exception.getMessage());
  }

  @Test
  public void testExceptionWithDetailedMessage() {
    // when
    String detailedMessage = "Business unit code already exists: MWH.001";
    WarehouseValidationException exception = new WarehouseValidationException(detailedMessage);

    // then
    assertEquals(detailedMessage, exception.getMessage());
  }
}
