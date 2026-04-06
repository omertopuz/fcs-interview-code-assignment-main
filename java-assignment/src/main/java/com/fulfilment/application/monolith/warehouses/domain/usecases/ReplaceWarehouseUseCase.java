package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    // Find the existing warehouse
    Warehouse existingWarehouse = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existingWarehouse == null || existingWarehouse.archivedAt != null) {
      throw new IllegalArgumentException("Warehouse not found: " + newWarehouse.businessUnitCode);
    }

    // 1. Validate location exists
    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new IllegalArgumentException("Invalid location: " + newWarehouse.location);
    }

    // 2. Check warehouse count at location (excluding current warehouse if moving to different location)
    if (!existingWarehouse.location.equals(newWarehouse.location)) {
      long warehouseCount = warehouseStore.countActiveByLocation(newWarehouse.location);
      if (warehouseCount >= location.maxNumberOfWarehouses) {
        throw new IllegalArgumentException("Maximum number of warehouses reached for location: " + newWarehouse.location);
      }
    }

    // 3. Validate capacity doesn't exceed location maximum
    if (newWarehouse.capacity > location.maxCapacity) {
      throw new IllegalArgumentException("Warehouse capacity exceeds location maximum capacity");
    }

    // 4. Validate new warehouse capacity can accommodate existing stock
    if (existingWarehouse.stock > newWarehouse.capacity) {
      throw new IllegalArgumentException("New warehouse capacity cannot accommodate existing stock");
    }

    // 5. Validate stock matches (replacement stocks should be the same)
    if (!existingWarehouse.stock.equals(newWarehouse.stock)) {
      throw new IllegalArgumentException("Stock of new warehouse must match the stock of warehouse being replaced");
    }

    // Update the warehouse
    newWarehouse.createdAt = existingWarehouse.createdAt;
    warehouseStore.update(newWarehouse);
  }
}
