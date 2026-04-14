package com.fulfilment.application.monolith.fulfillment.api;

import com.fulfilment.application.monolith.fulfillment.domain.FulfillmentAssociation;
import com.fulfilment.application.monolith.fulfillment.domain.FulfillmentService;
import com.fulfilment.application.monolith.fulfillment.domain.FulfillmentValidationException;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * REST resource for managing fulfillment associations between Products, Stores, and Warehouses.
 */
@Path("/fulfillment")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FulfillmentResource {

  @Inject FulfillmentService fulfillmentService;

  /**
   * Get all fulfillment associations.
   */
  @GET
  public List<FulfillmentAssociationDto> getAllAssociations() {
    return fulfillmentService.getAllAssociations().stream()
        .map(FulfillmentAssociationDto::from)
        .toList();
  }

  /**
   * Get fulfillment associations by store.
   */
  @GET
  @Path("/store/{storeId}")
  public List<FulfillmentAssociationDto> getAssociationsByStore(@PathParam("storeId") Long storeId) {
    return fulfillmentService.getAssociationsByStore(storeId).stream()
        .map(FulfillmentAssociationDto::from)
        .toList();
  }

  /**
   * Get fulfillment associations by product.
   */
  @GET
  @Path("/product/{productId}")
  public List<FulfillmentAssociationDto> getAssociationsByProduct(
      @PathParam("productId") Long productId) {
    return fulfillmentService.getAssociationsByProduct(productId).stream()
        .map(FulfillmentAssociationDto::from)
        .toList();
  }

  /**
   * Get fulfillment associations by warehouse.
   */
  @GET
  @Path("/warehouse/{warehouseBusinessUnitCode}")
  public List<FulfillmentAssociationDto> getAssociationsByWarehouse(
      @PathParam("warehouseBusinessUnitCode") String warehouseBusinessUnitCode) {
    return fulfillmentService.getAssociationsByWarehouse(warehouseBusinessUnitCode).stream()
        .map(FulfillmentAssociationDto::from)
        .toList();
  }

  /**
   * Create a new fulfillment association.
   * Associates a warehouse as a fulfillment unit for a product to a store.
   */
  @POST
  public Response createAssociation(CreateFulfillmentRequest request) {
    try {
      FulfillmentAssociation association =
          fulfillmentService.createAssociation(
              request.productId, request.storeId, request.warehouseBusinessUnitCode);
      return Response.status(Response.Status.CREATED)
          .entity(FulfillmentAssociationDto.from(association))
          .build();
    } catch (FulfillmentValidationException e) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ErrorResponse(e.getMessage()))
          .build();
    }
  }

  /**
   * Delete a fulfillment association.
   */
  @DELETE
  public Response deleteAssociation(
      @QueryParam("productId") Long productId,
      @QueryParam("storeId") Long storeId,
      @QueryParam("warehouseBusinessUnitCode") String warehouseBusinessUnitCode) {

    if (productId == null || storeId == null || warehouseBusinessUnitCode == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ErrorResponse("productId, storeId, and warehouseBusinessUnitCode are required"))
          .build();
    }

    boolean deleted =
        fulfillmentService.deleteAssociation(productId, storeId, warehouseBusinessUnitCode);

    if (deleted) {
      return Response.noContent().build();
    } else {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse("Association not found"))
          .build();
    }
  }

  /** DTO for creating a fulfillment association */
  public static class CreateFulfillmentRequest {
    public Long productId;
    public Long storeId;
    public String warehouseBusinessUnitCode;
  }

  /** DTO for fulfillment association response */
  public static class FulfillmentAssociationDto {
    public Long id;
    public Long productId;
    public Long storeId;
    public String warehouseBusinessUnitCode;
    public String createdAt;

    public static FulfillmentAssociationDto from(FulfillmentAssociation association) {
      FulfillmentAssociationDto dto = new FulfillmentAssociationDto();
      dto.id = association.id;
      dto.productId = association.productId;
      dto.storeId = association.storeId;
      dto.warehouseBusinessUnitCode = association.warehouseBusinessUnitCode;
      dto.createdAt = association.createdAt != null ? association.createdAt.toString() : null;
      return dto;
    }
  }

  /** Error response DTO */
  public static class ErrorResponse {
    public String error;

    public ErrorResponse(String error) {
      this.error = error;
    }
  }
}
