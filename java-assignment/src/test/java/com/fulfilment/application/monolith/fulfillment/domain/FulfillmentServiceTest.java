package com.fulfilment.application.monolith.fulfillment.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FulfillmentServiceTest {

  private FulfillmentRepository fulfillmentRepository;
  private WarehouseStore warehouseStore;
  private EntityManager entityManager;
  private FulfillmentService fulfillmentService;

  private Product testProduct;
  private Store testStore;
  private Warehouse testWarehouse;

  @BeforeEach
  void setUp() {
    fulfillmentRepository = mock(FulfillmentRepository.class);
    warehouseStore = mock(WarehouseStore.class);
    entityManager = mock(EntityManager.class);

    fulfillmentService = new FulfillmentService();
    // Use reflection to inject mocks
    setField(fulfillmentService, "fulfillmentRepository", fulfillmentRepository);
    setField(fulfillmentService, "warehouseStore", warehouseStore);
    setField(fulfillmentService, "entityManager", entityManager);

    testProduct = new Product("TestProduct");
    testProduct.id = 1L;

    testStore = new Store("TestStore");
    testStore.id = 1L;

    testWarehouse = new Warehouse();
    testWarehouse.businessUnitCode = "WH-001";
  }

  private void setField(Object target, String fieldName, Object value) {
    try {
      var field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void createAssociation_success() {
    // Given
    when(entityManager.find(Product.class, 1L)).thenReturn(testProduct);
    when(entityManager.find(Store.class, 1L)).thenReturn(testStore);
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(testWarehouse);
    when(fulfillmentRepository.associationExists(1L, 1L, "WH-001")).thenReturn(false);
    when(fulfillmentRepository.warehouseFulfillsProductForStore(1L, 1L, "WH-001")).thenReturn(false);
    when(fulfillmentRepository.countDistinctWarehousesForProductAndStore(1L, 1L)).thenReturn(0L);
    when(fulfillmentRepository.warehouseAssociatedWithStore(1L, "WH-001")).thenReturn(false);
    when(fulfillmentRepository.countDistinctWarehousesForStore(1L)).thenReturn(0L);
    when(fulfillmentRepository.productStoredInWarehouse(1L, "WH-001")).thenReturn(false);
    when(fulfillmentRepository.countDistinctProductsForWarehouse("WH-001")).thenReturn(0L);

    // When
    FulfillmentAssociation result = fulfillmentService.createAssociation(1L, 1L, "WH-001");

    // Then
    assertNotNull(result);
    assertEquals(1L, result.productId);
    assertEquals(1L, result.storeId);
    assertEquals("WH-001", result.warehouseBusinessUnitCode);
    verify(fulfillmentRepository).persist(any(FulfillmentAssociation.class));
  }

  @Test
  void createAssociation_productNotFound_throwsException() {
    // Given
    when(entityManager.find(Product.class, 999L)).thenReturn(null);

    // When/Then
    FulfillmentValidationException exception =
        assertThrows(
            FulfillmentValidationException.class,
            () -> fulfillmentService.createAssociation(999L, 1L, "WH-001"));

    assertEquals("Product with id 999 not found", exception.getMessage());
  }

  @Test
  void createAssociation_storeNotFound_throwsException() {
    // Given
    when(entityManager.find(Product.class, 1L)).thenReturn(testProduct);
    when(entityManager.find(Store.class, 999L)).thenReturn(null);

    // When/Then
    FulfillmentValidationException exception =
        assertThrows(
            FulfillmentValidationException.class,
            () -> fulfillmentService.createAssociation(1L, 999L, "WH-001"));

    assertEquals("Store with id 999 not found", exception.getMessage());
  }

  @Test
  void createAssociation_warehouseNotFound_throwsException() {
    // Given
    when(entityManager.find(Product.class, 1L)).thenReturn(testProduct);
    when(entityManager.find(Store.class, 1L)).thenReturn(testStore);
    when(warehouseStore.findByBusinessUnitCode("INVALID")).thenReturn(null);

    // When/Then
    FulfillmentValidationException exception =
        assertThrows(
            FulfillmentValidationException.class,
            () -> fulfillmentService.createAssociation(1L, 1L, "INVALID"));

    assertEquals("Warehouse with business unit code INVALID not found", exception.getMessage());
  }

  @Test
  void createAssociation_duplicateAssociation_throwsException() {
    // Given
    when(entityManager.find(Product.class, 1L)).thenReturn(testProduct);
    when(entityManager.find(Store.class, 1L)).thenReturn(testStore);
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(testWarehouse);
    when(fulfillmentRepository.associationExists(1L, 1L, "WH-001")).thenReturn(true);

    // When/Then
    FulfillmentValidationException exception =
        assertThrows(
            FulfillmentValidationException.class,
            () -> fulfillmentService.createAssociation(1L, 1L, "WH-001"));

    assertTrue(exception.getMessage().contains("Association already exists"));
  }

  @Test
  void createAssociation_maxWarehousesPerProductStore_throwsException() {
    // Given - product already has 2 warehouses for this store
    when(entityManager.find(Product.class, 1L)).thenReturn(testProduct);
    when(entityManager.find(Store.class, 1L)).thenReturn(testStore);
    when(warehouseStore.findByBusinessUnitCode("WH-003")).thenReturn(testWarehouse);
    when(fulfillmentRepository.associationExists(1L, 1L, "WH-003")).thenReturn(false);
    when(fulfillmentRepository.warehouseFulfillsProductForStore(1L, 1L, "WH-003")).thenReturn(false);
    when(fulfillmentRepository.countDistinctWarehousesForProductAndStore(1L, 1L)).thenReturn(2L);

    // When/Then
    FulfillmentValidationException exception =
        assertThrows(
            FulfillmentValidationException.class,
            () -> fulfillmentService.createAssociation(1L, 1L, "WH-003"));

    assertTrue(exception.getMessage().contains("already has 2 warehouses fulfilling it"));
  }

  @Test
  void createAssociation_maxWarehousesPerStore_throwsException() {
    // Given - store already has 3 warehouses assigned
    when(entityManager.find(Product.class, 1L)).thenReturn(testProduct);
    when(entityManager.find(Store.class, 1L)).thenReturn(testStore);
    when(warehouseStore.findByBusinessUnitCode("WH-004")).thenReturn(testWarehouse);
    when(fulfillmentRepository.associationExists(1L, 1L, "WH-004")).thenReturn(false);
    when(fulfillmentRepository.warehouseFulfillsProductForStore(1L, 1L, "WH-004")).thenReturn(false);
    when(fulfillmentRepository.countDistinctWarehousesForProductAndStore(1L, 1L)).thenReturn(0L);
    when(fulfillmentRepository.warehouseAssociatedWithStore(1L, "WH-004")).thenReturn(false);
    when(fulfillmentRepository.countDistinctWarehousesForStore(1L)).thenReturn(3L);

    // When/Then
    FulfillmentValidationException exception =
        assertThrows(
            FulfillmentValidationException.class,
            () -> fulfillmentService.createAssociation(1L, 1L, "WH-004"));

    assertTrue(exception.getMessage().contains("already has 3 warehouses assigned"));
  }

  @Test
  void createAssociation_maxProductsPerWarehouse_throwsException() {
    // Given - warehouse already stores 5 product types
    when(entityManager.find(Product.class, 6L)).thenReturn(testProduct);
    when(entityManager.find(Store.class, 1L)).thenReturn(testStore);
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(testWarehouse);
    when(fulfillmentRepository.associationExists(6L, 1L, "WH-001")).thenReturn(false);
    when(fulfillmentRepository.warehouseFulfillsProductForStore(6L, 1L, "WH-001")).thenReturn(false);
    when(fulfillmentRepository.countDistinctWarehousesForProductAndStore(6L, 1L)).thenReturn(0L);
    when(fulfillmentRepository.warehouseAssociatedWithStore(1L, "WH-001")).thenReturn(true);
    when(fulfillmentRepository.productStoredInWarehouse(6L, "WH-001")).thenReturn(false);
    when(fulfillmentRepository.countDistinctProductsForWarehouse("WH-001")).thenReturn(5L);

    // When/Then
    FulfillmentValidationException exception =
        assertThrows(
            FulfillmentValidationException.class,
            () -> fulfillmentService.createAssociation(6L, 1L, "WH-001"));

    assertTrue(exception.getMessage().contains("already stores 5 product types"));
  }

  @Test
  void createAssociation_warehouseAlreadyAssociatedWithStore_skipsStoreConstraint() {
    // Given - warehouse is already associated with store (different product)
    when(entityManager.find(Product.class, 2L)).thenReturn(testProduct);
    when(entityManager.find(Store.class, 1L)).thenReturn(testStore);
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(testWarehouse);
    when(fulfillmentRepository.associationExists(2L, 1L, "WH-001")).thenReturn(false);
    when(fulfillmentRepository.warehouseFulfillsProductForStore(2L, 1L, "WH-001")).thenReturn(false);
    when(fulfillmentRepository.countDistinctWarehousesForProductAndStore(2L, 1L)).thenReturn(0L);
    when(fulfillmentRepository.warehouseAssociatedWithStore(1L, "WH-001")).thenReturn(true);
    when(fulfillmentRepository.productStoredInWarehouse(2L, "WH-001")).thenReturn(false);
    when(fulfillmentRepository.countDistinctProductsForWarehouse("WH-001")).thenReturn(1L);

    // When
    FulfillmentAssociation result = fulfillmentService.createAssociation(2L, 1L, "WH-001");

    // Then
    assertNotNull(result);
    verify(fulfillmentRepository).persist(any(FulfillmentAssociation.class));
  }

  @Test
  void deleteAssociation_success() {
    // Given
    when(fulfillmentRepository.deleteAssociation(1L, 1L, "WH-001")).thenReturn(true);

    // When
    boolean result = fulfillmentService.deleteAssociation(1L, 1L, "WH-001");

    // Then
    assertTrue(result);
  }

  @Test
  void deleteAssociation_notFound() {
    // Given
    when(fulfillmentRepository.deleteAssociation(999L, 999L, "WH-999")).thenReturn(false);

    // When
    boolean result = fulfillmentService.deleteAssociation(999L, 999L, "WH-999");

    // Then
    assertFalse(result);
  }
}
