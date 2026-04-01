# Content Marketplace - Project Context

## Overview

This project is a **Financial Content Marketplace** that implements a dual-storage architecture for managing financial data from multiple sources (SEC EDGAR, Bloomberg, brokers, etc.) with the goal of creating a unified, general financial data model.

---

## Core Architecture: DIR + Elasticsearch

### Data Item Registry (DIR) - PostgreSQL

The **Data Item Registry (DIR)** is the metadata layer stored in PostgreSQL that describes and catalogs all data sources. It serves as the "schema registry" for the marketplace.

**Key Entity: `DataItem`** (`data_items` table)
- **Purpose**: Registry entry describing a dataset's structure, origin, and schema
- **Fields**:
  - `id` (UUID) - Primary key
  - `name` - Dataset name (e.g., "SEC 2025Q4 - sub.txt")
  - `sourceType` - Data format (XML, JSON, CSV, EXCEL, PDF, TEXT, OTHER)
  - `provider` - Data source (e.g., "SEC", "Bloomberg")
  - `metadata` (JSONB) - Flexible schema storage including field definitions

### Actual Data - Elasticsearch

The actual data records are stored in **Elasticsearch** for full-text search and flexible querying.

**Key Document: `ContentDocument`** (`content_documents` index)
- `dataItemId` - **Link back to DIR** (PostgreSQL DataItem UUID)
- `rawContent` - Original unparsed content
- `extractedFields` - Parsed field values as Map

---

## GraphQL API

### Endpoints

| Endpoint | Purpose |
|----------|---------|
| `/graphql` | GraphQL API endpoint |
| `/graphiql` | GraphQL interactive playground |

### Schema Overview

**Queries:**
```graphql
# Company queries
company(id: UUID!): Company
companyByTicker(ticker: String!): Company
companyByCik(cik: String!): Company
companies(first: Int, after: String, filter: CompanyFilter): CompanyConnection!

# Financial Period queries
financialPeriod(id: UUID!): FinancialPeriod
financialPeriods(companyId: UUID!, periodType: PeriodType, fiscalYear: Int): FinancialPeriodConnection!

# Financial Statement queries
balanceSheet(id: UUID!): BalanceSheet
incomeStatement(id: UUID!): IncomeStatement
cashFlowStatement(id: UUID!): CashFlowStatement

# SEC Filing queries
secFiling(id: UUID!): SecFiling
secFilingByAdsh(adsh: String!): SecFiling
secFilings(companyId: UUID, formType: String): SecFilingConnection!

# Estimate queries
brokerEstimates(companyId: UUID!, metric: EstimateMetric, fiscalYear: Int): BrokerEstimateConnection!
consensusEstimates(companyId: UUID!, metric: EstimateMetric, fiscalYear: Int): ConsensusEstimateConnection!

# Data Item queries
dataItem(id: UUID!): DataItem
dataItems(provider: String, sourceType: SourceType, query: String): DataItemConnection!

# Content queries (Elasticsearch)
contentDocument(id: String!): ContentDocument
contentDocuments(provider: String, contentType: String, query: String): ContentDocumentConnection!
```

**Mutations:**
```graphql
# Company mutations
createCompany(input: CreateCompanyInput!): Company!
updateCompany(id: UUID!, input: UpdateCompanyInput!): Company!
deleteCompany(id: UUID!): Boolean!

# DataItem mutations
createDataItem(input: CreateDataItemInput!): DataItem!
updateDataItem(id: UUID!, input: UpdateDataItemInput!): DataItem!
deleteDataItem(id: UUID!): Boolean!

# Content mutations
storeContent(input: StoreContentInput!): ContentDocument!
updateContent(id: String!, input: UpdateContentInput!): ContentDocument!
deleteContent(id: String!): Boolean!
```

### Custom Scalars

| Scalar | Java Type | Usage |
|--------|-----------|-------|
| `UUID` | `java.util.UUID` | Entity IDs |
| `DateTime` | `java.time.LocalDateTime` | Timestamps |
| `Date` | `java.time.LocalDate` | Date fields |
| `BigDecimal` | `java.math.BigDecimal` | Financial values |
| `JSON` | `Map<String, Object>` | Metadata fields |

### Pagination

Uses Relay-style cursor-based pagination:
```graphql
type CompanyConnection {
  edges: [CompanyEdge!]!
  pageInfo: PageInfo!
  totalCount: Int!
}

type CompanyEdge {
  node: Company!
  cursor: String!
}

type PageInfo {
  hasNextPage: Boolean!
  hasPreviousPage: Boolean!
  startCursor: String
  endCursor: String
}
```

### Example Queries

**Get company with financial data:**
```graphql
query {
  companyByTicker(ticker: "AAPL") {
    id
    name
    ticker
    sector
    financialPeriods(periodType: ANNUAL, first: 5) {
      edges {
        node {
          fiscalYear
          balanceSheets {
            totalAssets
            totalLiabilities
            totalEquity
          }
          incomeStatements {
            revenue
            netIncome
            epsBasic
          }
        }
      }
    }
  }
}
```

**Search data items:**
```graphql
query {
  dataItems(provider: "SEC", first: 10) {
    edges {
      node {
        id
        name
        sourceType
        contentDocuments {
          id
          title
        }
      }
    }
    totalCount
  }
}
```

