# Movie Store API

## Overview

This project is a backend service built as part of a technical challenge for a movie store platform.

The system allows:

- **Merchants** to search movies from an external catalogue, create and manage store listings, and view transaction history related to their store.
- **Customers** to browse merchant stores, create payments for movie purchase or rental, confirm payments using simulated card details, and view their own transaction history.

The implementation has been developed using **Java 25**, **Quarkus**, **PostgreSQL**, **Flyway**, and **Docker Compose**.

The solution focuses not only on feature delivery, but also on the operational concerns explicitly requested in the challenge, namely:

- resilience when integrating with an external movie service;
- payment lifecycle modelling;
- correctness under retries, duplicates, and concurrent requests;
- clear separation between listings, payments, and transaction history.

---

## Main Features

### Merchant capabilities

A merchant can:

- search movies using the external movie API;
- select a movie from the external catalogue;
- add the movie to their store by referencing the external movie identifier;
- define purchase price and/or rental price;
- define available stock;
- view their own listings;
- retrieve a single listing by identifier;
- update prices, stock, and activation status;
- view transaction history related to their store.

### Customer capabilities

A customer can:

- browse a merchant's store;
- view listing details, including price and available stock;
- create a payment for either **purchase** or **rental**;
- confirm a payment using simulated card details;
- retrieve payment details;
- view personal transaction history.

---

## External Movie Integration

Movie information is retrieved from an external movie API.

The integration follows these principles:

- movies are treated as data owned by an external service;
- the platform does not create its own movie catalogue independently of the external source;
- when a merchant creates a listing, the system resolves the external movie identifier against the external catalogue and stores a **local snapshot** of the required movie data inside the listing.

This approach was chosen in order to preserve:

- **data consistency** at listing creation time;
- **system resilience**, even if the external service becomes unavailable later;
- **historical correctness**, so that existing listings and payments remain meaningful even if the external service changes or becomes unavailable.

The movie service includes timeout and retry behaviour for external requests in order to behave reasonably when the external API is slow or temporarily unavailable.

---

## Technology Stack

- **Java 25**
- **Quarkus**
- **PostgreSQL**
- **Hibernate ORM with Panache**
- **Flyway**
- **REST Client**
- **Hibernate Validator**
- **SmallRye OpenAPI / Swagger UI**
- **Docker / Docker Compose**
- **JUnit 5 / Mockito / Quarkus integration testing**

---

## Project Structure

The codebase is organised by business domain.

```text
src/main/java/com/lagrodrigues/moviestore
├── auth
├── common
├── listing
├── movie
├── payment
├── transaction
└── user
```

### Package responsibilities

- **auth**: API key authentication, authenticated user resolution, and role-based checks.
- **common**: shared utilities and custom API exception handling.
- **listing**: merchant store listings and customer store browsing.
- **movie**: external movie API integration and mapping.
- **payment**: payment creation, confirmation, lifecycle management, and idempotency handling.
- **transaction**: immutable payment transaction history and transaction listing endpoints.
- **user**: user model and user-related persistence.

---

## Data Model

### AppUser

Represents an authenticated platform user.

Relevant fields include:

- `id`
- `username`
- `apiKey`
- `role`
- `createdAt`

Roles currently supported:

- `MERCHANT`
- `CUSTOMER`

### StoreListing

Represents a merchant listing in the store.

Relevant fields include:

- `id`
- `merchant`
- `externalMovieId`
- `movieTitle`
- `movieYear`
- `moviePosterUrl`
- `purchasePrice`
- `rentalPrice`
- `stock`
- `active`
- `createdAt`
- `updatedAt`
- `version`

A single listing may support:

- purchase only;
- rental only;
- both purchase and rental.

### Payment

Represents the current state of a payment.

Relevant fields include:

- `id`
- `customer`
- `merchant`
- `storeListing`
- `idempotencyKey`
- `externalMovieId`
- `movieTitle`
- `type`
- `amount`
- `status`
- `failureReason`
- `cardLast4`
- `createdAt`
- `updatedAt`
- `version`

### PaymentTransaction

Represents an immutable transaction history entry describing a payment state transition.

Relevant fields include:

- `id`
- `payment`
- `fromStatus`
- `toStatus`
- `amount`
- `message`
- `createdAt`

This separation between **Payment** and **PaymentTransaction** was intentional:

- `Payment` stores the **current state**;
- `PaymentTransaction` stores the **history of transitions**.

