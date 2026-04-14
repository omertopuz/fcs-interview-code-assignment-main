# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**

### Key Questions to Clarify Scope

1. **Granularity of Cost Tracking**: At what level do we need to track costs? Per warehouse, per store, per product SKU, per order, or a combination? What's the reporting frequency (real-time, daily, weekly)?

2. **Cost Attribution Model**: How should shared costs (e.g., corporate overhead, shared transportation routes) be allocated? Do we use activity-based costing (ABC), direct allocation, or a hybrid approach?

3. **Data Sources & Ownership**: Which systems currently hold labor, inventory, and transportation data? Who owns this data and what are the data quality SLAs?

4. **Regulatory & Compliance Requirements**: Are there specific financial reporting standards (GAAP, IFRS) or audit requirements that constrain our design choices?

### Technical & Architectural Considerations

**Data Model Design**:
- Implement a dimensional model separating cost facts from dimension tables (time, location, cost type, product hierarchy)
- Design for immutability: costs should be append-only with effective dates to support auditing and historical analysis
- Consider a multi-tenant architecture if different business units have varying cost structures

**Integration Challenges**:
- Labor costs often come from HR/Payroll systems with different update cycles than operational systems
- Inventory valuation methods (FIFO, LIFO, weighted average) must be consistent across the platform
- Transportation costs may arrive asynchronously from 3PL partners—design for eventual consistency

**Previous Experience & Patterns**:
In my experience building cost allocation systems, the biggest challenges are:
- **Transfer pricing between entities**: When inventory moves from Warehouse to Store, how is the internal cost captured?
- **Currency and regional variations**: If operating across regions, FX rates and local regulations add complexity
- **Reconciliation with GL**: The Cost Control Tool must reconcile with the General Ledger to avoid dual-source-of-truth problems

**Recommended Approach**:
Start with a clear cost taxonomy, establish data contracts with upstream systems, and build reconciliation mechanisms from day one. Use event-driven architecture to capture cost events in real-time while maintaining batch reconciliation for financial close processes.

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**

### Key Questions to Clarify Scope

1. **Current Cost Baseline**: What is the current cost breakdown by category (labor, inventory, transportation, overhead)? What's the target reduction percentage or absolute amount?

2. **Service Level Constraints**: What are the non-negotiable SLAs (delivery time, order accuracy, stock availability)? Optimization without SLA context risks degrading customer experience.

3. **Investment Appetite**: Is there budget for capital expenditure (automation, new technology) or are we focused on operational efficiencies only?

4. **Timeline & Quick Wins**: Is this a long-term strategic initiative or are there immediate cost pressures requiring quick wins?

### Cost Optimization Strategy Framework

**Phase 1: Discovery & Analysis**
- Conduct a Pareto analysis: typically 20% of SKUs/routes/operations drive 80% of costs
- Map the value stream to identify waste (Lean methodology): overproduction, waiting, unnecessary transport, over-processing, excess inventory, unnecessary motion, defects
- Benchmark against industry standards and internal high-performers

**Phase 2: Prioritization Matrix**
Evaluate each opportunity on two axes:
- **Impact**: Cost reduction potential (high/medium/low)
- **Feasibility**: Implementation complexity, risk, and time-to-value

| Strategy | Impact | Feasibility | Priority |
|----------|--------|-------------|----------|
| Route optimization (TSP algorithms) | High | Medium | 1 |
| Demand forecasting improvement | High | High | 1 |
| Inventory positioning optimization | Medium | Medium | 2 |
| Labor scheduling optimization | Medium | High | 2 |
| Warehouse slotting optimization | Medium | Medium | 3 |
| Carrier contract renegotiation | High | Low | 3 |

**Phase 3: Implementation Strategies**

1. **Transportation**:
   - Implement dynamic routing algorithms (consider vehicle capacity, time windows, traffic patterns)
   - Consolidate shipments across warehouses and stores
   - Evaluate last-mile delivery partnerships vs. in-house operations

