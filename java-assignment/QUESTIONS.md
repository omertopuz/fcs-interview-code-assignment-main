# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

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
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
OpenAPI-first (Code Generation):
Pros: Contract-first design, auto-generated documentation, type safety, client SDK 
generation, API consistency, single source of truth.
Cons: Learning curve, less flexibility, generated code can be harder to customize, 
tool dependencies.

Code-first (Direct Implementation):
Pros: Faster initial development, full control, simpler for small APIs, no tool overhead.
Cons: Documentation drift, manual validation, no automatic client generation, consistency 
gaps across endpoints.

Choice: For larger/public APIs, OpenAPI-first is preferred for contract consistency 
and documentation. For internal/simple APIs, code-first may be sufficient. In this 
codebase, extending OpenAPI approach to all endpoints (Product, Store) would improve 
consistency and enable better API documentation and client generation. Code generation 
ensures contracts are always up-to-date and reduces manual errors.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Priority order:
1. Unit tests for business logic (Use Cases) - fastest feedback, highest ROI
   - CreateWarehouseUseCase, ReplaceWarehouseUseCase, ArchiveWarehouseUseCase
   - LocationGateway for location resolution
   - Focus on validations and edge cases
   
2. Integration tests for API endpoints - validate HTTP contracts and status codes
   - Test all CRUD operations with valid/invalid data
   - Verify error responses (400, 404)
   - Ensure database persistence
   
3. Repository tests with test containers - ensure data access correctness
   - Test query methods with real database behavior
   - Validate transaction handling

Strategy:
- Use JaCoCo to enforce 80% coverage threshold on critical classes (use cases, gateways)
- Exclude models and generated code from coverage requirements
- Run unit tests in CI/CD pipeline on every commit (fast, < 5 seconds)
- Run integration tests on pull requests before merge
- Use meaningful test names following pattern: shouldDoXWhenYHappens
- Mock external dependencies to keep tests isolated and fast
- Prefer component/slice tests over end-to-end tests for faster feedback
- Review coverage reports monthly, focusing on critical paths first
- Balance breadth (cover all scenarios) with depth (test edge cases)
- Use property-based testing for validation logic when applicable
```