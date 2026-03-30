# Content Marketplace - Project Context

## Overview

This project is a **Financial Content Marketplace** that implements a dual-storage architecture for managing financial data from multiple sources (SEC EDGAR, LSEG, Bloomberg, brokers, etc.) with the goal of creating a unified, general financial data model.

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
  - `sourceFormat` - MIME type (e.g., "text/tab-separated-values")
  - `contentType` - Domain category (e.g., "financial/sec-edgar")
  - `provider` - Data source (e.g., "SEC", "Bloomberg", "Reuters")
  - `version` - Dataset version (e.g., "2025Q4")
  - `description` - Human-readable description
  - `elasticsearchId` - Link to Elasticsearch index containing actual data
  - `metadata` (JSONB) - Flexible schema storage including:
    - Field definitions (name, type, nullable)
    - Row counts
    - Source-specific attributes

### Actual Data - Elasticsearch

The actual data records are stored in **Elasticsearch** for full-text search and flexible querying.

**Key Document: `ContentDocument`** (`content_documents` index)
- **Purpose**: Stores actual data rows with search capabilities
- **Fields**:
  - `id` - Document ID
  - `dataItemId` - **Link back to DIR** (PostgreSQL DataItem UUID)
  - `rawContent` - Original unparsed content (JSON serialization)
  - `normalizedContent` - Cleaned/standardized content
  - `extractedFields` - Parsed field values as Map
  - `contentType`, `sourceType`, `provider` - Categorization
  - `title`, `summary` - Human-readable descriptors
  - `indexedAt`, `updatedAt` - Timestamps

### Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                        DATA FLOW                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   External Data Feeds                                                │
│   ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐               │
│   │SEC EDGAR │ │Bloomberg │ │ Reuters  │ │  Custom  │               │
│   └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘               │
│        │            │            │            │                      │
│        └────────────┴─────┬──────┴────────────┘                      │
│                           │                                          │
│                           ▼                                          │
│                    ┌─────────────┐                                   │
│                    │  ETL Layer  │                                   │
│                    │  (Adaptors) │                                   │
│                    └──────┬──────┘                                   │
│                           │                                          │
│           ┌───────────────┴───────────────┐                          │
│           ▼                               ▼                          │
│   ┌───────────────┐               ┌───────────────┐                  │
│   │  PostgreSQL   │               │ Elasticsearch │                  │
│   │     (DIR)     │◄──────────────│   (Content)   │                  │
│   │               │  dataItemId   │               │                  │
│   │ - Schema      │               │ - Raw Data    │                  │
│   │ - Metadata    │               │ - Searchable  │                  │
│   │ - Registry    │               │ - Normalized  │                  │
│   └───────────────┘               └───────────────┘                  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Vision: General Financial Data Model

### Goal

Create a **unified financial data model** that normalizes data from heterogeneous sources into a consistent structure, enabling:
1. Cross-source querying and analysis
2. On-the-fly adaptor generation
3. Schema-driven data transformation

### Current Financial Models (PostgreSQL)

The system already has normalized financial models:

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

### Adaptor Architecture (Future)

