package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.beans.Warehouse;
import jakarta.ws.rs.WebApplicationException;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WarehouseResourceImplTest {

  private WarehouseRepository warehouseRepository;
  private CreateWarehouseOperation createWarehouseOperation;
  private ArchiveWarehouseOperation archiveWarehouseOperation;
  private ReplaceWarehouseOperation replaceWarehouseOperation;
  private WarehouseResourceImpl warehouseResource;

  @BeforeEach
  public void setUp() throws Exception {
    warehouseRepository = mock(WarehouseRepository.class);
    createWarehouseOperation = mock(CreateWarehouseOperation.class);
    archiveWarehouseOperation = mock(ArchiveWarehouseOperation.class);
    replaceWarehouseOperation = mock(ReplaceWarehouseOperation.class);

    warehouseResource = new WarehouseResourceImpl();

    // Use reflection to inject mocks since fields are private
    setField(warehouseResource, "warehouseRepository", warehouseRepository);
    setField(warehouseResource, "createWarehouseOperation", createWarehouseOperation);
    setField(warehouseResource, "archiveWarehouseOperation", archiveWarehouseOperation);
    setField(warehouseResource, "replaceWarehouseOperation", replaceWarehouseOperation);
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  @Test
  public void testListAllWarehouses_ShouldReturnAllWarehouses() {
    // given
    var warehouse1 = createDomainWarehouse("MWH.001", "ZWOLLE-001", 50, 10);
    var warehouse2 = createDomainWarehouse("MWH.002", "AMSTERDAM-001", 80, 20);

    when(warehouseRepository.getAll()).thenReturn(List.of(warehouse1, warehouse2));

    // when
    var result = warehouseResource.listAllWarehousesUnits();

    // then
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("MWH.001", result.get(0).getBusinessUnitCode());
    assertEquals("MWH.002", result.get(1).getBusinessUnitCode());
  }

  @Test
  public void testListAllWarehouses_EmptyList_ShouldReturnEmpty() {
    // given
    when(warehouseRepository.getAll()).thenReturn(List.of());

    // when
    var result = warehouseResource.listAllWarehousesUnits();

    // then
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  public void testGetWarehouseById_Found_ShouldReturnWarehouse() {
    // given
    var domainWarehouse = createDomainWarehouse("MWH.001", "ZWOLLE-001", 50, 10);
    when(warehouseRepository.findByBusinessUnitCode("MWH.001")).thenReturn(domainWarehouse);

    // when
    var result = warehouseResource.getAWarehouseUnitByID("MWH.001");

    // then
    assertNotNull(result);
    assertEquals("MWH.001", result.getBusinessUnitCode());
    assertEquals("ZWOLLE-001", result.getLocation());
    assertEquals(50, result.getCapacity());
    assertEquals(10, result.getStock());
  }

  @Test
  public void testGetWarehouseById_NotFound_ShouldThrowNotFound() {
    // given
    when(warehouseRepository.findByBusinessUnitCode("NON-EXISTENT")).thenReturn(null);

    // when/then
    WebApplicationException exception =
        assertThrows(
            WebApplicationException.class,
            () -> warehouseResource.getAWarehouseUnitByID("NON-EXISTENT"));
    assertEquals(404, exception.getResponse().getStatus());
  }

  @Test
  public void testCreateWarehouse_Success_ShouldReturnCreatedWarehouse() {
    // given
    var apiWarehouse = createApiWarehouse("NEW-001", "AMSTERDAM-001", 50, 10);
    var createdDomainWarehouse = createDomainWarehouse("NEW-001", "AMSTERDAM-001", 50, 10);

    doNothing().when(createWarehouseOperation).create(any());
    when(warehouseRepository.findByBusinessUnitCode("NEW-001")).thenReturn(createdDomainWarehouse);

    // when
    var result = warehouseResource.createANewWarehouseUnit(apiWarehouse);

    // then
    assertNotNull(result);
    assertEquals("NEW-001", result.getBusinessUnitCode());
    verify(createWarehouseOperation).create(any());
  }

  @Test
  public void testCreateWarehouse_ValidationError_ShouldThrowBadRequest() {
    // given
    var apiWarehouse = createApiWarehouse("NEW-001", "INVALID-LOC", 50, 10);

    doThrow(new WarehouseValidationException("Invalid location"))
        .when(createWarehouseOperation)
        .create(any());

    // when/then
    WebApplicationException exception =
        assertThrows(
            WebApplicationException.class,
            () -> warehouseResource.createANewWarehouseUnit(apiWarehouse));
    assertEquals(400, exception.getResponse().getStatus());
  }

  @Test
  public void testArchiveWarehouse_Success_ShouldNotThrow() {
    // given
    doNothing().when(archiveWarehouseOperation).archive(any());

    // when
    warehouseResource.archiveAWarehouseUnitByID("MWH.001");

    // then
    verify(archiveWarehouseOperation).archive(any());
  }

  @Test
  public void testArchiveWarehouse_NotFound_ShouldThrowNotFound() {
    // given
    doThrow(new WarehouseNotFoundException("Warehouse not found"))
        .when(archiveWarehouseOperation)
        .archive(any());

    // when/then
    WebApplicationException exception =
        assertThrows(
            WebApplicationException.class,
            () -> warehouseResource.archiveAWarehouseUnitByID("NON-EXISTENT"));
    assertEquals(404, exception.getResponse().getStatus());
  }

  @Test
  public void testReplaceWarehouse_Success_ShouldReturnReplacedWarehouse() {
    // given
    var apiWarehouse = createApiWarehouse("MWH.001", "AMSTERDAM-001", 60, 15);
    var replacedDomainWarehouse = createDomainWarehouse("MWH.001", "AMSTERDAM-001", 60, 15);

    doNothing().when(replaceWarehouseOperation).replace(any());
    when(warehouseRepository.findByBusinessUnitCode("MWH.001")).thenReturn(replacedDomainWarehouse);

    // when
    var result = warehouseResource.replaceTheCurrentActiveWarehouse("MWH.001", apiWarehouse);

    // then
    assertNotNull(result);
    assertEquals("MWH.001", result.getBusinessUnitCode());
    assertEquals("AMSTERDAM-001", result.getLocation());
    verify(replaceWarehouseOperation).replace(any());
  }

  @Test
  public void testReplaceWarehouse_NotFound_ShouldThrowNotFound() {
    // given
    var apiWarehouse = createApiWarehouse("NON-EXISTENT", "AMSTERDAM-001", 60, 15);

    doThrow(new WarehouseNotFoundException("Warehouse not found"))
        .when(replaceWarehouseOperation)
        .replace(any());

    // when/then
    WebApplicationException exception =
        assertThrows(
            WebApplicationException.class,
            () -> warehouseResource.replaceTheCurrentActiveWarehouse("NON-EXISTENT", apiWarehouse));
    assertEquals(404, exception.getResponse().getStatus());
  }

  @Test
  public void testReplaceWarehouse_ValidationError_ShouldThrowBadRequest() {
    // given
    var apiWarehouse = createApiWarehouse("MWH.001", "INVALID-LOC", 60, 15);

    doThrow(new WarehouseValidationException("Invalid location"))
        .when(replaceWarehouseOperation)
        .replace(any());

    // when/then
    WebApplicationException exception =
        assertThrows(
            WebApplicationException.class,
            () -> warehouseResource.replaceTheCurrentActiveWarehouse("MWH.001", apiWarehouse));
    assertEquals(400, exception.getResponse().getStatus());
  }

  // Helper methods
  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse
      createDomainWarehouse(String businessUnitCode, String location, int capacity, int stock) {
    var warehouse =
        new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse.businessUnitCode = businessUnitCode;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  private Warehouse createApiWarehouse(
      String businessUnitCode, String location, int capacity, int stock) {
    var warehouse = new Warehouse();
    warehouse.setBusinessUnitCode(businessUnitCode);
    warehouse.setLocation(location);
    warehouse.setCapacity(capacity);
    warehouse.setStock(stock);
    return warehouse;
  }
}