---

## Financial Models (PostgreSQL)

| Model | Purpose | Key Fields |
|-------|---------|------------|
| `Company` | Master entity | ticker, cik, name, sector |
| `FinancialPeriod` | Time periods | periodType, fiscalYear, fiscalQuarter |
| `SecFiling` | SEC filings | adsh, formType, filingDate |
| `BalanceSheet` | Assets/Liabilities | currentAssets, totalLiabilities, equity |
| `IncomeStatement` | P&L | revenue, operatingIncome, netIncome, eps |
| `CashFlowStatement` | Cash flows | operatingCashFlow, freeCashFlow |
| `BrokerEstimate` | Analyst projections | broker, metric, estimatedValue |
| `ConsensusEstimate` | Aggregated estimates | mean, median, high, low |

---

## Project Structure

```
src/main/java/com/tianzige/marketplace/
├── config/
│   ├── JpaConfig.java
│   └── ElasticsearchConfig.java
├── controller/                    # REST controllers
│   ├── ContentController.java
│   └── DataItemController.java
├── graphql/                       # GraphQL layer
│   ├── controller/                # GraphQL resolvers
│   │   ├── CompanyGraphQLController.java
│   │   ├── FinancialPeriodGraphQLController.java
│   │   ├── FinancialStatementGraphQLController.java
│   │   ├── SecFilingGraphQLController.java
│   │   ├── EstimateGraphQLController.java
│   │   ├── DataItemGraphQLController.java
│   │   └── ContentGraphQLController.java
│   ├── input/                     # Input DTOs
│   │   ├── CompanyFilter.java
│   │   ├── CreateCompanyInput.java
│   │   └── ...
│   ├── scalar/
│   │   └── ScalarConfig.java      # Custom scalar registration
│   ├── pagination/
│   │   └── ConnectionUtils.java   # Relay pagination utilities
│   └── exception/
│       └── GraphQLExceptionHandler.java
├── model/
│   ├── content/
│   │   └── ContentDocument.java
│   ├── dir/
│   │   └── DataItem.java
│   └── financial/
│       ├── Company.java
│       ├── FinancialPeriod.java
│       ├── BalanceSheet.java
│       ├── IncomeStatement.java
│       ├── CashFlowStatement.java
│       ├── SecFiling.java
│       ├── BrokerEstimate.java
│       └── ConsensusEstimate.java
├── repository/
│   ├── ContentRepository.java
│   ├── DataItemRepository.java
│   └── financial/
│       ├── CompanyRepository.java
│       ├── FinancialPeriodRepository.java
│       ├── BalanceSheetRepository.java
│       ├── IncomeStatementRepository.java
│       ├── CashFlowStatementRepository.java
│       ├── SecFilingRepository.java
│       ├── BrokerEstimateRepository.java
│       └── ConsensusEstimateRepository.java
├── service/
│   ├── ContentService.java
│   ├── DataItemService.java
│   └── financial/
│       ├── CompanyService.java
│       ├── FinancialStatementService.java
│       ├── SecFilingService.java
│       └── EstimateService.java
└── ingest/
    ├── SecDatasetIngestService.java
    ├── SecZipParser.java
    └── TsvSchemaInferrer.java

src/main/resources/
├── application.yml
└── graphql/                       # GraphQL schema files
    ├── schema.graphqls            # Root Query/Mutation, scalars
    ├── company.graphqls
    ├── financial-statements.graphqls
    ├── sec-filing.graphqls
    ├── estimates.graphqls
    ├── data-item.graphqls
    └── content.graphqls
```

---

## Vision: General Financial Data Model with Adaptors

### Goal

Create a **unified financial data model** that normalizes data from heterogeneous sources into a consistent structure, enabling:
1. Cross-source querying and analysis via GraphQL
2. On-the-fly adaptor generation
3. Schema-driven data transformation

### Adaptor Architecture (Future)

```
Source Schema (DIR)          Target Schema (General Model)
     │                              │
     └──────────┬───────────────────┘
                │
         ┌──────▼──────┐
         │   Adaptor   │
         │  Generator  │
         └──────┬──────┘
                │
         ┌──────▼──────┐
         │   Runtime   │
         │   Adaptor   │
         └─────────────┘
```

---

## Quick Reference

### Run the Application
```bash
docker-compose up -d        # Start PostgreSQL + Elasticsearch
mvn spring-boot:run         # Run the application
```

### API Endpoints

| Endpoint | Type | Purpose |
|----------|------|---------|
| `/graphql` | GraphQL | Main API |
| `/graphiql` | UI | GraphQL playground |
| `/api/v1/data-items` | REST | Data item registry |
| `/api/v1/contents` | REST | Content documents |
| `/swagger-ui.html` | UI | REST API docs |

### Test GraphQL Query
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ companies(first: 5) { edges { node { id ticker name } } } }"}'
```

---

## Technology Stack

- **Framework**: Spring Boot 3.2.4 (Java 17)
- **API**: Spring for GraphQL + REST
- **Database**: PostgreSQL 15 (with JSONB via Hypersistence Utils)
- **Search**: Elasticsearch 8.12.0
- **Build**: Maven
- **Testing**: Testcontainers