```
┌─────────────────────────────────────────────────────────────────────┐
│                    ADAPTOR GENERATION FLOW                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   ┌─────────────────────┐       ┌─────────────────────┐             │
│   │  Source Schema      │       │  Target Schema      │             │
│   │  (in DIR)           │       │  (General Model)    │             │
│   │                     │       │                     │             │
│   │  SEC num.txt:       │       │  FinancialMetric:   │             │
│   │  - adsh: string     │       │  - companyId: UUID  │             │
│   │  - tag: string      │       │  - period: Period   │             │
│   │  - value: decimal   │       │  - metric: enum     │             │
│   │  - qtrs: int        │       │  - value: decimal   │             │
│   └──────────┬──────────┘       └──────────┬──────────┘             │
│              │                             │                         │
│              │     ┌───────────────┐       │                         │
│              └────►│   Adaptor     │◄──────┘                         │
│                    │   Generator   │                                 │
│                    │               │                                 │
│                    │  - Field Map  │                                 │
│                    │  - Transform  │                                 │
│                    │  - Validate   │                                 │
│                    └───────┬───────┘                                 │
│                            │                                         │
│                            ▼                                         │
│                    ┌───────────────┐                                 │
│                    │ Runtime       │                                 │
│                    │ Adaptor       │                                 │
│                    │ Instance      │                                 │
│                    └───────────────┘                                 │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## ETL Pipeline

### Current Implementation

The system has an SEC EDGAR ingest pipeline:

**Components:**
- `SecDatasetIngestService` - Orchestrates ZIP file ingestion
- `SecZipParser` - Extracts and parses TSV files
- `TsvSchemaInferrer` - Automatically infers field types

**Flow:**
```
SEC ZIP → Parse TSV → Infer Schema → Create DataItem (DIR) → Index Content (ES)
```

### Future: Generalized ETL with Dynamic Adaptors

```
┌─────────────────────────────────────────────────────────────────────┐
│                     GENERALIZED ETL PIPELINE                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   1. INGEST                                                         │
│      ┌─────────────────────────────────────────────────────────┐    │
│      │ Source Connector (SEC, Bloomberg, CSV, API, etc.)       │    │
│      │ → Parse raw data                                        │    │
│      │ → Infer/validate schema                                 │    │
│      │ → Register in DIR with source schema                    │    │
│      └─────────────────────────────────────────────────────────┘    │
│                               │                                      │
│                               ▼                                      │
│   2. TRANSFORM                                                       │
│      ┌─────────────────────────────────────────────────────────┐    │
│      │ Adaptor (generated from DIR source + target schemas)   │    │
│      │ → Map source fields to general model                   │    │
│      │ → Type conversion                                      │    │
│      │ → Validation & enrichment                              │    │
│      │ → Handle nulls/defaults                                │    │
│      └─────────────────────────────────────────────────────────┘    │
│                               │                                      │
│                               ▼                                      │
│   3. LOAD                                                            │
│      ┌─────────────────────────────────────────────────────────┐    │
│      │ Dual Write:                                             │    │
│      │ → PostgreSQL: Normalized financial entities             │    │
│      │ → Elasticsearch: Searchable content with link to DIR   │    │
│      └─────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Key Concepts

### 1. Schema in DIR

Each `DataItem` stores its schema in the `metadata` JSONB field:

```json
{
  "filename": "num.txt",
  "quarter": "2025Q4",
  "totalRows": 1500000,
  "indexedRows": 1500000,
  "fieldCount": 9,
  "fields": [
    {"name": "adsh", "type": "string", "nullable": false},
    {"name": "tag", "type": "string", "nullable": false},
    {"name": "version", "type": "string", "nullable": true},
    {"name": "ddate", "type": "integer", "nullable": false},
    {"name": "qtrs", "type": "integer", "nullable": false},
    {"name": "uom", "type": "string", "nullable": true},
    {"name": "value", "type": "decimal", "nullable": true},
    {"name": "footnote", "type": "string", "nullable": true}
  ]
}
```

### 2. General Financial Data Model in DIR

Define target schemas for normalized financial data:

```json
{
  "name": "GeneralFinancialMetric",
  "version": "1.0",
  "fields": [
    {"name": "companyId", "type": "uuid", "nullable": false},
    {"name": "periodType", "type": "enum", "values": ["ANNUAL", "QUARTERLY", "TTM"]},
    {"name": "fiscalYear", "type": "integer", "nullable": false},
    {"name": "fiscalQuarter", "type": "integer", "nullable": true},
    {"name": "metricName", "type": "string", "nullable": false},
    {"name": "value", "type": "decimal", "nullable": true},
    {"name": "currency", "type": "string", "default": "USD"},
    {"name": "source", "type": "string", "nullable": false},
    {"name": "sourceId", "type": "string", "nullable": false}
  ]
}
```

### 3. Adaptor Definition (Future)

Adaptors map source schemas to target schemas:

