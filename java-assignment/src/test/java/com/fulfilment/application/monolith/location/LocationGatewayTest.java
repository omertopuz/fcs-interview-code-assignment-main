package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LocationGatewayTest {

  private LocationGateway locationGateway;

  @BeforeEach
  public void setUp() {
    locationGateway = new LocationGateway();
  }

  @Test
  public void testWhenResolveExistingLocationShouldReturn() {
    // when
    Location location = locationGateway.resolveByIdentifier("ZWOLLE-001");

    // then
    assertNotNull(location);
    assertEquals("ZWOLLE-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(40, location.maxCapacity);
  }

  @Test
  public void testWhenResolveNonExistingLocationShouldReturnNull() {
    // when
    Location location = locationGateway.resolveByIdentifier("INVALID-LOCATION");

    // then
    assertNull(location);
  }

  @Test
  public void testWhenResolveAmsterdamLocationShouldReturn() {
    // when
    Location location = locationGateway.resolveByIdentifier("AMSTERDAM-001");

    // then
    assertNotNull(location);
    assertEquals("AMSTERDAM-001", location.identification);
    assertEquals(5, location.maxNumberOfWarehouses);
    assertEquals(100, location.maxCapacity);
  }

  @Test
  public void testWhenResolveZwolle002LocationShouldReturn() {
    // when
    Location location = locationGateway.resolveByIdentifier("ZWOLLE-002");

    // then
    assertNotNull(location);
    assertEquals("ZWOLLE-002", location.identification);
    assertEquals(2, location.maxNumberOfWarehouses);
    assertEquals(50, location.maxCapacity);
  }

  @Test
  public void testWhenResolveAmsterdam002LocationShouldReturn() {
    // when
    Location location = locationGateway.resolveByIdentifier("AMSTERDAM-002");

    // then
    assertNotNull(location);
    assertEquals("AMSTERDAM-002", location.identification);
    assertEquals(3, location.maxNumberOfWarehouses);
    assertEquals(75, location.maxCapacity);
  }

  @Test
  public void testWhenResolveTilburg001LocationShouldReturn() {
    // when
    Location location = locationGateway.resolveByIdentifier("TILBURG-001");

    // then
    assertNotNull(location);
    assertEquals("TILBURG-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(40, location.maxCapacity);
  }

  @Test
  public void testWhenResolveHelmond001LocationShouldReturn() {
    // when
    Location location = locationGateway.resolveByIdentifier("HELMOND-001");

    // then
    assertNotNull(location);
    assertEquals("HELMOND-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(45, location.maxCapacity);
  }

  @Test
  public void testWhenResolveEindhoven001LocationShouldReturn() {
    // when
    Location location = locationGateway.resolveByIdentifier("EINDHOVEN-001");

    // then
    assertNotNull(location);
    assertEquals("EINDHOVEN-001", location.identification);
    assertEquals(2, location.maxNumberOfWarehouses);
    assertEquals(70, location.maxCapacity);
  }

  @Test
  public void testWhenResolveVetsby001LocationShouldReturn() {
    // when
    Location location = locationGateway.resolveByIdentifier("VETSBY-001");

    // then
    assertNotNull(location);
    assertEquals("VETSBY-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(90, location.maxCapacity);
  }

  @Test
  public void testWhenResolveNullIdentifierShouldReturnNull() {
    // when
    Location location = locationGateway.resolveByIdentifier(null);

    // then
    assertNull(location);
  }

  @Test
  public void testWhenResolveEmptyIdentifierShouldReturnNull() {
    // when
    Location location = locationGateway.resolveByIdentifier("");

    // then
    assertNull(location);
  }

  @Test
  public void testWhenResolveCaseSensitiveIdentifierShouldReturnNull() {
    // when - identifiers are case-sensitive
    Location location = locationGateway.resolveByIdentifier("zwolle-001");

    // then
    assertNull(location);
  }
}
