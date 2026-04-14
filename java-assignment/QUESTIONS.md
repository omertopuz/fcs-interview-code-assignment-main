# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes, I would refactor the database access patterns to achieve consistency and better architecture across the codebase. The current mixed implementation is problematic because it creates cognitive overhead and maintenance burden:

**Why inconsistency hurts:**
- Developers must context-switch between 3 different patterns when working across modules
- New team members can't learn "how we do things here" - there's no single approach
- Code reviews become harder as there's no clear standard to enforce

**Why Active Record (Store) is particularly bad:**
- Static methods like `Store.findById()` cannot be mocked in unit tests
- The entity knows how to save itself, violating single responsibility
- You cannot test business logic without a real database

**Why the shallow Repository (Product) is insufficient:**
- The empty `ProductRepository implements PanacheRepository<Product>` adds indirection without value
- Product is still a JPA entity, so domain logic remains coupled to persistence framework
- You're paying the abstraction cost without the abstraction benefit

**The core issue:** When you eventually need to add business rules to Store or Product (validation, events, complex workflows), you have no clean place to put them. The Warehouse module already has this - use cases that depend only on the `WarehouseStore` interface, fully testable with mocks.

The refactoring effort is an investment in future maintainability. Without it, technical debt compounds as developers add features in inconsistent ways.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
OpenAPI Code Generation (Warehouse approach):

Pros:
- Contract-first design: API specification serves as documentation and source of truth
- Consistency: Generated interfaces ensure consistent parameter handling, annotations, and response types
- Client SDK generation: The same OpenAPI spec can generate client SDKs in multiple languages
- Validation: Schema validation is built into the specification
- Team collaboration: Frontend/backend teams can work in parallel using the spec as contract
- Documentation: OpenAPI spec can be used with Swagger UI for interactive API docs

Cons:
- Build complexity: Requires code generation step in the build process
- Learning curve: Team needs to understand OpenAPI syntax and generation tooling
- Indirection: Implementation class (WarehouseResourceImpl) must implement generated interface
- Regeneration overhead: Changes require updating YAML, regenerating, and updating implementation
- Type mapping: May need custom mapping between generated beans and domain models

Code-first approach (Product/Store):

Pros:
- Simplicity: Direct coding is straightforward and quick to implement
- Flexibility: Easy to make rapid changes without regeneration steps
- Less tooling: No additional build plugins or generation configuration needed
- Debugging: Direct stack traces without generated code indirection

Cons:
- Documentation drift: API docs may become outdated if not maintained separately
- Inconsistency: Different developers may structure endpoints differently
- No contract: Harder to share API spec with consumers before implementation
- Manual validation: Must manually implement validation logic

My choice: OpenAPI code generation (contract-first)

For a production application with multiple consumers, I would standardize on OpenAPI for all endpoints because:
1. It enforces a contract that can be shared with API consumers before implementation
2. Ensures consistency across all APIs
3. Provides self-documenting endpoints
4. Facilitates better testing with schema validation
5. Supports parallel development between teams

For internal/small projects where speed matters more than formality, code-first can be acceptable with annotations generating OpenAPI documentation.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Test Prioritization (in order of importance):

1. Unit Tests for Domain Logic (Highest Priority)
Focus on use cases like CreateWarehouseUseCase, ReplaceWarehouseUseCase, ArchiveWarehouseUseCase.
- These tests are fast, isolated, and test critical business rules
- Use mocks for repositories and external dependencies (as currently done in the codebase)
- Cover all validation scenarios, edge cases, and business rules
- Should represent ~70% of test effort

2. Integration/API Tests (High Priority)
Tests like ProductEndpointTest that verify HTTP endpoints work correctly with the database.
- Use @QuarkusTest for realistic end-to-end testing of the REST layer
- Cover CRUD operations, error responses, and content negotiation
- Test database transactions and rollback behavior
- Should represent ~20% of test effort

3. Functional/E2E Tests (Medium Priority)
Cucumber tests in functional-test module for business scenario validation.
- Cover critical user journeys (create warehouse, replace, archive flows)
- Run against a deployed instance
- Useful for acceptance criteria validation and regression testing
- Should represent ~10% of test effort (most expensive to maintain)

Strategies for Effective Coverage Over Time:

1. Diff Coverage (Focus on Changed Code)
- Use diff-cover or SonarQube to measure coverage ONLY on new/modified lines
- Enforce 90% coverage on changed code, even if legacy overall coverage is lower
- Prevents untested code from sneaking into PRs - fail builds when new code lacks tests

2. Static Code Analysis & Quality Gates
- SonarQube quality gates: "Coverage on new code must be > 80%" - blocks merge if violated
- Tracks coverage trends over time, identifies gaps by module
- SpotBugs/PMD detect code patterns that should have tests (unchecked exceptions, error handling)

3. Mutation Testing
- PIT verifies tests actually assert behavior, not just execute code
- If changing `>` to `>=` doesn't fail any test, you're missing boundary tests
- Run on critical modules to validate test quality beyond line coverage

4. CI/CD Enforcement
- Block merges when coverage drops or new code lacks tests
- Upload reports to SonarQube/Codecov for historical tracking and trend alerts
```