2. **Inventory**:
   - Deploy safety stock optimization using demand variability and lead time analysis
   - Implement ABC/XYZ classification for differentiated stocking policies
   - Consider postponement strategies to reduce inventory carrying costs

3. **Labor**:
   - Implement workforce management systems with demand-based scheduling
   - Cross-train employees to improve flexibility and reduce overtime
   - Measure and improve pick path efficiency

4. **Technology Enablement**:
   - Automation ROI analysis: where does automation payback exceed threshold?
   - Implement real-time visibility to reduce expediting costs

**Expected Outcomes & Measurement**:
- Define clear KPIs: cost per order, cost per unit, transportation cost per km
- Establish measurement cadence and ownership
- Build feedback loops to continuously refine strategies

**Previous Experience**: Cost optimization is iterative—start with low-hanging fruit to build momentum and fund larger initiatives. Always validate assumptions with pilots before full rollout.

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**

### Key Questions to Clarify Scope

1. **Current Financial Landscape**: What ERP/financial systems are in place (SAP, Oracle, NetSuite, custom)? What's their API maturity and integration readiness?

2. **Data Synchronization Requirements**: What latency is acceptable? Real-time (sub-second), near-real-time (minutes), or batch (hourly/daily)? Different use cases may have different requirements.

3. **Source of Truth Definition**: Which system will be authoritative for each data element? Cost Control Tool for operational costs, GL for financial reporting—but how do we reconcile?

4. **Change Data Capture**: How will we detect and propagate changes? Event-driven, polling, or database-level CDC?

5. **Security & Compliance**: What data classification applies? Are there restrictions on data movement (PCI, SOX compliance)?

### Business Benefits of Integration

1. **Single Source of Truth**: Eliminates manual reconciliation, reduces errors, and ensures consistent reporting across operational and financial views

2. **Real-Time Visibility**: Finance teams can monitor cost trends as they occur, enabling proactive intervention rather than reactive correction

3. **Accelerated Close Process**: Automated data flow reduces month-end close cycle time—a key CFO priority

4. **Improved Decision Making**: Operational managers get financial context; finance gets operational granularity

5. **Audit Trail & Compliance**: Automated integration provides complete traceability for auditors

### Integration Architecture Considerations

**Pattern Selection**:
- **Event-Driven Architecture (EDA)**: Publish cost events to a message broker (Kafka, RabbitMQ); financial systems subscribe. Best for real-time, loosely coupled integration.
- **API-Based Integration**: REST/GraphQL APIs for request-response patterns. Good for on-demand queries and synchronous operations.
- **Batch ETL**: Traditional extract-transform-load for high-volume, periodic synchronization. Still relevant for financial close processes.

**Recommended Hybrid Approach**:
```
┌─────────────────┐    Events     ┌──────────────┐    API/Batch    ┌─────────────┐
│ Cost Control    │──────────────▶│ Integration  │────────────────▶│ Financial   │
│ Tool            │               │ Layer (ESB)  │                 │ Systems     │
└─────────────────┘               └──────────────┘                 └─────────────┘
                                        │
                                        ▼
                                  ┌──────────────┐
                                  │ Data Quality │
                                  │ & Validation │
                                  └──────────────┘
```

**Data Quality & Validation**:
- Implement schema validation at integration boundaries
- Build reconciliation reports comparing Cost Control totals with GL balances
- Create alerting for data anomalies (unexpected variances, missing data)

**Seamless Integration Best Practices**:
1. **Idempotency**: Ensure all operations can be safely retried without duplication
2. **Dead Letter Queues**: Handle failed messages gracefully with retry logic and alerting
3. **Circuit Breakers**: Prevent cascade failures when downstream systems are unavailable
4. **Versioning**: Design APIs for backward compatibility; use semantic versioning
5. **Monitoring**: Implement end-to-end tracing (OpenTelemetry) and business-level monitors

**Previous Experience**: The biggest integration failures I've seen stem from treating integration as an afterthought. Design integration contracts early, automate testing, and establish clear ownership between teams.

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**

### Key Questions to Clarify Scope

