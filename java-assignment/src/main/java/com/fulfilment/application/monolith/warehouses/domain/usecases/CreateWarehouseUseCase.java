package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseRepository warehouseRepository;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(
      WarehouseRepository warehouseRepository, LocationResolver locationResolver) {
    this.warehouseRepository = warehouseRepository;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    // Validation 1: Business Unit Code Verification
    // Ensure that the specified business unit code for the warehouse doesn't already exist
    Warehouse existingWarehouse =
        warehouseRepository.findAnyByBusinessUnitCode(warehouse.businessUnitCode);
    if (existingWarehouse != null) {
      throw new WarehouseValidationException(
          "Business unit code already exists: " + warehouse.businessUnitCode);
    }

    // Validation 2: Location Validation
    // Confirm that the warehouse location is valid (must be an existing valid location)
    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new WarehouseValidationException("Invalid location: " + warehouse.location);
    }

    // Validation 3: Warehouse Creation Feasibility
    // Check if a new warehouse can be created at the specified location
    // or if the maximum number of warehouses has already been reached
    List<Warehouse> activeWarehousesAtLocation =
        warehouseRepository.findActiveByLocation(warehouse.location);
    if (activeWarehousesAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WarehouseValidationException(
          "Maximum number of warehouses reached for location: " + warehouse.location);
    }

    // Validation 4: Capacity and Stock Validation
    // Validate the warehouse capacity doesn't exceed the maximum capacity associated with the
    // location
    int currentTotalCapacity =
        activeWarehousesAtLocation.stream().mapToInt(w -> w.capacity).sum();
    if (currentTotalCapacity + warehouse.capacity > location.maxCapacity) {
      throw new WarehouseValidationException(
          "Warehouse capacity exceeds location maximum capacity. Available capacity: "
              + (location.maxCapacity - currentTotalCapacity));
    }

    // Validate that capacity can handle the stock informed
    if (warehouse.stock != null && warehouse.capacity != null && warehouse.stock > warehouse.capacity) {
      throw new WarehouseValidationException(
          "Stock cannot exceed warehouse capacity. Stock: "
              + warehouse.stock
              + ", Capacity: "
              + warehouse.capacity);
    }

    // Set creation timestamp
    warehouse.createdAt = LocalDateTime.now();

    // If all validations pass, create the warehouse
    warehouseRepository.create(warehouse);
  }
}
