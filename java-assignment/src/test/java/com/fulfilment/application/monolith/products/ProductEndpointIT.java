package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductEndpointIT {

  private static final String PATH = "product";

  @Test
  @Order(1)
  public void testListAllProducts() {
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
  public void testGetProductById() {
    given()
        .when()
        .get(PATH + "/1")
        .then()
        .statusCode(200)
        .body("name", equalTo("TONSTAD"))
        .body("stock", equalTo(10));
  }

  @Test
  @Order(3)
  public void testGetProductByIdNotFound() {
    given()
        .when()
        .get(PATH + "/999")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(4)
  public void testCreateProduct() {
    String newProduct = """
        {
          "name": "NEW_PRODUCT",
          "description": "A new test product",
          "price": 99.99,
          "stock": 50
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(newProduct)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("name", equalTo("NEW_PRODUCT"))
        .body("description", equalTo("A new test product"))
        .body("stock", equalTo(50))
        .body("id", notNullValue());
  }

  @Test
  @Order(5)
  public void testCreateProductWithIdFails() {
    String productWithId = """
        {
          "id": 100,
          "name": "INVALID_PRODUCT",
          "stock": 10
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(productWithId)
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
  }

  @Test
  @Order(6)
  public void testUpdateProduct() {
    String updatedProduct = """
        {
          "name": "UPDATED_TONSTAD",
          "description": "Updated description",
          "price": 149.99,
          "stock": 25
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(updatedProduct)
        .when()
        .put(PATH + "/1")
        .then()
        .statusCode(200)
        .body("name", equalTo("UPDATED_TONSTAD"))
        .body("description", equalTo("Updated description"))
        .body("stock", equalTo(25));
  }

  @Test
  @Order(7)
  public void testUpdateProductNotFound() {
    String updatedProduct = """
        {
          "name": "NONEXISTENT",
          "stock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(updatedProduct)
        .when()
        .put(PATH + "/999")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(8)
  public void testUpdateProductWithoutNameFails() {
    String invalidProduct = """
        {
          "stock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(invalidProduct)
        .when()
        .put(PATH + "/1")
        .then()
        .statusCode(422);
  }

  @Test
  @Order(9)
  public void testDeleteProduct() {
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
  @Order(10)
  public void testDeleteProductNotFound() {
    given()
        .when()
        .delete(PATH + "/999")
        .then()
        .statusCode(404);
  }
}
