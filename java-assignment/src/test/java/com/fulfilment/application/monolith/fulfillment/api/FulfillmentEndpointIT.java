package com.fulfilment.application.monolith.fulfillment.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FulfillmentEndpointIT {

  @Test
  @Order(1)
  void getAllAssociations_returnsInitialData() {
    given()
        .when()
        .get("/fulfillment")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(2)
  void createAssociation_success() {
    String requestBody =
        """
        {
          "productId": 2,
          "storeId": 2,
          "warehouseBusinessUnitCode": "MWH.012"
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/fulfillment")
        .then()
        .statusCode(201)
        .body("productId", equalTo(2))
        .body("storeId", equalTo(2))
        .body("warehouseBusinessUnitCode", equalTo("MWH.012"));
  }

  @Test
  @Order(3)
  void createAssociation_duplicateFails() {
    String requestBody =
        """
        {
          "productId": 2,
          "storeId": 2,
          "warehouseBusinessUnitCode": "MWH.012"
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/fulfillment")
        .then()
        .statusCode(400)
        .body("error", containsString("Association already exists"));
  }

  @Test
  @Order(4)
  void createAssociation_invalidProduct_returns400() {
    String requestBody =
        """
        {
          "productId": 9999,
          "storeId": 1,
          "warehouseBusinessUnitCode": "MWH.001"
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/fulfillment")
        .then()
        .statusCode(400)
        .body("error", containsString("Product with id 9999 not found"));
  }

  @Test
  @Order(5)
  void createAssociation_invalidStore_returns400() {
    String requestBody =
        """
        {
          "productId": 1,
          "storeId": 9999,
          "warehouseBusinessUnitCode": "MWH.001"
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/fulfillment")
        .then()
        .statusCode(400)
        .body("error", containsString("Store with id 9999 not found"));
  }

  @Test
  @Order(6)
  void createAssociation_invalidWarehouse_returns400() {
    String requestBody =
        """
        {
          "productId": 1,
          "storeId": 1,
          "warehouseBusinessUnitCode": "INVALID"
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/fulfillment")
        .then()
        .statusCode(400)
        .body("error", containsString("Warehouse with business unit code INVALID not found"));
  }

  @Test
  @Order(7)
  void getAssociationsByStore_success() {
    given()
        .when()
        .get("/fulfillment/store/1")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(8)
  void getAssociationsByProduct_success() {
    given()
        .when()
        .get("/fulfillment/product/1")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(9)
  void getAssociationsByWarehouse_success() {
    given()
        .when()
        .get("/fulfillment/warehouse/MWH.001")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(10)
  void deleteAssociation_success() {
    // First create an association to delete
    String requestBody =
        """
        {
          "productId": 3,
          "storeId": 3,
          "warehouseBusinessUnitCode": "MWH.023"
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/fulfillment")
        .then()
        .statusCode(201);

    // Then delete it
    given()
        .queryParam("productId", 3)
        .queryParam("storeId", 3)
        .queryParam("warehouseBusinessUnitCode", "MWH.023")
        .when()
        .delete("/fulfillment")
        .then()
        .statusCode(204);
  }

  @Test
  @Order(11)
  void deleteAssociation_notFound_returns404() {
    given()
        .queryParam("productId", 9999)
        .queryParam("storeId", 9999)
        .queryParam("warehouseBusinessUnitCode", "INVALID")
        .when()
        .delete("/fulfillment")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(12)
  void deleteAssociation_missingParams_returns400() {
    given()
        .queryParam("productId", 1)
        .when()
        .delete("/fulfillment")
        .then()
        .statusCode(400)
        .body("error", containsString("required"));
  }

  @Test
  @Order(13)
  void createAssociation_maxWarehousesPerProductStore_returns400() {
    // Create first association for product 1, store 2
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "productId": 1,
              "storeId": 2,
              "warehouseBusinessUnitCode": "MWH.001"
            }
            """)
        .when()
        .post("/fulfillment")
        .then()
        .statusCode(201);

    // Create second association (max 2 warehouses per product-store)
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "productId": 1,
              "storeId": 2,
              "warehouseBusinessUnitCode": "MWH.012"
            }
            """)
        .when()
        .post("/fulfillment")
        .then()
        .statusCode(201);

    // Try to create third association - should fail
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "productId": 1,
              "storeId": 2,
              "warehouseBusinessUnitCode": "MWH.023"
            }
            """)
        .when()
        .post("/fulfillment")
        .then()
        .statusCode(400)
        .body("error", containsString("already has 2 warehouses fulfilling it"));
  }
}
