package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WarehouseEndpointIT {

  private static final String PATH = "warehouse";

  @Test
  @Order(1)
  public void testListAllWarehouses() {
    // Verify initial data from import.sql
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  @Order(2)
  public void testGetWarehouseByBusinessUnitCode() {
    given()
        .when()
        .get(PATH + "/MWH.001")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("MWH.001"))
        .body("location", equalTo("ZWOLLE-001"))
        .body("capacity", equalTo(100))
        .body("stock", equalTo(10));
  }

  @Test
  @Order(3)
  public void testGetWarehouseByIdNotFound() {
    given()
        .when()
        .get(PATH + "/NONEXISTENT")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(4)
  public void testCreateWarehouse() {
    // Use HELMOND-001 which is unused and has max capacity 45
    String newWarehouse = """
        {
          "businessUnitCode": "MWH.100",
          "location": "HELMOND-001",
          "capacity": 40,
          "stock": 0
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(newWarehouse)
        .when()
        .post(PATH)
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("MWH.100"))
        .body("location", equalTo("HELMOND-001"))
        .body("capacity", equalTo(40));

    // Verify creation
    given()
        .when()
        .get(PATH + "/MWH.100")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("MWH.100"));
  }

  @Test
  @Order(5)
  public void testCreateWarehouseWithInvalidDataFails() {
    // Test with empty businessUnitCode
    String invalidWarehouse = """
        {
          "businessUnitCode": "",
          "location": "INVALID-001",
          "capacity": 100,
          "stock": 0
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(invalidWarehouse)
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  @Order(6)
  public void testReplaceWarehouse() {
    // MWH.012 has stock=5, so replacement must accommodate that stock
    // Using AMSTERDAM-002 which has max capacity 75
    String replacementWarehouse = """
        {
          "businessUnitCode": "MWH.012",
          "location": "AMSTERDAM-002",
          "capacity": 60,
          "stock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(replacementWarehouse)
        .when()
        .post(PATH + "/MWH.012/replacement")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("MWH.012"))
        .body("location", equalTo("AMSTERDAM-002"))
        .body("capacity", equalTo(60));
  }

  @Test
  @Order(7)
  public void testReplaceWarehouseNotFound() {
    String replacementWarehouse = """
        {
          "businessUnitCode": "NONEXISTENT",
          "location": "NOWHERE-001",
          "capacity": 100,
          "stock": 0
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(replacementWarehouse)
        .when()
        .post(PATH + "/NONEXISTENT/replacement")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(8)
  public void testArchiveWarehouse() {
    given()
        .when()
        .delete(PATH + "/MWH.023")
        .then()
        .statusCode(204);

    // Verify archival - warehouse should no longer be found
    given()
        .when()
        .get(PATH + "/MWH.023")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(9)
  public void testArchiveWarehouseNotFound() {
    given()
        .when()
        .delete(PATH + "/NONEXISTENT")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(10)
  public void testListWarehousesAfterModifications() {
    // After previous tests: 
    // - MWH.001 exists at ZWOLLE-001
    // - MWH.012 was replaced at AMSTERDAM-002 
    // - MWH.023 was archived
    // - MWH.100 was created at HELMOND-001
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"), 
            containsString("MWH.012"), 
            containsString("MWH.100"),
            containsString("AMSTERDAM-002"),
            containsString("HELMOND-001"))
        .body(not(containsString("TILBURG-001"))); // MWH.023 was archived
  }
}
