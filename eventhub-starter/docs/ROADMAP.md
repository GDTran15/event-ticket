# EventHub — 8-week backend roadmap

**Assumption:** 15–25 focused hours per week. Each week closes with a runnable demo, a short ADR (architecture decision record), and tests for the risk introduced that week. One Spring Boot process and one PostgreSQL database throughout. Add components only when the measured problem warrants them.

## Week 1 — Foundation, identity, ownership

- **Build:** Run this starter with Docker Compose. Inspect V1 Flyway schema and JPA mappings. Complete register/login/me, admin grants ORGANIZER/STAFF, and add Event/TicketType create, update, publish, list/detail APIs. Only the owning organizer can edit an event. A staff grant alone does not grant access to every event; assign staff to an event.
- **Learn:** Spring Security filter chain, JWT signature/expiry, RBAC versus resource ownership, Flyway versus Hibernate `ddl-auto=validate`, lazy loading and DTO mapping.
- **Evidence:** Tests for customer creating an event (403), organizer A editing organizer B's event (403), invalid JWT (401), invalid sale window (400). Demo create draft → ticket type → publish.
- **ADR:** Why modular monolith; why role and ownership checks both exist.

## Week 2 — Inventory and reservation

- **Build:** `POST /api/reservations` with atomic conditional SQL update of `available_quantity`, reservation expires_at, cancel endpoint, scheduled expiry worker. One reservation per ticket type for the initial flow. Idempotent release using conditional status transition; use database row locking/claiming for multi-instance workers.
- **Learn:** Transactions, lost updates, atomic SQL, row locks, isolation, `@Modifying` persistence-context synchronization, partial indexes, clock/time boundaries.
- **Evidence:** PostgreSQL Testcontainers race: capacity 1, 50 buyers, exactly 1 success and available 0. Repeat expiry worker invocation and verify inventory restored once.
- **ADR:** Atomic conditional update versus optimistic/pessimistic locking; why PostgreSQL owns inventory state.

## Week 3 — Orders, prices and promotion

- **Build:** Create order tied 1:1 to active reservation; immutable `OrderItem.unitPrice` snapshot; server computes totals. Promotion percentage/fixed, time window, max uses, same-event constraint; atomic redemption and rollback. Clearly define whether reservation price is fixed and when payment deadline occurs.
- **Learn:** Money/BigDecimal, transactional boundaries, consistency invariants, unique constraints, concurrent coupon redemption, API idempotency.
- **Evidence:** Price changes after purchase do not alter order history; final coupon use has one winner; total = subtotal - discount; rejected promotion does not consume capacity.
- **ADR:** Price snapshot and coupon usage timing.

## Week 4 — Payment workflow and retries

- **Build:** Mock gateway with controllable success/failure/delay; `POST /api/orders/{id}/payments` requires idempotency key plus request hash. Persist attempt before external call, handle retries/in-flight state and gateway timeout reconciliation. Payment success transitions order to PAID and reservation to CONFIRMED once. Failures and expiry release inventory once. Never hold an SQL transaction open across a slow external call.
- **Learn:** At-least-once delivery, exactly-once business effects, outbox/inbox patterns, payment state machine, failure recovery.
- **Evidence:** 20 concurrent retries with one key make one logical payment; same key with changed body is rejected; simulated timeout then reconciliation preserves one final state.
- **ADR:** Payment idempotency and pending/reconciliation policy.

## Week 5 — Ticket issuance and venue check-in

- **Build:** Issue one ticket per purchased unit; QR payload is high-entropy opaque token, store only hash; staff scans for assigned event through `POST /api/events/{id}/check-ins`. Atomic VALID→USED transition and unique check-in constraint. Refund/cancellation invalidates ticket; no mobile frontend needed.
- **Learn:** Secure random tokens, hashing, replay protection, unique constraints, authorization by event assignment, state transitions.
- **Evidence:** Two simultaneous scans: exactly one succeeds; wrong-event staff 403; second scan 409; unpaid ticket never issued.
- **ADR:** Opaque QR token versus signed JWT QR.

## Week 6 — Read models, live updates and analytics

- **Build:** Organizer-only revenue/sales/attendance APIs using SQL aggregation and indexes. Add SSE `GET /api/events/{id}/sales/stream` from AFTER_COMMIT events; send snapshot on connect and resync after reconnect. Optional outbox if event delivery must survive process restart.
- **Learn:** Read versus write paths, query plans, N+1/pagination, transaction events, SSE ordering/reconnect, eventual consistency.
- **Evidence:** Explain/analyze key report queries, compare totals against paid order and checked-in ticket fixtures; sales subscribers see committed changes only.
- **ADR:** SSE versus WebSocket, and acceptable update loss for dashboard UI.

## Week 7 — Load, failure and deployment hardening

- **Build:** Docker Compose app + PostgreSQL health check; add CI pipeline for Maven tests and image build. Add structured logs with request ID, Actuator, metrics, load script and backups. Run 2 app replicas against one DB for reservation/expiry/scan race validation. Consider Redis only if measurements show a need; never use cache TTL as sole release mechanism.
- **Learn:** Horizontal scaling, connection pools, bottlenecks, indexes, load testing, observability, deployment secrets, backup/restore.
- **Evidence:** Reproducible load results (throughput, p95, error rate), no overselling, restore database backup into fresh environment, documented deployment on Docker host.
- **ADR:** Bottleneck found and change justified by metrics.

## Week 8 — Portfolio finish

- **Build:** API reference and Postman/HTTP examples; system context, sequence of purchase/payment/check-in, ERD, ADRs, threat/failure table, seeded demo scenario, Docker host deployment with HTTPS and private DB network. Optional rule-based dynamic pricing only after all core checks pass; show quoted price persisted when reserved. Social integration and dedicated scanner app remain out of scope.
- **Learn:** Trade-off communication, operating a service, incident/recovery story, interview explanation.
- **Evidence:** Recorded demo: organizer publishes 1 ticket; two buyers race (one wins); payment retry gives one success; QR first scan accepts and second rejects; dashboard reconciles totals. README lists real measured numbers and limitations.

## Interview-ready artifacts

1. Architecture diagram: clients → Spring Boot modules → PostgreSQL; SSE optional.
2. Data invariants and state transitions for reservation/order/payment/ticket.
3. Concurrency test with actual PostgreSQL and explanation of lock/update strategy.
4. Failure test for retry/reconciliation, logs and measured load results.
5. Public repo with safe sample environment, CI, clear runbook and working Docker demo.

**Scope guard:** the PDF also mentions multimedia pages, social media, dynamic pricing, fraud prevention and a mobile scanning app. The backend MVP covers ticketing, pricing tiers, promotion, capacity, payment workflow, analytics and a scanning API. Add dynamic pricing as a controlled extension after the core lifecycle; payment remains mocked until a real provider is deliberately integrated. Do not claim real charges, fraud detection or a mobile application on the CV unless implemented.
