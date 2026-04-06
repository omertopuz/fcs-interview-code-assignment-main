# Code Assignment Implementation Instructions

This document provides comprehensive instructions for implementing and completing all tasks in the Java code assignment.

---

## Table of Contents

1. [Running the Project Locally](#1-running-the-project-locally)
2. [Task 1: Location Gateway](#2-task-1-location-gateway)
3. [Task 2: Store Resource - Transaction Fix](#3-task-2-store-resource---transaction-fix)
4. [Task 3: Warehouse Operations](#4-task-3-warehouse-operations)
5. [Bonus Task: Product-Warehouse-Store Association](#5-bonus-task-product-warehouse-store-association)
6. [Unit Testing Guidelines](#6-unit-testing-guidelines)
7. [JaCoCo Test Coverage](#7-jacoco-test-coverage)
8. [Integration Tests](#8-integration-tests)
9. [Postman Collection](#9-postman-collection)
10. [Cucumber Functional Tests](#10-cucumber-functional-tests)
11. [Answers to Questions](#11-answers-to-questions)

---

## 1. Running the Project Locally

### Prerequisites

- **JDK 17+** installed and `JAVA_HOME` environment variable set
- **Docker** (for PostgreSQL database in production mode)
- **Maven** (or use the included `./mvnw` wrapper)

### Development Mode (Recommended)

Quarkus Dev Services will automatically start a PostgreSQL container:

```bash
cd java-assignment
./mvnw quarkus:dev
```

The application will be available at: http://localhost:8080

### Production Mode

1. **Start PostgreSQL with Docker:**
```bash
docker run -it --rm=true --name quarkus_test \
  -e POSTGRES_USER=quarkus_test \
  -e POSTGRES_PASSWORD=quarkus_test \
  -e POSTGRES_DB=quarkus_test \
  -p 15432:5432 postgres:13.3
```

2. **Build and run the application:**
```bash
./mvnw package
java -jar ./target/quarkus-app/quarkus-run.jar
```

### Verify Build

```bash
./mvnw clean package
```

All tests should pass and the build should complete successfully.

---

## 2. Task 1: Location Gateway

**File:** `src/main/java/com/fulfilment/application/monolith/location/LocationGateway.java`

### Implementation

Implement `resolveByIdentifier` method to find a location by its identifier:

```java
@Override
public Location resolveByIdentifier(String identifier) {
    return locations.stream()
        .filter(location -> location.identification.equals(identifier))
        .findFirst()
        .orElse(null);
}
```

### Valid Location Identifiers

From the static locations list:
- `ZWOLLE-001` (max 1 warehouse, capacity 40)
- `ZWOLLE-002` (max 2 warehouses, capacity 50)
- `AMSTERDAM-001` (max 5 warehouses, capacity 100)
- `AMSTERDAM-002` (max 3 warehouses, capacity 75)
- `TILBURG-001` (max 1 warehouse, capacity 40)
- `HELMOND-001` (max 1 warehouse, capacity 45)
- `EINDHOVEN-001` (max 2 warehouses, capacity 70)
- `VETSBY-001` (max 1 warehouse, capacity 90)

---

## 3. Task 2: Store Resource - Transaction Fix

**File:** `src/main/java/com/fulfilment/application/monolith/stores/StoreResource.java`

### Problem

The `LegacyStoreManagerGateway` is called within the same transaction as the database operation. If the transaction rolls back, the legacy system would have outdated data.

### Solution

Use `TransactionSynchronizationRegistry` to ensure gateway calls happen **after** the transaction commits:

```java
@Inject
TransactionSynchronizationRegistry txRegistry;

@POST
@Transactional
public Response create(Store store) {
    if (store.id != null) {
        throw new WebApplicationException("Id was invalidly set on request.", 422);
    }

    store.persist();

    // Call legacy system only after transaction commits
    txRegistry.registerInterposedSynchronization(new Synchronization() {
        @Override
        public void beforeCompletion() {}

        @Override
        public void afterCompletion(int status) {
            if (status == Status.STATUS_COMMITTED) {
                legacyStoreManagerGateway.createStoreOnLegacySystem(store);
            }
        }
    });

    return Response.ok(store).status(201).build();
}
```

Apply the same pattern to `update()` and `patch()` methods.

**Alternative:** Use CDI Events with `@Observes(during = TransactionPhase.AFTER_SUCCESS)`.

---

## 4. Task 3: Warehouse Operations

### Implementation Files

- **Resource:** `src/main/java/com/fulfilment/application/monolith/warehouses/adapters/restapi/WarehouseResourceImpl.java`
- **Use Cases:**
    - `CreateWarehouseUseCase.java`
    - `ReplaceWarehouseUseCase.java`
    - `ArchiveWarehouseUseCase.java`

### Business Rules to Implement

#### Create Warehouse Validations

1. **Business Unit Code Verification:** Ensure the `businessUnitCode` doesn't already exist (for active warehouses)
2. **Location Validation:** Location must exist in `LocationGateway`
3. **Warehouse Creation Feasibility:** Check if location hasn't reached `maxNumberOfWarehouses`
4. **Capacity Validation:** Warehouse capacity must not exceed location's `maxCapacity`
5. **Stock Validation:** Stock must not exceed warehouse capacity

#### Replace Warehouse Validations

1. All create validations apply
2. **Capacity Accommodation:** New warehouse capacity must accommodate existing stock
3. **Stock Matching:** New warehouse stock must match the previous warehouse stock

#### Archive Warehouse

1. Warehouse must exist
2. Set `archivedAt` timestamp (soft delete)

### Example Implementation for CreateWarehouseUseCase

```java
@Override
public void create(Warehouse warehouse) {
    // 1. Validate business unit code doesn't exist
    var existing = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existing != null && existing.archivedAt == null) {
        throw new IllegalArgumentException("Business unit code already exists");
    }

    // 2. Validate location exists
    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
        throw new IllegalArgumentException("Invalid location: " + warehouse.location);
    }

    // 3. Check warehouse count at location
    long warehouseCount = warehouseStore.countActiveByLocation(warehouse.location);
    if (warehouseCount >= location.maxNumberOfWarehouses) {
        throw new IllegalArgumentException("Maximum warehouses reached for location");
    }

    // 4. Validate capacity
    if (warehouse.capacity > location.maxCapacity) {
        throw new IllegalArgumentException("Capacity exceeds location maximum");
    }

    // 5. Validate stock doesn't exceed capacity
    if (warehouse.stock > warehouse.capacity) {
        throw new IllegalArgumentException("Stock exceeds warehouse capacity");
    }

    warehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(warehouse);
}
```

### HTTP Response Codes

| Operation | Success | Not Found | Invalid Request |
|-----------|---------|-----------|-----------------|
| GET /warehouse | 200 | - | - |
| GET /warehouse/{id} | 200 | 404 | - |
| POST /warehouse | 201 | - | 400 |
| DELETE /warehouse/{id} | 204 | 404 | - |
| POST /warehouse/{code}/replacement | 200 | 404 | 400 |

---

## 5. Bonus Task: Product-Warehouse-Store Association

### Constraints

1. Each `Product` can be fulfilled by max **2 different Warehouses per Store**
2. Each `Store` can be fulfilled by max **3 different Warehouses**
3. Each `Warehouse` can store max **5 types of Products**

### Suggested Implementation

1. Create a new entity `WarehouseFulfilment`:

```java
@Entity
public class WarehouseFulfilment extends PanacheEntity {
    @ManyToOne
    public Product product;
    
    @ManyToOne
    public Store store;
    
    @ManyToOne
    public DbWarehouse warehouse;
}
```

2. Add validation methods to check constraints before creating associations
3. Create REST endpoints for managing associations

---

## 6. Unit Testing Guidelines

### Principles

- **Realistic scenarios only:** Base tests on actual business requirements
- **Follow AAA pattern:** Arrange, Act, Assert
- **Use descriptive test names:** `shouldReturnNullWhenLocationNotFound`
- **Mock external dependencies:** Use `@InjectMock` for Quarkus tests

### Test Cases to Implement

#### LocationGatewayTest

```java
@Test
void shouldReturnLocationWhenIdentifierExists() {
    LocationGateway gateway = new LocationGateway();
    Location result = gateway.resolveByIdentifier("AMSTERDAM-001");
    
    assertNotNull(result);
    assertEquals("AMSTERDAM-001", result.identification);
    assertEquals(5, result.maxNumberOfWarehouses);
    assertEquals(100, result.maxCapacity);
}

@Test
void shouldReturnNullWhenIdentifierNotFound() {
    LocationGateway gateway = new LocationGateway();
    Location result = gateway.resolveByIdentifier("INVALID-001");
    
    assertNull(result);
}
```

#### CreateWarehouseUseCaseTest

```java
@Test
void shouldCreateWarehouseWithValidData() { /* ... */ }

@Test
void shouldRejectDuplicateBusinessUnitCode() { /* ... */ }

@Test
void shouldRejectInvalidLocation() { /* ... */ }

@Test
void shouldRejectWhenLocationAtMaxWarehouses() { /* ... */ }

@Test
void shouldRejectCapacityExceedingLocationMax() { /* ... */ }

@Test
void shouldRejectStockExceedingCapacity() { /* ... */ }
```

#### ReplaceWarehouseUseCaseTest

```java
@Test
void shouldReplaceWarehouseSuccessfully() { /* ... */ }

@Test
void shouldRejectWhenNewCapacityCannotAccommodateStock() { /* ... */ }

@Test
void shouldRejectWhenStockDoesNotMatch() { /* ... */ }

@Test
void shouldRejectWhenWarehouseToReplaceNotFound() { /* ... */ }
```

#### ArchiveWarehouseUseCaseTest

```java
@Test
void shouldArchiveExistingWarehouse() { /* ... */ }

@Test
void shouldRejectArchivingNonExistentWarehouse() { /* ... */ }
```

### Running Unit Tests

```bash
./mvnw test
```

---

## 7. JaCoCo Test Coverage

### Add JaCoCo Plugin to pom.xml

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Generate Coverage Report

```bash
./mvnw test jacoco:report
```

Report available at: `target/site/jacoco/index.html`

### Ensure 80% Coverage

Focus testing efforts on:
- All use case classes (business logic)
- Location gateway
- Repository implementations
- Resource/controller classes

---

## 8. Integration Tests

### Configuration

Add Failsafe plugin for integration tests:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>3.1.2</version>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <systemPropertyVariables>
            <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

### Integration Tests to Complete

**File:** `WarehouseEndpointIT.java`

```java
@QuarkusIntegrationTest
public class WarehouseEndpointIT {

    private static final String BASE_PATH = "warehouse";

    @Test
    void shouldListAllWarehouses() {
        given()
            .when().get(BASE_PATH)
            .then()
                .statusCode(200)
                .body(containsString("MWH.001"))
                .body(containsString("MWH.012"))
                .body(containsString("MWH.023"));
    }

    @Test
    void shouldGetWarehouseById() {
        given()
            .when().get(BASE_PATH + "/1")
            .then()
                .statusCode(200)
                .body("businessUnitCode", equalTo("MWH.001"));
    }

    @Test
    void shouldReturn404ForNonExistentWarehouse() {
        given()
            .when().get(BASE_PATH + "/9999")
            .then()
                .statusCode(404);
    }

    @Test
    void shouldCreateWarehouseWithValidData() {
        String json = """
            {
                "businessUnitCode": "MWH.NEW",
                "location": "AMSTERDAM-001",
                "capacity": 50,
                "stock": 10
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(json)
            .when().post(BASE_PATH)
            .then()
                .statusCode(201)
                .body("businessUnitCode", equalTo("MWH.NEW"));
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
            .when().post(BASE_PATH)
            .then()
                .statusCode(400);
    }

    @Test
    void shouldArchiveWarehouse() {
        given()
            .when().delete(BASE_PATH + "/3")
            .then()
                .statusCode(204);
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
            .when().post(BASE_PATH + "/MWH.001/replacement")
            .then()
                .statusCode(200);
    }
}
```

### Running Integration Tests

```bash
./mvnw verify
```

---

## 9. Postman Collection

Create a file `postman/warehouse-api-collection.json`:

```json
{
  "info": {
    "name": "Warehouse Fulfilment API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080",
      "type": "string"
    }
  ],
  "item": [
    {
      "name": "Warehouse",
      "item": [
        {
          "name": "List All Warehouses",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/warehouse"
          }
        },
        {
          "name": "Get Warehouse by ID",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/warehouse/1"
          }
        },
        {
          "name": "Create Warehouse",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/warehouse",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"businessUnitCode\": \"MWH.NEW\",\n  \"location\": \"AMSTERDAM-001\",\n  \"capacity\": 50,\n  \"stock\": 10\n}"
            }
          }
        },
        {
          "name": "Archive Warehouse",
          "request": {
            "method": "DELETE",
            "url": "{{baseUrl}}/warehouse/1"
          }
        },
        {
          "name": "Replace Warehouse",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/warehouse/MWH.001/replacement",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"businessUnitCode\": \"MWH.001\",\n  \"location\": \"ZWOLLE-001\",\n  \"capacity\": 100,\n  \"stock\": 10\n}"
            }
          }
        }
      ]
    },
    {
      "name": "Store",
      "item": [
        {
          "name": "List All Stores",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/store"
          }
        },
        {
          "name": "Get Store by ID",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/store/1"
          }
        },
        {
          "name": "Create Store",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/store",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"name\": \"NEW STORE\",\n  \"quantityProductsInStock\": 100\n}"
            }
          }
        },
        {
          "name": "Update Store",
          "request": {
            "method": "PUT",
            "url": "{{baseUrl}}/store/1",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"name\": \"UPDATED STORE\",\n  \"quantityProductsInStock\": 150\n}"
            }
          }
        },
        {
          "name": "Delete Store",
          "request": {
            "method": "DELETE",
            "url": "{{baseUrl}}/store/1"
          }
        }
      ]
    },
    {
      "name": "Product",
      "item": [
        {
          "name": "List All Products",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/product"
          }
        },
        {
          "name": "Get Product by ID",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/product/1"
          }
        },
        {
          "name": "Create Product",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/product",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"name\": \"NEW PRODUCT\",\n  \"description\": \"Product description\",\n  \"price\": 29.99,\n  \"stock\": 100\n}"
            }
          }
        },
        {
          "name": "Update Product",
          "request": {
            "method": "PUT",
            "url": "{{baseUrl}}/product/1",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"name\": \"UPDATED PRODUCT\",\n  \"description\": \"Updated description\",\n  \"price\": 39.99,\n  \"stock\": 50\n}"
            }
          }
        },
        {
          "name": "Delete Product",
          "request": {
            "method": "DELETE",
            "url": "{{baseUrl}}/product/1"
          }
        }
      ]
    }
  ]
}
```

**Import:** Open Postman → Import → Upload the JSON file

---

## 10. Cucumber Functional Tests

### Project Structure

```
functional-test/
├── pom.xml
├── src/
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── fulfilment/
│       │           └── functional/
│       │               ├── CucumberRunner.java
│       │               └── steps/
│       │                   ├── WarehouseSteps.java
│       │                   ├── StoreSteps.java
│       │                   └── ProductSteps.java
│       └── resources/
│           └── features/
│               └── fulfilment-api.feature
```

### pom.xml for functional-test

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.fulfilment</groupId>
    <artifactId>functional-test</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <cucumber.version>7.15.0</cucumber.version>
        <rest-assured.version>5.4.0</rest-assured.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.cucumber</groupId>
            <artifactId>cucumber-java</artifactId>
            <version>${cucumber.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.cucumber</groupId>
            <artifactId>cucumber-junit-platform-engine</artifactId>
            <version>${cucumber.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.platform</groupId>
            <artifactId>junit-platform-suite</artifactId>
            <version>1.10.1</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.rest-assured</groupId>
            <artifactId>rest-assured</artifactId>
            <version>${rest-assured.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <version>3.25.1</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.1.2</version>
            </plugin>
            <plugin>
                <groupId>net.masterthought</groupId>
                <artifactId>maven-cucumber-reporting</artifactId>
                <version>5.7.8</version>
                <executions>
                    <execution>
                        <id>execution</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                        <configuration>
                            <projectName>Fulfilment API Functional Tests</projectName>
                            <outputDirectory>${project.build.directory}/cucumber-reports</outputDirectory>
                            <jsonFiles>
                                <param>**/*.json</param>
                            </jsonFiles>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### Feature File: fulfilment-api.feature

```gherkin
Feature: Fulfilment API Functional Tests
  As an API consumer
  I want to manage warehouses, stores, and products
  So that I can fulfill customer orders

  # ================== WAREHOUSE SCENARIOS ==================

  @warehouse @list
  Scenario: List all warehouses
    Given the API is running
    When I request a list of all warehouses
    Then the response status should be 200
    And the response should contain warehouses

  @warehouse @get
  Scenario: Get warehouse by ID
    Given the API is running
    And a warehouse with ID "1" exists
    When I request warehouse with ID "1"
    Then the response status should be 200
    And the warehouse business unit code should be "MWH.001"

  @warehouse @get @notfound
  Scenario: Get non-existent warehouse returns 404
    Given the API is running
    When I request warehouse with ID "9999"
    Then the response status should be 404

  @warehouse @create
  Scenario: Create a new warehouse with valid data
    Given the API is running
    When I create a warehouse with:
      | businessUnitCode | MWH.TEST     |
      | location         | AMSTERDAM-001 |
      | capacity         | 50            |
      | stock            | 10            |
    Then the response status should be 201
    And the created warehouse should have business unit code "MWH.TEST"

  @warehouse @create @validation
  Scenario: Create warehouse with invalid location is rejected
    Given the API is running
    When I create a warehouse with:
      | businessUnitCode | MWH.INVALID  |
      | location         | INVALID-LOC  |
      | capacity         | 50           |
      | stock            | 10           |
    Then the response status should be 400

  @warehouse @create @validation
  Scenario: Create warehouse with capacity exceeding location max is rejected
    Given the API is running
    When I create a warehouse with:
      | businessUnitCode | MWH.OVERCAP  |
      | location         | TILBURG-001  |
      | capacity         | 999          |
      | stock            | 10           |
    Then the response status should be 400

  @warehouse @create @validation
  Scenario: Create warehouse with stock exceeding capacity is rejected
    Given the API is running
    When I create a warehouse with:
      | businessUnitCode | MWH.OVERSTOCK |
      | location         | AMSTERDAM-001 |
      | capacity         | 50            |
      | stock            | 100           |
    Then the response status should be 400

  @warehouse @archive
  Scenario: Archive an existing warehouse
    Given the API is running
    And a warehouse with ID "3" exists
    When I archive warehouse with ID "3"
    Then the response status should be 204

  @warehouse @archive @notfound
  Scenario: Archive non-existent warehouse returns 404
    Given the API is running
    When I archive warehouse with ID "9999"
    Then the response status should be 404

  @warehouse @replace
  Scenario: Replace an existing warehouse
    Given the API is running
    And a warehouse with business unit code "MWH.001" exists
    When I replace warehouse "MWH.001" with:
      | businessUnitCode | MWH.001      |
      | location         | ZWOLLE-001   |
      | capacity         | 100          |
      | stock            | 10           |
    Then the response status should be 200

  @warehouse @replace @validation
  Scenario: Replace warehouse with mismatched stock is rejected
    Given the API is running
    And a warehouse with business unit code "MWH.012" exists with stock 5
    When I replace warehouse "MWH.012" with:
      | businessUnitCode | MWH.012       |
      | location         | AMSTERDAM-001 |
      | capacity         | 50            |
      | stock            | 999           |
    Then the response status should be 400

  # ================== STORE SCENARIOS ==================

  @store @list
  Scenario: List all stores
    Given the API is running
    When I request a list of all stores
    Then the response status should be 200
    And the response should contain stores

  @store @get
  Scenario: Get store by ID
    Given the API is running
    When I request store with ID "1"
    Then the response status should be 200
    And the store name should be "TONSTAD"

  @store @create
  Scenario: Create a new store
    Given the API is running
    When I create a store with name "NEW STORE" and stock 50
    Then the response status should be 201
    And the created store should have name "NEW STORE"

  @store @update
  Scenario: Update an existing store
    Given the API is running
    And a store with ID "2" exists
    When I update store "2" with name "UPDATED STORE" and stock 75
    Then the response status should be 200
    And the store name should be "UPDATED STORE"

  @store @delete
  Scenario: Delete a store
    Given the API is running
    And a store with ID "3" exists
    When I delete store with ID "3"
    Then the response status should be 204

  # ================== PRODUCT SCENARIOS ==================

  @product @list
  Scenario: List all products
    Given the API is running
    When I request a list of all products
    Then the response status should be 200
    And the response should contain products

  @product @get
  Scenario: Get product by ID
    Given the API is running
    When I request product with ID "1"
    Then the response status should be 200

  @product @create
  Scenario: Create a new product
    Given the API is running
    When I create a product with name "NEW PRODUCT" and stock 100
    Then the response status should be 201

  @product @delete
  Scenario: Delete a product
    Given the API is running
    When I delete product with ID "3"
    Then the response status should be 204
```

### CucumberRunner.java

```java
package com.fulfilment.functional;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.*;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.fulfilment.functional.steps")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, json:target/cucumber-reports/cucumber.json, html:target/cucumber-reports/cucumber.html")
public class CucumberRunner {
}
```

### Running Cucumber Tests

```bash
cd functional-test
mvn test
```

**Prerequisites:** Ensure the Java application is running on `http://localhost:8080` before executing functional tests.

### Cucumber Report

After running tests, the HTML report is available at:
`functional-test/target/cucumber-reports/cucumber.html`

---

## 11. Answers to Questions

Update `java-assignment/QUESTIONS.md` with:

### Question 1: Database Access Layer Refactoring

**Answer:**
```txt
Yes, I would refactor to use a consistent approach. The codebase mixes Active Record 
(Panache entities with static methods like Store.findById()) and Repository pattern 
(ProductRepository, WarehouseRepository). This inconsistency makes the code harder to 
maintain and test. 

WHY: The Repository pattern provides better separation of concerns, making business 
logic testable without database dependencies. Active Record couples entities to 
persistence, making unit testing difficult without a database. A unified Repository 
approach enables easier mocking, cleaner domain models, and consistent team practices.
```

### Question 2: OpenAPI vs Direct Coding

**Answer:**
```txt
OpenAPI-first (Code Generation):
Pros: Contract-first design, auto-generated documentation, type safety, client SDK 
generation, API consistency.
Cons: Learning curve, less flexibility, generated code can be harder to customize.

Code-first (Direct Implementation):
Pros: Faster initial development, full control, simpler for small APIs.
Cons: Documentation drift, manual validation, no automatic client generation.

Choice: For larger/public APIs, OpenAPI-first is preferred for contract consistency 
and documentation. For internal/simple APIs, code-first may be sufficient. In this 
codebase, extending OpenAPI approach to all endpoints would improve consistency.
```

### Question 3: Testing Strategy

**Answer:**
```txt
Priority order:
1. Unit tests for business logic (Use Cases) - fastest feedback, highest ROI
2. Integration tests for API endpoints - validate HTTP contracts
3. Repository tests with test containers - ensure data access correctness

Strategy:
- Focus on critical paths: warehouse create/replace/archive validations
- Use property-based testing for edge cases
- Maintain coverage with CI/CD quality gates
- Review coverage reports monthly to identify gaps
- Prefer meaningful tests over coverage percentage
```

---

## Quick Reference Commands

| Task | Command |
|------|---------|
| Build | `./mvnw clean package` |
| Run Dev Mode | `./mvnw quarkus:dev` |
| Run Unit Tests | `./mvnw test` |
| Run Integration Tests | `./mvnw verify` |
| Generate Coverage Report | `./mvnw test jacoco:report` |
| Run Functional Tests | `cd functional-test && mvn test` |

---

## Checklist

- [ ] Project builds successfully (`./mvnw clean package`)
- [ ] All unit tests pass (`./mvnw test`)
- [ ] JaCoCo coverage ≥ 80%
- [ ] All integration tests pass (`./mvnw verify`)
- [ ] Functional tests pass (Cucumber)
- [ ] Postman collection validated
- [ ] QUESTIONS.md answered
- [ ] Task 1: LocationGateway implemented
- [ ] Task 2: StoreResource transaction fix applied
- [ ] Task 3: Warehouse operations implemented
- [ ] Bonus: Product-Warehouse-Store association (optional)