This improves observability, troubleshooting, and auditability.

---

## Payment Lifecycle

Payments follow the lifecycle below:

```text
CREATED → PROCESSING → SUCCEEDED / FAILED
```

### Lifecycle semantics

- **CREATED**: the payment has been created but not yet confirmed.
- **PROCESSING**: payment confirmation is being processed.
- **SUCCEEDED**: payment completed successfully.
- **FAILED**: payment confirmation failed.

### Transaction history examples

A successful payment typically produces transaction history entries similar to:

```text
null        -> CREATED
CREATED     -> PROCESSING
PROCESSING  -> SUCCEEDED
```

A failed payment typically produces transaction history entries similar to:

```text
null        -> CREATED
CREATED     -> PROCESSING
PROCESSING  -> FAILED
```

The initial transition uses `fromStatus = null` because no previous persisted lifecycle state exists before creation.

---

## Security Model

The system uses a simple **API key** authentication approach.

Each request must include a valid API key header.

### Example header

```http
X-API-KEY: customer-key
```

### Seed users

The project includes two seed users for quick execution and demonstration:

| Role     | Username | API Key      |
|----------|----------|--------------|
| Merchant | admin    | admin-key    |
| Customer | user     | customer-key |

Role-based access is enforced in the application layer.

---

## API Overview

### Authentication

#### Get current authenticated user

```http
GET /api/auth/me
```

---

### Merchant Listings

#### Create listing

```http
POST /api/merchant/listings
```

#### Get merchant listings

```http
GET /api/merchant/listings
```

#### Get merchant listing by id

```http
GET /api/merchant/listings/{listingId}
```

#### Update merchant listing

```http
PUT /api/merchant/listings/{listingId}
```

---

### Customer Store Browsing

#### Browse merchant store

```http
GET /api/stores/{merchantId}/listings
```

#### Get store listing details

```http
GET /api/stores/{merchantId}/listings/{listingId}
```

---

### Payments

#### Create payment

```http
POST /api/payments
```

This endpoint requires an idempotency header:

```http
Idempotency-Key: payment-create-001
```

#### Confirm payment

```http
POST /api/payments/{paymentId}/confirm
```

#### Get customer payment

```http
GET /api/payments/customer/{paymentId}
```

#### Get merchant payment

```http
GET /api/payments/merchant/{paymentId}
```

---

### Transaction History

#### Merchant transaction history

```http
GET /api/transactions/merchant
```

Supported filters include:

- `paymentId`
- `customerId`
- `fromStatus`
- `toStatus`
- `page`
- `size`
- `sortBy`
- `sortDirection`

#### Customer transaction history

```http
GET /api/transactions/customer
```

Supported filters include:

- `paymentId`
- `merchantId`
- `fromStatus`
- `toStatus`
- `page`
- `size`
- `sortBy`
- `sortDirection`

---

## Running the Service

### Prerequisites

- Docker
- Docker Compose

The service can be started with a single command from the project root.

#### Windows

```bat
run.bat
```

#### Direct Docker Compose command

```bash
docker compose up --build
```

### Exposed services

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/q/swagger-ui`
- OpenAPI: `http://localhost:8080/q/openapi`
- PostgreSQL: `localhost:5432`

The Docker Compose setup starts:

- PostgreSQL
- the Quarkus API service

Flyway migrations are applied automatically on startup.

---

## Running the Service Locally (Optional)

The application may also be started locally if Java and Maven are available.

```bash
mvn quarkus:dev
```

This mode is mainly useful during development.

The recommended delivery and evaluation path remains Docker Compose.

---

## Running the Tests

### Unit and integration tests

```bash
mvn test
```

The project includes:

- unit tests for service-layer business logic;
- integration tests for the most important API flows.

### Coverage focus

Test coverage was prioritised around the most critical behaviour:

- payment creation;
- idempotency;
- payment confirmation;
- payment lifecycle transitions;
- transaction history retrieval;
- listing creation and update;
- external movie service mapping and error handling.

---

## Example Usage

### 1. Create a payment

```http
POST /api/payments
X-API-KEY: customer-key
Idempotency-Key: payment-001
Content-Type: application/json
```

```json
{
  "listingId": "<listing-uuid>",
  "saleType": "PURCHASE"
}
```

### 2. Confirm a payment

```http
POST /api/payments/{paymentId}/confirm
X-API-KEY: customer-key
Content-Type: application/json
```

