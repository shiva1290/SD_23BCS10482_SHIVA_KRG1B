# Elasticsearch vs PostgreSQL Performance Comparison

This project is a Spring Boot application demonstrating the query execution speed differences between a relational database (PostgreSQL) and a full-text search engine (Elasticsearch).

It uses a 2,000,000-row synthetic dataset representing an eCommerce product catalog (e.g., Sony Digital Audio, Apple 4K Televisions).

## How It Works

Upon application startup, the `SearchService` checks if the databases contain data. If empty, the application generates 2,000,000 product records and inserts them into both databases simultaneously.

Data Load Implementation: Instead of using standard Spring Data JPA `saveAll()` methods to initialize this dataset, the application uses Spring `JdbcTemplate.batchUpdate()`. Using raw SQL inserts in chunks of 10,000 optimizes memory usage and prevents JVM OutOfMemory exceptions during startup.

When a search is executed via the frontend interface at `http://localhost:8080`, the query is executed against both engines independently to measure and compare response times.

## Architectural Comparison

### 1. PostgreSQL Search (Relational Database)
A standard SQL search implementation uses an `ILIKE` condition:
```sql
SELECT * FROM products p 
WHERE p.name ILIKE '%query%' OR p.description ILIKE '%query%' 
ORDER BY p.timestamp DESC 
LIMIT 50;
```

A relational database is optimized for exact matches. Running an `ILIKE` query requires PostgreSQL to perform a sequential scan, checking characters across the 2,000,000 textual records for matching substrings.

The `ORDER BY timestamp DESC` clause prevents query short-circuiting. Without it, PostgreSQL stops evaluating rows once it finds the first 50 matches. By enforcing a sort order, PostgreSQL evaluates the `ILIKE` condition on all rows before sorting, which accurately simulates real-world search queries. This operation typically completes in 500ms to 2000ms.

### 2. Elasticsearch (Full-Text Search Engine)
Instead of a sequential scan, Elasticsearch relies on an Inverted Index. During initialization, Elasticsearch tokenizes product names (e.g., `["apple", "digital", "audio"]`) and creates an indexed dictionary mapping tokens directly to the relevant documents.

The application executes a wildcard `query_string` request:
```json
{"query_string": {"query": "*query*", "fields": ["name", "description"]}}
```
Executing this query identifies matched tokens using the index instead of iterating through rows. The engine scores results based on relevance and returns matches quickly. This provides real-time search capabilities.

## Installation Methods

### Prerequisites
1. Docker Desktop
2. Java 17 or higher

### Setup Instructions

1. Start the PostgreSQL and Elasticsearch containers using Docker:
```bash
docker compose up -d
```
Note: PostgreSQL is configured to use port 5434.

2. Run the Spring Boot application:
```bash
./mvnw spring-boot:run
```

3. Navigate to `http://localhost:8080` in a web browser. The initial dataset generation will take approximately 3 minutes after the application launches to fully populate PostgreSQL and Elasticsearch.
