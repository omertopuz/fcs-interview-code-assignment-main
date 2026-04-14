package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.WarehouseNotFoundException;
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
    // Find the existing warehouse
    Warehouse existingWarehouse =
        warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existingWarehouse == null) {
      throw new WarehouseNotFoundException(
          "Warehouse not found with business unit code: " + warehouse.businessUnitCode);
    }

    // Set archive timestamp
    existingWarehouse.archivedAt = LocalDateTime.now();

    // Update the warehouse with archived status
    warehouseStore.update(existingWarehouse);
  }
}
