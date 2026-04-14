package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject private WarehouseRepository warehouseRepository;

  @Inject private CreateWarehouseOperation createWarehouseOperation;

  @Inject private ArchiveWarehouseOperation archiveWarehouseOperation;

  @Inject private ReplaceWarehouseOperation replaceWarehouseOperation;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    try {
      var domainWarehouse = toDomainWarehouse(data);
      createWarehouseOperation.create(domainWarehouse);

      // Fetch the created warehouse to return it
      var createdWarehouse =
          warehouseRepository.findByBusinessUnitCode(data.getBusinessUnitCode());
      return toWarehouseResponse(createdWarehouse);
    } catch (WarehouseValidationException e) {
      throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
    }
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    var warehouse = warehouseRepository.findByBusinessUnitCode(id);
    if (warehouse == null) {
      throw new WebApplicationException(
          "Warehouse not found with id: " + id, Response.Status.NOT_FOUND);
    }
    return toWarehouseResponse(warehouse);
  }

  @Override
  public void archiveAWarehouseUnitByID(String id) {
    try {
      var warehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
      warehouse.businessUnitCode = id;
      archiveWarehouseOperation.archive(warehouse);
    } catch (WarehouseNotFoundException e) {
      throw new WebApplicationException(e.getMessage(), Response.Status.NOT_FOUND);
    }
  }

  @Override
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    try {
      var newWarehouse = toDomainWarehouse(data);
      newWarehouse.businessUnitCode = businessUnitCode;
      replaceWarehouseOperation.replace(newWarehouse);

      // Fetch the created warehouse to return it
      var createdWarehouse = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
      return toWarehouseResponse(createdWarehouse);
    } catch (WarehouseNotFoundException e) {
      throw new WebApplicationException(e.getMessage(), Response.Status.NOT_FOUND);
    } catch (WarehouseValidationException e) {
      throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
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
    var warehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse.businessUnitCode = apiWarehouse.getBusinessUnitCode();
    warehouse.location = apiWarehouse.getLocation();
    warehouse.capacity = apiWarehouse.getCapacity();
    warehouse.stock = apiWarehouse.getStock();
    return warehouse;
  }
}