1. **Planning Horizon & Granularity**: What timeframes are needed (annual budget, quarterly forecast, monthly rolling)? At what entity level (company, region, warehouse, store)?

2. **Forecasting Drivers**: What are the primary cost drivers? Volume-based (orders, units), fixed costs, or a combination? How predictable is demand?

3. **Budget Ownership & Workflow**: Who owns the budgeting process? Is it top-down (finance-driven) or bottom-up (operations-driven)? What approval workflows are required?

4. **Historical Data Quality**: How much reliable historical data exists? Forecasting accuracy depends heavily on data quality and depth.

5. **Scenario Planning Requirements**: Do stakeholders need what-if analysis capabilities (e.g., "what if volume increases 20%")?

### Importance of Budgeting & Forecasting in Fulfillment

1. **Resource Planning**: Fulfillment operations are labor and capital intensive. Accurate forecasts enable:
   - Workforce planning (hiring, scheduling, training)
   - Capacity planning (warehouse space, equipment)
   - Inventory investment decisions

2. **Cash Flow Management**: Understanding future costs enables treasury to manage working capital effectively

3. **Performance Management**: Budgets establish targets; variance analysis drives accountability and continuous improvement

4. **Strategic Decision Support**: Forecasts inform decisions like new warehouse openings, technology investments, and market expansion

### System Design Considerations

**Data Foundation**:
- **Historical Repository**: Store granular historical data (at least 2-3 years) for pattern analysis
- **Driver Metrics**: Capture key drivers (order volume, SKU mix, seasonality patterns, promotional calendars)
- **External Data**: Incorporate macroeconomic indicators, market trends, and competitive intelligence where relevant

**Forecasting Methodology**:

| Method | Use Case | Complexity |
|--------|----------|------------|
| Time Series (ARIMA, Exponential Smoothing) | Stable, repeating patterns | Low-Medium |
| Regression Analysis | When clear driver relationships exist | Medium |
| Machine Learning (XGBoost, LSTM) | Complex patterns, large datasets | High |
| Ensemble Methods | Combining multiple models for robustness | High |

**Recommended Approach**: Start with proven statistical methods; add ML complexity only when justified by accuracy improvements and data availability.

**System Architecture**:
```
┌─────────────┐    ┌─────────────────┐    ┌──────────────────┐
│ Historical  │───▶│ Forecasting     │───▶│ Budget Planning  │
│ Data Store  │    │ Engine          │    │ & Collaboration  │
└─────────────┘    └─────────────────┘    └──────────────────┘
                           │                       │
                           ▼                       ▼
                   ┌───────────────┐       ┌──────────────────┐
                   │ Model         │       │ Variance         │
                   │ Performance   │       │ Analysis &       │
                   │ Monitoring    │       │ Reporting        │
                   └───────────────┘       └──────────────────┘
```

**Key Features**:
1. **Driver-Based Planning**: Link costs to operational drivers (cost per order, cost per unit)
2. **Rolling Forecasts**: Move away from static annual budgets; implement continuous re-forecasting
3. **Scenario Modeling**: Enable users to adjust assumptions and see impact in real-time
4. **Variance Analysis**: Automated comparison of actuals vs. budget with drill-down capability
5. **Collaboration Workflows**: Support multi-user input, version control, and approval routing

**Accuracy & Governance**:
- Track forecast accuracy (MAPE, bias) and continuously improve models
- Establish forecast governance: who can adjust, when, and with what justification
- Implement audit trails for compliance and accountability

**Previous Experience**: The most successful budgeting systems I've built balance sophistication with usability. Finance teams need flexibility to apply judgment; overly rigid systems get circumvented with spreadsheets. Build trust through transparency—show how forecasts are generated.

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**

### Key Questions to Clarify Scope

1. **Transition Timeline**: What is the overlap period between old and new warehouse operations? Will there be parallel running, or a hard cutover?

2. **Cost Attribution During Transition**: During the transition, how do we attribute costs? The old warehouse may have wind-down costs while the new one has ramp-up costs.

3. **Historical Reporting Requirements**: How far back must we preserve cost history? Are there legal/regulatory retention requirements? What queries need to run against historical data?