```json
{
  "cardNumber": "4111111111111111",
  "cardHolderName": "John Doe",
  "expiryMonth": 12,
  "expiryYear": 2028,
  "cvv": "123"
}
```

### 3. Retrieve customer transactions

```http
GET /api/transactions/customer?toStatus=SUCCEEDED&page=0&size=10
X-API-KEY: customer-key
```

### 4. Retrieve merchant transactions

```http
GET /api/transactions/merchant?customerId=<customer-uuid>
X-API-KEY: admin-key
```

---

## Design Decisions and Trade-offs

### 1. Relational storage was chosen deliberately

PostgreSQL was selected because the challenge places strong emphasis on:

- correctness under retries;
- duplicate request handling;
- concurrency safety;
- modelling of payments and transactions.

A relational database is well suited to these needs because it provides:

- transactional guarantees;
- referential integrity;
- unique constraints;
- row locking;
- straightforward query support for filtered transaction history.

### 2. Payment state and transaction history were separated

Instead of storing only the latest payment state, the solution stores:

- a mutable `Payment` entity for the current state;
- an immutable `PaymentTransaction` history for all state transitions.

This provides:

- better auditability;
- easier debugging;
- clearer reasoning about lifecycle transitions.

### 3. Movie data is snapshotted into listings

Although movies belong to the external service, listing creation stores a local snapshot of the relevant movie data.

This trade-off was chosen in order to:

- preserve historical correctness;
- avoid breaking listings if the external service becomes unavailable later;
- reduce repeated dependence on the external provider for already-created listings and payments.

### 4. Payments use idempotency keys

Payment creation supports an `Idempotency-Key` header.

This was introduced to ensure correctness when client requests are retried or duplicated.

If the same customer submits the same idempotency key again, the system returns the existing payment instead of creating a duplicate.

### 5. Pessimistic locking was used for payment confirmation

Payment confirmation uses pessimistic locking on both:

- the payment being confirmed;
- the store listing whose stock is being decremented.

This was chosen over a more lightweight optimistic strategy because the challenge explicitly prioritises correctness under concurrent interaction.

The selected approach is easier to reason about and better suited to demonstrating strong consistency guarantees in a challenge context.

### 6. The payment provider is simulated

Card confirmation is intentionally simulated.

The project therefore models `PROCESSING` as an internal lifecycle stage rather than as a real asynchronous callback from an external payment gateway.

In a production system, this stage would often be completed asynchronously via webhook or polling integration.

---

## Correctness Under Retries, Duplicates, and Concurrency

This implementation addresses the main operational concerns requested by the challenge.

### Retries and duplicate requests

- payment creation uses idempotency keys;
- duplicate create requests return the original payment;
- race conditions during duplicate creation are handled by combining a unique constraint with recovery lookup logic.

### Multiple actors interacting with the same payment

- customers can create and confirm payments;
- merchants can inspect payments and transaction history related to their store;
- transaction history provides visibility into lifecycle transitions over time.

### Concurrent requests

- payment confirmation locks the target payment row;
- listing stock update is protected by locking the target listing row;
- a payment cannot be processed more than once.

---

## Assumptions

The following assumptions were made in order to keep the implementation focused and proportional to the challenge:

- a single listing may support purchase, rental, or both;
- one shared stock value is used for the listing rather than separate stock pools for purchase and rental;
- payment confirmation is initiated by the customer, not by the merchant;
- transaction history endpoints expose lifecycle transitions rather than payment summaries;
- API key authentication is sufficient for the purpose of the exercise.

---

## Limitations and Possible Future Improvements

The implementation is intentionally focused on the scope of the challenge.

Possible future improvements include:

- replacing API key authentication with JWT or OAuth 2.0;
- introducing a real payment provider integration;
- supporting asynchronous payment completion via callbacks or message-driven workflow;
- introducing structured validation for pagination limits and richer query filtering;
- adding more extensive integration and end-to-end tests;
- adding CI/CD pipelines and automated quality checks;
- introducing separate DTO variants for public API consumers and administrative/internal views.

---

## Deliverables Summary

This repository provides:

- a runnable backend service implementation;
- source code in GitHub;
- instructions for running and interacting with the system;
- architecture decisions and trade-offs documentation;
- Dockerfile and Docker Compose support.

---

## Repository

GitHub repository:

`https://github.com/lagrodrigues/moviestore`

