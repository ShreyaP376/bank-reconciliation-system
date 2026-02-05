Bank Reconciliation System

A rule-based bank reconciliation engine that matches internal invoices with bank transactions using deterministic and fuzzy matching, supporting exact matches, partial payments, and overpayments.


How to Run
Prerequisites

Java 17+

Maven 3.8+

PostgreSQL 14+

Git

(Optional) IntelliJ IDEA


Local Setup

Clone the repository

git clone <repo-url>
cd bank-reconciliation


Create PostgreSQL database

CREATE DATABASE reconciliation_db;


Update application.yml

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/reconciliation_db
    username: postgres
    password: postgres


Build the project

mvn clean install

Run Tests
mvn test

Run Application
mvn spring-boot:run


Server starts at:

http://localhost:8080

System Architecture


<img width="685" height="755" alt="Screenshot 2026-02-03 232135" src="https://github.com/user-attachments/assets/afeba8e5-d45e-4026-9e22-92be87076d12" />



Current Design (Sample Data / Demo Scale)

Architecture

CSV Upload
   ↓
Spring Boot REST API
   ↓
Reconciliation Service (Rule Engine)
   ↓
PostgreSQL


Explanation

CSV files for bank statements and internal ledger are ingested.

Reconciliation runs synchronously.

Matching rules are executed in priority order.

Status is derived from matched amounts, not hardcoded.

This design works well for thousands of invoices and demo datasets.

Proposed Enterprise Design (1M+ Businesses)

Architecture

CSV Upload / Bank Feeds
        ↓
API Gateway
        ↓
Kafka / SQS Queue
        ↓
Reconciliation Worker Pool
        ↓
PostgreSQL (Sharded / Partitioned)
        ↓
Read Replica (Dashboards)


Explanation

Reconciliation runs asynchronously in background workers.

Each business is processed independently.

Horizontal scaling via message queues.

Read/write separation for dashboards.

Key Design Decisions
1. Database Choice: PostgreSQL

Rationale

Strong consistency guarantees (ACID).

Excellent support for joins and financial data.

Mature indexing and partitioning support.

Trade-offs

Harder horizontal scaling compared to NoSQL.

Requires careful schema and index design at scale.

2. Reconciliation Processing: Synchronous (Current) → Async (Future)

Rationale

Synchronous processing simplifies debugging and correctness.

Suitable for sample and interview-scale data.

Bottleneck

CPU spikes during fuzzy matching.

Request timeout at large volumes.

3. Matching Algorithm

Approach Used

Rule-based priority matching:

Exact match

Fuzzy match (Jaro–Winkler)

Partial payment (subset sum)

Overpayment handling

Time Complexity

Exact & fuzzy: O(n × m)

Subset sum (worst-case): O(2^n) (bounded by date & amount filters)

Space Complexity

O(n + m) for in-memory matching sets

Why This Works

Deterministic rules ensure explainability.

Confidence scores help resolve ambiguity.

Practical constraints (date, amount) reduce search space.

4. Data Isolation (Multi-tenancy)

Approach

business_id column on all core tables.

Queries always scoped by tenant.

How We Prevent Data Leakage

Repository-level filters by business_id

No cross-tenant joins

Future: Hibernate filters / row-level security

Security Considerations

Input validation on CSV uploads

Principle of least privilege for DB users

Audit logging for manual overrides

Experience with Scale

Have you handled 1M+ row systems before?
NO (This is a sample system)

Design decisions are based on real-world patterns and industry practices rather than direct production ownership.

What Breaks at 1M+ Businesses

At ~10k invoices per business:

☑ Synchronous reconciliation (too slow)

☑ Loading all invoices in dashboard (memory pressure)

☑ Fuzzy matching (O(n²) risk)

☑ Single DB instance (CPU / IO bound)

☑ CSV uploads timing out

How I’d Fix It

Move reconciliation to async worker queues

Partition data by business_id

Pre-filter candidate matches using indexes

Paginate dashboards + caching

Stream CSV ingestion (chunk-based)

Testing
Unit Tests

Exact match logic

Fuzzy confidence thresholding

Partial payment subset matching

Status derivation logic

How to Run
mvn test

Coverage

~70% (service-layer focused)

What’s NOT Included (And Why)

Real bank APIs — out of scope for sample project

Production-grade security — simplified for clarity

Actual payment processing — reconciliation only

Distributed locking — single-node demo

Future Improvements (Nice-to-have)

 Async background reconciliation

 Reconciliation confidence dashboard

 Docker Compose setup

 ML-based matcher

 Audit log viewer

 Batch & streaming imports

Final Note

This system prioritizes correctness, explainability, and extensibility over raw scale.
It is intentionally designed to evolve from a rule engine into a distributed reconciliation platform.