4. **Business Unit Code Implications**: Since the BU Code is reused, how do downstream systems distinguish between the archived warehouse and the new one?

5. **Baseline for New Warehouse**: What cost baseline should the new warehouse be measured against—the old warehouse's costs, industry benchmarks, or a fresh target?

### Importance of Preserving Cost History

1. **Regulatory & Audit Compliance**: Financial records typically have 7+ year retention requirements. Cost history may be needed for audits, tax purposes, or legal matters.

2. **Trend Analysis & Benchmarking**: Understanding historical cost patterns enables:
   - Accurate benchmarking for the new warehouse
   - Identification of cost trends that should influence new warehouse design
   - ROI calculation: was the warehouse replacement investment justified?

3. **Contractual Obligations**: Historical costs may be referenced in supplier contracts, customer agreements, or lease negotiations.

4. **Knowledge Transfer**: Historical patterns (seasonal variations, efficiency improvements over time) inform operational planning for the new facility.

### Cost Control Aspects of Warehouse Replacement

**Pre-Replacement Phase**:
- **Budget the Transition**: Create a specific budget for replacement costs (moving inventory, parallel operations, decommissioning)
- **Baseline Documentation**: Capture final cost baseline of old warehouse for post-replacement comparison
- **Asset Disposition**: Plan for depreciation, sale, or write-off of old warehouse assets

**During Replacement**:
- **Dual Tracking**: Maintain separate cost centers for old and new warehouse during transition
- **Variance Monitoring**: Track actual vs. planned transition costs closely
- **Efficiency Curve Planning**: New warehouses typically have learning curve inefficiencies; budget for temporary higher costs

**Post-Replacement**:
- **Cost Comparison**: Compare new warehouse operations against:
  - Old warehouse baseline (apples-to-apples comparison)
  - Original business case projections
  - Industry benchmarks
- **Ramp-Up Tracking**: Monitor the efficiency curve—how quickly are we reaching target cost levels?

### Technical Implementation Considerations

**Data Model for BU Code Reuse**:
```
Business Unit Code: WH-001
├── Warehouse Instance 1 (Archived)
│   ├── ID: warehouse_uuid_123
│   ├── Status: ARCHIVED
│   ├── Effective: 2020-01-01 to 2026-03-31
│   └── Cost Records: [preserved, immutable]
│
└── Warehouse Instance 2 (Active)
    ├── ID: warehouse_uuid_456
    ├── Status: ACTIVE
    ├── Effective: 2026-04-01 to present
    └── Cost Records: [current operations]
```

**Key Design Principles**:
1. **Temporal Modeling**: Implement effective dates on all entities; never delete, always archive
2. **Immutability**: Historical cost records should be immutable; adjustments create new records, not updates
3. **Query Flexibility**: APIs and reports should support querying by BU Code (aggregated view) or by specific warehouse instance (historical accuracy)
4. **Clear State Transitions**: Implement explicit state machine (ACTIVE → REPLACING → ARCHIVED) with audit logging

**Budget Control for New Warehouse**:
1. **Establish Clear Targets**: Define expected cost structure for new warehouse based on:
   - Old warehouse actuals (adjusted for known improvements)
   - Business case assumptions
   - Technology/automation ROI projections

2. **Monitoring Framework**:
   - Set up dashboards comparing actual costs to budget
   - Create alerts for significant variances
   - Implement rolling forecasts to predict end-state costs

3. **Accountability**: Assign clear ownership for achieving cost targets; include in operational reviews

**Previous Experience**: Warehouse replacements are high-risk, high-visibility projects. The biggest cost control failures occur when:
- Transition costs are underestimated (always add contingency)
- Ramp-up inefficiencies aren't anticipated
- Historical data is lost or becomes inaccessible
- Success metrics aren't defined upfront

**Recommendation**: Treat the warehouse replacement as a formal program with cost governance structure. Establish a steering committee that reviews budget vs. actuals weekly during transition. Ensure historical data is migrated to a stable, queryable archive before decommissioning old systems.

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.
