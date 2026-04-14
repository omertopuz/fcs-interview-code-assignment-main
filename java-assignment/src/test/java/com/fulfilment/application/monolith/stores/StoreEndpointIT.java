package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StoreEndpointIT {

  private static final String PATH = "store";

  @Test
  @Order(1)
  public void testListAllStores() {
    // Verify initial data from import.sql
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));
  }

  @Test
  @Order(2)
  public void testGetStoreById() {
    given()
        .when()
        .get(PATH + "/1")
        .then()
        .statusCode(200)
        .body("name", equalTo("TONSTAD"))
        .body("quantityProductsInStock", equalTo(10));
  }

  @Test
  @Order(3)
  public void testGetStoreByIdNotFound() {
    given()
        .when()
        .get(PATH + "/999")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(4)
  public void testCreateStore() {
    String newStore = """
        {
          "name": "NEW_STORE",
          "quantityProductsInStock": 25
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(newStore)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("name", equalTo("NEW_STORE"))
        .body("quantityProductsInStock", equalTo(25))
        .body("id", notNullValue());
  }

  @Test
  @Order(5)
  public void testCreateStoreWithIdFails() {
    String storeWithId = """
        {
          "id": 100,
          "name": "INVALID_STORE",
          "quantityProductsInStock": 10
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(storeWithId)
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
  }

  @Test
  @Order(6)
  public void testUpdateStore() {
    String updatedStore = """
        {
          "name": "UPDATED_TONSTAD",
          "quantityProductsInStock": 15
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(updatedStore)
        .when()
        .put(PATH + "/1")
        .then()
        .statusCode(200)
        .body("name", equalTo("UPDATED_TONSTAD"))
        .body("quantityProductsInStock", equalTo(15));
  }

  @Test
  @Order(7)
  public void testUpdateStoreNotFound() {
    String updatedStore = """
        {
          "name": "NONEXISTENT",
          "quantityProductsInStock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(updatedStore)
        .when()
        .put(PATH + "/999")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(8)
  public void testUpdateStoreWithoutNameFails() {
    String invalidStore = """
        {
          "quantityProductsInStock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(invalidStore)
        .when()
        .put(PATH + "/1")
        .then()
        .statusCode(422);
  }

  @Test
  @Order(9)
  public void testPatchStore() {
    String patchData = """
        {
          "name": "PATCHED_KALLAX",
          "quantityProductsInStock": 20
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(patchData)
        .when()
        .patch(PATH + "/2")
        .then()
        .statusCode(200)
        .body("name", equalTo("PATCHED_KALLAX"));
  }

  @Test
  @Order(10)
  public void testPatchStoreNotFound() {
    String patchData = """
        {
          "name": "NONEXISTENT",
          "quantityProductsInStock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(patchData)
        .when()
        .patch(PATH + "/999")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(11)
  public void testDeleteStore() {
    given()
        .when()
        .delete(PATH + "/3")
        .then()
        .statusCode(204);

    // Verify deletion
    given()
        .when()
        .get(PATH + "/3")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(12)
  public void testDeleteStoreNotFound() {
    given()
        .when()
        .delete(PATH + "/999")
        .then()
        .statusCode(404);
  }
}
