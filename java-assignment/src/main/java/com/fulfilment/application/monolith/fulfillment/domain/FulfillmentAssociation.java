package com.fulfilment.application.monolith.fulfillment.domain;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * Represents an association between a Product, Store, and Warehouse for fulfillment.
 * This entity tracks which warehouses can fulfill which products for which stores.
 */
@Entity
@Table(
    name = "fulfillment_association",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"productId", "storeId", "warehouseBusinessUnitCode"})
    })
@Cacheable
public class FulfillmentAssociation {

  @Id @GeneratedValue public Long id;

  @Column(nullable = false)
  public Long productId;

  @Column(nullable = false)
  public Long storeId;

  @Column(nullable = false)
  public String warehouseBusinessUnitCode;

  @Column(nullable = false)
  public LocalDateTime createdAt;

  public FulfillmentAssociation() {}

  public FulfillmentAssociation(Long productId, Long storeId, String warehouseBusinessUnitCode) {
    this.productId = productId;
    this.storeId = storeId;
    this.warehouseBusinessUnitCode = warehouseBusinessUnitCode;
    this.createdAt = LocalDateTime.now();
  }
}
