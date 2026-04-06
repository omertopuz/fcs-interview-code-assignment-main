package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private final WarehouseStore warehouseStore;

  public ArchiveWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  public void archive(Warehouse warehouse) {
    // Validate warehouse exists
    Warehouse existing = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existing == null || existing.archivedAt != null) {
      throw new IllegalArgumentException("Warehouse not found: " + warehouse.businessUnitCode);
    }

    // Set archive timestamp
    warehouse.archivedAt = LocalDateTime.now();
    warehouse.createdAt = existing.createdAt;
    warehouse.location = existing.location;
    warehouse.capacity = existing.capacity;
    warehouse.stock = existing.stock;

    warehouseStore.update(warehouse);
  }
}
