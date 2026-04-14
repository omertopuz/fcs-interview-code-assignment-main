package com.fulfilment.application.monolith.fulfillment.domain;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * Service to manage fulfillment associations between Products, Stores, and Warehouses.
 * Enforces the following constraints:
 * 1. Each Product can be fulfilled by max 2 different Warehouses per Store
 * 2. Each Store can be fulfilled by max 3 different Warehouses
 * 3. Each Warehouse can store max 5 types of Products
 */
@ApplicationScoped
public class FulfillmentService {

  public static final int MAX_WAREHOUSES_PER_PRODUCT_STORE = 2;
  public static final int MAX_WAREHOUSES_PER_STORE = 3;
  public static final int MAX_PRODUCTS_PER_WAREHOUSE = 5;

  @Inject FulfillmentRepository fulfillmentRepository;

  @Inject WarehouseStore warehouseStore;

  @Inject EntityManager entityManager;

  /**
   * Create a new fulfillment association.
   * @param productId The product ID
   * @param storeId The store ID
   * @param warehouseBusinessUnitCode The warehouse business unit code
   * @return The created association
   * @throws FulfillmentValidationException if any constraint is violated
   */
  @Transactional
  public FulfillmentAssociation createAssociation(
      Long productId, Long storeId, String warehouseBusinessUnitCode) {

    // Validate entities exist
    validateEntitiesExist(productId, storeId, warehouseBusinessUnitCode);

    // Check if association already exists
    if (fulfillmentRepository.associationExists(productId, storeId, warehouseBusinessUnitCode)) {
      throw new FulfillmentValidationException(
          "Association already exists for product " + productId + ", store " + storeId
              + ", and warehouse " + warehouseBusinessUnitCode);
    }

    // Constraint 1: Each Product can be fulfilled by max 2 different Warehouses per Store
    if (!fulfillmentRepository.warehouseFulfillsProductForStore(
        productId, storeId, warehouseBusinessUnitCode)) {
      long warehouseCount =
          fulfillmentRepository.countDistinctWarehousesForProductAndStore(productId, storeId);
      if (warehouseCount >= MAX_WAREHOUSES_PER_PRODUCT_STORE) {
        throw new FulfillmentValidationException(
            "Product " + productId + " already has " + MAX_WAREHOUSES_PER_PRODUCT_STORE
                + " warehouses fulfilling it for store " + storeId);
      }
    }

    // Constraint 2: Each Store can be fulfilled by max 3 different Warehouses
    if (!fulfillmentRepository.warehouseAssociatedWithStore(storeId, warehouseBusinessUnitCode)) {
      long storeWarehouseCount = fulfillmentRepository.countDistinctWarehousesForStore(storeId);
      if (storeWarehouseCount >= MAX_WAREHOUSES_PER_STORE) {
        throw new FulfillmentValidationException(
            "Store " + storeId + " already has " + MAX_WAREHOUSES_PER_STORE
                + " warehouses assigned");
      }
    }

    // Constraint 3: Each Warehouse can store max 5 types of Products
    if (!fulfillmentRepository.productStoredInWarehouse(productId, warehouseBusinessUnitCode)) {
      long warehouseProductCount =
          fulfillmentRepository.countDistinctProductsForWarehouse(warehouseBusinessUnitCode);
      if (warehouseProductCount >= MAX_PRODUCTS_PER_WAREHOUSE) {
        throw new FulfillmentValidationException(
            "Warehouse " + warehouseBusinessUnitCode + " already stores "
                + MAX_PRODUCTS_PER_WAREHOUSE + " product types");
      }
    }

    // Create and persist the association
    FulfillmentAssociation association =
        new FulfillmentAssociation(productId, storeId, warehouseBusinessUnitCode);
    fulfillmentRepository.persist(association);
    return association;
  }

  /**
   * Delete a fulfillment association.
   */
  @Transactional
  public boolean deleteAssociation(
      Long productId, Long storeId, String warehouseBusinessUnitCode) {
    return fulfillmentRepository.deleteAssociation(productId, storeId, warehouseBusinessUnitCode);
  }

  /**
   * Get all fulfillment associations.
   */
  public List<FulfillmentAssociation> getAllAssociations() {
    return fulfillmentRepository.listAll();
  }

  /**
   * Get associations by store.
   */
  public List<FulfillmentAssociation> getAssociationsByStore(Long storeId) {
    return fulfillmentRepository.findByStoreId(storeId);
  }

  /**
   * Get associations by product.
   */
  public List<FulfillmentAssociation> getAssociationsByProduct(Long productId) {
    return fulfillmentRepository.findByProductId(productId);
  }

  /**
   * Get associations by warehouse.
   */
  public List<FulfillmentAssociation> getAssociationsByWarehouse(String warehouseBusinessUnitCode) {
    return fulfillmentRepository.findByWarehouseBusinessUnitCode(warehouseBusinessUnitCode);
  }

  private void validateEntitiesExist(
      Long productId, Long storeId, String warehouseBusinessUnitCode) {
    // Validate product exists
    Product product = entityManager.find(Product.class, productId);
    if (product == null) {
      throw new FulfillmentValidationException("Product with id " + productId + " not found");
    }

    // Validate store exists
    Store store = entityManager.find(Store.class, storeId);
    if (store == null) {
      throw new FulfillmentValidationException("Store with id " + storeId + " not found");
    }

    // Validate warehouse exists and is active
    var warehouse = warehouseStore.findByBusinessUnitCode(warehouseBusinessUnitCode);
    if (warehouse == null) {
      throw new FulfillmentValidationException(
          "Warehouse with business unit code " + warehouseBusinessUnitCode + " not found");
    }
  }
}
