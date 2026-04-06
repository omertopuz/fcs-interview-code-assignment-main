package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ArchiveWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.CreateWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ReplaceWarehouseUseCase;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject private WarehouseRepository warehouseRepository;
  @Inject private CreateWarehouseUseCase createWarehouseUseCase;
  @Inject private ReplaceWarehouseUseCase replaceWarehouseUseCase;
  @Inject private ArchiveWarehouseUseCase archiveWarehouseUseCase;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  @Transactional
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    try {
      var domainWarehouse = toDomainWarehouse(data);
      createWarehouseUseCase.create(domainWarehouse);
      var created = warehouseRepository.findByBusinessUnitCode(data.getBusinessUnitCode());
      return toWarehouseResponse(created);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    }
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    try {
      Long warehouseId = Long.parseLong(id);
      var warehouse = warehouseRepository.getWarehouseById(warehouseId);
      if (warehouse == null) {
        throw new WebApplicationException("Warehouse not found", 404);
      }
      return toWarehouseResponse(warehouse);
    } catch (NumberFormatException e) {
      throw new WebApplicationException("Invalid warehouse ID format", 400);
    }
  }

  @Override
  @Transactional
  public void archiveAWarehouseUnitByID(String id) {
    try {
      Long warehouseId = Long.parseLong(id);
      var warehouse = warehouseRepository.getWarehouseById(warehouseId);
      if (warehouse == null) {
        throw new WebApplicationException("Warehouse not found", 404);
      }
      archiveWarehouseUseCase.archive(warehouse);
    } catch (NumberFormatException e) {
      throw new WebApplicationException("Invalid warehouse ID format", 400);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    }
  }

  @Override
  @Transactional
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    try {
      var domainWarehouse = toDomainWarehouse(data);
      replaceWarehouseUseCase.replace(domainWarehouse);
      var updated = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
      return toWarehouseResponse(updated);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    }
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);

    return response;
  }

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse toDomainWarehouse(
      Warehouse apiWarehouse) {
    var domainWarehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    domainWarehouse.businessUnitCode = apiWarehouse.getBusinessUnitCode();
    domainWarehouse.location = apiWarehouse.getLocation();
    domainWarehouse.capacity = apiWarehouse.getCapacity();
    domainWarehouse.stock = apiWarehouse.getStock();
    return domainWarehouse;
  }
}
