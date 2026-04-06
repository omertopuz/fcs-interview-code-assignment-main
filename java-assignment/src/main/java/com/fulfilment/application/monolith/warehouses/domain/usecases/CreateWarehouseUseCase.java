package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    // 1. Validate business unit code doesn't already exist (for active warehouses)
    var existing = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existing != null && existing.archivedAt == null) {
      throw new IllegalArgumentException("Business unit code already exists: " + warehouse.businessUnitCode);
    }

    // 2. Validate location exists
    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new IllegalArgumentException("Invalid location: " + warehouse.location);
    }

    // 3. Check warehouse count at location hasn't reached maximum
    long warehouseCount = warehouseStore.countActiveByLocation(warehouse.location);
    if (warehouseCount >= location.maxNumberOfWarehouses) {
      throw new IllegalArgumentException("Maximum number of warehouses reached for location: " + warehouse.location);
    }

    // 4. Validate capacity doesn't exceed location maximum
    if (warehouse.capacity > location.maxCapacity) {
      throw new IllegalArgumentException("Warehouse capacity exceeds location maximum capacity");
    }

    // 5. Validate stock doesn't exceed warehouse capacity
    if (warehouse.stock > warehouse.capacity) {
      throw new IllegalArgumentException("Stock cannot exceed warehouse capacity");
    }

    // Set creation timestamp
    warehouse.createdAt = LocalDateTime.now();

    // if all validations passed, create the warehouse
    warehouseStore.create(warehouse);
  }
}
