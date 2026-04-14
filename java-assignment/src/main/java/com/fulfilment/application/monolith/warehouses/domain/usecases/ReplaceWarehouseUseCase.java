package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseRepository warehouseRepository;
  private final LocationResolver locationResolver;
  private final ArchiveWarehouseOperation archiveWarehouseOperation;

  public ReplaceWarehouseUseCase(
      WarehouseRepository warehouseRepository,
      LocationResolver locationResolver,
      ArchiveWarehouseOperation archiveWarehouseOperation) {
    this.warehouseRepository = warehouseRepository;
    this.locationResolver = locationResolver;
    this.archiveWarehouseOperation = archiveWarehouseOperation;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    // Find the existing warehouse that is being replaced
    Warehouse existingWarehouse =
        warehouseRepository.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existingWarehouse == null) {
      throw new WarehouseNotFoundException(
          "Warehouse not found with business unit code: " + newWarehouse.businessUnitCode);
    }

    // Validation: Location Validation
    Location newLocation = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (newLocation == null) {
      throw new WarehouseValidationException("Invalid location: " + newWarehouse.location);
    }

    // Validation: Capacity Accommodation
    // Ensure the new warehouse's capacity can accommodate the stock from the warehouse being
    // replaced
    if (newWarehouse.capacity < existingWarehouse.stock) {
      throw new WarehouseValidationException(
          "New warehouse capacity ("
              + newWarehouse.capacity
              + ") cannot accommodate the stock from the warehouse being replaced ("
              + existingWarehouse.stock
              + ")");
    }

    // Validation: Stock Matching
    // Confirm that the stock of the new warehouse matches the stock of the previous warehouse
    if (newWarehouse.stock != null
        && !newWarehouse.stock.equals(existingWarehouse.stock)) {
      throw new WarehouseValidationException(
          "Stock of the new warehouse ("
              + newWarehouse.stock
              + ") must match the stock of the previous warehouse ("
              + existingWarehouse.stock
              + ")");
    }

    // Validate capacity doesn't exceed location's max capacity (considering old warehouse is being
    // replaced)
    List<Warehouse> activeWarehousesAtLocation =
        warehouseRepository.findActiveByLocation(newWarehouse.location);
    int currentTotalCapacity =
        activeWarehousesAtLocation.stream()
            .filter(w -> !w.businessUnitCode.equals(newWarehouse.businessUnitCode))
            .mapToInt(w -> w.capacity)
            .sum();
    if (currentTotalCapacity + newWarehouse.capacity > newLocation.maxCapacity) {
      throw new WarehouseValidationException(
          "New warehouse capacity exceeds location maximum capacity. Available capacity: "
              + (newLocation.maxCapacity - currentTotalCapacity));
    }

    // Archive the existing warehouse
    archiveWarehouseOperation.archive(existingWarehouse);

    // Set the stock to match the previous warehouse if not explicitly set
    if (newWarehouse.stock == null) {
      newWarehouse.stock = existingWarehouse.stock;
    }

    // Set creation timestamp for the new warehouse
    newWarehouse.createdAt = LocalDateTime.now();

    // Create the new warehouse with the same business unit code
    warehouseRepository.create(newWarehouse);
  }
}