```json
{
  "name": "SEC-NUM-to-GeneralFinancialMetric",
  "sourceDataItemId": "uuid-of-sec-num-dataitem",
  "targetSchema": "GeneralFinancialMetric",
  "fieldMappings": [
    {
      "source": "adsh",
      "target": "sourceId",
      "transform": null
    },
    {
      "source": "tag",
      "target": "metricName",
      "transform": "NORMALIZE_SEC_TAG"
    },
    {
      "source": "value",
      "target": "value",
      "transform": null
    },
    {
      "source": null,
      "target": "source",
      "transform": "CONSTANT",
      "value": "SEC_EDGAR"
    }
  ],
  "lookups": [
    {
      "field": "companyId",
      "source": "adsh",
      "lookupTable": "sec_filings",
      "lookupField": "adsh",
      "returnField": "company_id"
    }
  ]
}
```

---

## Project Structure

```
src/main/java/com/tianzige/marketplace/
├── config/
│   ├── JpaConfig.java              # JPA auditing
│   └── ElasticsearchConfig.java    # ES repositories
├── controller/
│   ├── ContentController.java      # /api/v1/contents
│   └── DataItemController.java     # /api/v1/data-items
├── model/
│   ├── content/
│   │   └── ContentDocument.java    # ES document
│   ├── dir/
│   │   └── DataItem.java           # Registry entity
│   └── financial/
│       ├── Company.java
│       ├── FinancialPeriod.java
│       ├── SecFiling.java
│       ├── BalanceSheet.java
│       ├── IncomeStatement.java
│       ├── CashFlowStatement.java
│       ├── BrokerEstimate.java
│       └── ConsensusEstimate.java
├── repository/
│   ├── ContentRepository.java      # ES repository
│   └── DataItemRepository.java     # JPA repository
├── service/
│   ├── ContentService.java
│   └── DataItemService.java
└── ingest/
    ├── SecDatasetIngestService.java  # SEC ETL orchestrator
    ├── SecZipParser.java             # ZIP/TSV parser
    ├── TsvSchemaInferrer.java        # Schema inference
    ├── ParsedFile.java
    └── IngestSummary.java
```

---

## Next Steps / Roadmap

1. **Extend DIR for Target Schemas**
   - Add `schemaType` field (SOURCE vs TARGET)
   - Store general financial data model schemas in DIR

2. **Implement Adaptor Registry**
   - New `Adaptor` entity linking source → target schemas
   - Field mapping definitions
   - Transform function registry

3. **Dynamic Adaptor Generation**
   - Given source schema (DIR) + target schema (DIR)
   - Generate field mappings automatically where possible
   - Support manual override/customization

4. **Pluggable Transform Functions**
   - Type conversions
   - Normalization (e.g., SEC tag → standard metric name)
   - Lookups (e.g., ADSH → Company ID)
   - Calculations (e.g., derive ratios)

5. **Additional Data Connectors**
   - Bloomberg API connector
   - Reuters connector
   - CSV/Excel file ingestion
   - REST API ingestion

---

## Technology Stack

- **Framework**: Spring Boot 3.2.4 (Java 17)
- **Database**: PostgreSQL 15 (with JSONB via Hypersistence Utils)
- **Search**: Elasticsearch 8.12.0
- **Build**: Maven
- **Testing**: Testcontainers
- **API Docs**: Springdoc OpenAPI/Swagger

---

## Quick Reference

### Run the Application
```bash
docker-compose up -d        # Start PostgreSQL + Elasticsearch
./mvnw spring-boot:run      # Run the application
```

### API Endpoints
- `POST /api/v1/data-items` - Register new data source
- `GET /api/v1/data-items` - List all registered sources
- `POST /api/v1/contents` - Store content document
- `GET /api/v1/contents/search?query=...` - Full-text search
- `GET /api/v1/contents/by-data-item/{dataItemId}` - Get content by registry entry

### Ingest SEC Data
```java
@Autowired SecDatasetIngestService ingestService;

try (InputStream zipStream = new FileInputStream("sec-2025q4.zip")) {
    IngestSummary summary = ingestService.ingestSecDataset(zipStream, "2025Q4", 0);
    // summary contains per-file results with DataItem IDs
}
```
