package com.fulfilment.application.monolith.fulfillment.domain;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class FulfillmentRepository implements PanacheRepository<FulfillmentAssociation> {

  /**
   * Count distinct warehouses that fulfill a specific product for a specific store.
   * Constraint: Each Product can be fulfilled by max 2 different Warehouses per Store.
   */
  public long countDistinctWarehousesForProductAndStore(Long productId, Long storeId) {
    return find("productId = ?1 and storeId = ?2", productId, storeId)
        .stream()
        .map(fa -> fa.warehouseBusinessUnitCode)
        .distinct()
        .count();
  }

  /**
   * Count distinct warehouses that fulfill any product for a specific store.
   * Constraint: Each Store can be fulfilled by max 3 different Warehouses.
   */
  public long countDistinctWarehousesForStore(Long storeId) {
    return find("storeId", storeId)
        .stream()
        .map(fa -> fa.warehouseBusinessUnitCode)
        .distinct()
        .count();
  }

  /**
   * Count distinct products stored in a specific warehouse.
   * Constraint: Each Warehouse can store max 5 types of Products.
   */
  public long countDistinctProductsForWarehouse(String warehouseBusinessUnitCode) {
    return find("warehouseBusinessUnitCode", warehouseBusinessUnitCode)
        .stream()
        .map(fa -> fa.productId)
        .distinct()
        .count();
  }

  /**
   * Check if an association already exists.
   */
  public boolean associationExists(Long productId, Long storeId, String warehouseBusinessUnitCode) {
    return find(
            "productId = ?1 and storeId = ?2 and warehouseBusinessUnitCode = ?3",
            productId,
            storeId,
            warehouseBusinessUnitCode)
        .firstResultOptional()
        .isPresent();
  }

  /**
   * Check if a warehouse is already associated with a store (regardless of product).
   */
  public boolean warehouseAssociatedWithStore(Long storeId, String warehouseBusinessUnitCode) {
    return find(
            "storeId = ?1 and warehouseBusinessUnitCode = ?2",
            storeId,
            warehouseBusinessUnitCode)
        .firstResultOptional()
        .isPresent();
  }

  /**
   * Check if a product is already stored in a warehouse (regardless of store).
   */
  public boolean productStoredInWarehouse(Long productId, String warehouseBusinessUnitCode) {
    return find(
            "productId = ?1 and warehouseBusinessUnitCode = ?2",
            productId,
            warehouseBusinessUnitCode)
        .firstResultOptional()
        .isPresent();
  }

  /**
   * Check if a warehouse already fulfills a product for a store.
   */
  public boolean warehouseFulfillsProductForStore(
      Long productId, Long storeId, String warehouseBusinessUnitCode) {
    return find(
            "productId = ?1 and storeId = ?2 and warehouseBusinessUnitCode = ?3",
            productId,
            storeId,
            warehouseBusinessUnitCode)
        .firstResultOptional()
        .isPresent();
  }

  /**
   * Get all associations for a specific store.
   */
  public List<FulfillmentAssociation> findByStoreId(Long storeId) {
    return find("storeId", storeId).list();
  }

  /**
   * Get all associations for a specific product.
   */
  public List<FulfillmentAssociation> findByProductId(Long productId) {
    return find("productId", productId).list();
  }

  /**
   * Get all associations for a specific warehouse.
   */
  public List<FulfillmentAssociation> findByWarehouseBusinessUnitCode(
      String warehouseBusinessUnitCode) {
    return find("warehouseBusinessUnitCode", warehouseBusinessUnitCode).list();
  }

  /**
   * Delete an association by its composite key.
   */
  public boolean deleteAssociation(
      Long productId, Long storeId, String warehouseBusinessUnitCode) {
    return delete(
            "productId = ?1 and storeId = ?2 and warehouseBusinessUnitCode = ?3",
            productId,
            storeId,
            warehouseBusinessUnitCode)
        > 0;
  }
}
