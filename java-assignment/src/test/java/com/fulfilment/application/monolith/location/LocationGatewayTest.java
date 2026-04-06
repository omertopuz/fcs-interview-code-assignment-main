package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.Test;

public class LocationGatewayTest {

  @Test
  void shouldReturnLocationWhenIdentifierExists() {
    // Arrange
    LocationGateway gateway = new LocationGateway();

    // Act
    Location result = gateway.resolveByIdentifier("AMSTERDAM-001");

    // Assert
    assertNotNull(result);
    assertEquals("AMSTERDAM-001", result.identification);
    assertEquals(5, result.maxNumberOfWarehouses);
    assertEquals(100, result.maxCapacity);
  }

  @Test
  void shouldReturnNullWhenIdentifierNotFound() {
    // Arrange
    LocationGateway gateway = new LocationGateway();

    // Act
    Location result = gateway.resolveByIdentifier("INVALID-001");

    // Assert
    assertNull(result);
  }

  @Test
  void shouldReturnCorrectPropertiesForZwolle001() {
    // Arrange
    LocationGateway gateway = new LocationGateway();

    // Act
    Location result = gateway.resolveByIdentifier("ZWOLLE-001");

    // Assert
    assertNotNull(result);
    assertEquals("ZWOLLE-001", result.identification);
    assertEquals(1, result.maxNumberOfWarehouses);
    assertEquals(40, result.maxCapacity);
  }

  @Test
  void shouldReturnCorrectPropertiesForAmsterdam002() {
    // Arrange
    LocationGateway gateway = new LocationGateway();

    // Act
    Location result = gateway.resolveByIdentifier("AMSTERDAM-002");

    // Assert
    assertNotNull(result);
    assertEquals("AMSTERDAM-002", result.identification);
    assertEquals(3, result.maxNumberOfWarehouses);
    assertEquals(75, result.maxCapacity);
  }

  @Test
  void shouldReturnAllValidLocations() {
    // Arrange
    LocationGateway gateway = new LocationGateway();
    String[] validLocations = {
      "ZWOLLE-001",
      "ZWOLLE-002",
      "AMSTERDAM-001",
      "AMSTERDAM-002",
      "TILBURG-001",
      "HELMOND-001",
      "EINDHOVEN-001",
      "VETSBY-001"
    };

    // Act & Assert
    for (String locationId : validLocations) {
      Location result = gateway.resolveByIdentifier(locationId);
      assertNotNull(result, "Location " + locationId + " should exist");
      assertEquals(locationId, result.identification);
    }
  }
}
