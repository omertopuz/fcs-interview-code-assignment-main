package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsEqual.equalTo;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
public class WarehouseEndpointIT {

  private static final String BASE_PATH = "warehouse";

  @Test
  void shouldListAllWarehouses() {
    given()
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"));
  }

  @Test
  void shouldGetWarehouseById() {
    given()
        .when()
        .get(BASE_PATH + "/1")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("MWH.001"));
  }

  @Test
  void shouldReturn404ForNonExistentWarehouse() {
    given()
        .when()
        .get(BASE_PATH + "/9999")
        .then()
        .statusCode(404);
  }

  @Test
  void shouldCreateWarehouseWithValidData() {
    String json = """
        {
            "businessUnitCode": "MWH.NEWTEST",
            "location": "AMSTERDAM-001",
            "capacity": 50,
            "stock": 10
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(201)
        .body("businessUnitCode", equalTo("MWH.NEWTEST"));
  }

  @Test
  void shouldRejectInvalidLocation() {
    String json = """
        {
            "businessUnitCode": "MWH.INVALID",
            "location": "INVALID-LOCATION",
            "capacity": 50,
            "stock": 10
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(400);
  }

  @Test
  void shouldRejectCapacityExceedingLocationMax() {
    String json = """
        {
            "businessUnitCode": "MWH.OVERCAP",
            "location": "TILBURG-001",
            "capacity": 999,
            "stock": 10
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(400);
  }

  @Test
  void shouldRejectStockExceedingCapacity() {
    String json = """
        {
            "businessUnitCode": "MWH.OVERSTOCK",
            "location": "AMSTERDAM-001",
            "capacity": 50,
            "stock": 100
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(400);
  }

  @Test
  void shouldArchiveWarehouse() {
    // First, verify warehouse exists and is not archived
    given()
        .when()
        .get(BASE_PATH + "/3")
        .then()
        .statusCode(200);

    // Archive the warehouse
    given()
        .when()
        .delete(BASE_PATH + "/3")
        .then()
        .statusCode(204);
  }

  @Test
  void shouldReturn404WhenArchivingNonExistentWarehouse() {
    given()
        .when()
        .delete(BASE_PATH + "/9999")
        .then()
        .statusCode(404);
  }

  @Test
  void shouldReplaceWarehouse() {
    String json = """
        {
            "businessUnitCode": "MWH.001",
            "location": "ZWOLLE-001",
            "capacity": 100,
            "stock": 10
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .when()
        .post(BASE_PATH + "/MWH.001/replacement")
        .then()
        .statusCode(200);
  }

  @Test
  void shouldRejectReplaceWithMismatchedStock() {
    String json = """
        {
            "businessUnitCode": "MWH.012",
            "location": "AMSTERDAM-001",
            "capacity": 50,
            "stock": 999
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .when()
        .post(BASE_PATH + "/MWH.012/replacement")
        .then()
        .statusCode(400);
  }

  @Test
  void shouldRejectReplaceNonExistentWarehouse() {
    String json = """
        {
            "businessUnitCode": "MWH.NOTEXIST",
            "location": "AMSTERDAM-001",
            "capacity": 50,
            "stock": 10
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .when()
        .post(BASE_PATH + "/MWH.NOTEXIST/replacement")
        .then()
        .statusCode(400);
  }
}
