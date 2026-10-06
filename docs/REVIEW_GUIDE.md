# Project Execution Review: what to say

Each section matches a rubric item. Speak in your own words; this is the content.

## Problem statement
In a microservice system every service would otherwise need its own authentication, rate limiting,
logging and public address. Routes are usually hard-coded in config files, so changing one means a
redeploy, and releases are done by hand. The project builds a gateway that centralises these
concerns, a console to manage it live, and a DevOps pipeline that automates build, test, deploy and monitoring.

## Objectives
1. One secured entry point with JWT authentication and role-based access (ADMIN/USER).
2. Routes stored in MySQL and applied to the running gateway without restart.
3. Per-route rate limiting to protect backends.
4. Automated CI/CD with GitHub Actions and containerised deployment with Docker.
5. Real-time monitoring of traffic, latency and errors with Prometheus and Grafana.

## Research gap (state this carefully)
Many tutorials and tools show a gateway with static YAML routes, or a CI pipeline, or monitoring, each
alone. Heavy products (Kong, Apigee) are complex and costly for small teams. This project combines
dynamic route management, security, CI/CD and observability in one lightweight open-source stack.
Before the review, find 4 to 6 papers or articles (Google Scholar: "API gateway microservices",
"DevOps CI/CD pipeline", "Prometheus monitoring") and add them to your Literature Review slide with
author, year, what they did, and their limitation. Do not invent citations.

## Methodology
Agile in short sprints: (1) design and DB schema, (2) management service with auth, (3) gateway
filters and dynamic routing, (4) frontend console, (5) Docker + CI/CD, (6) monitoring and testing.

## Design decisions to explain
- Gateway and management are separate services: the gateway stays fast and stateless; admin logic is isolated.
- The gateway pulls routes from the management service (polling every 15 s). Simple and reliable. Trade-off: up to 15 s delay. A future version could push updates via a message queue.
- JWT is validated at the gateway and again in the management service (defence in depth).
- The `/internal/routes` endpoint uses a shared key so only the gateway can read the route table.
- Rate limiter is an in-memory fixed window, enough for one gateway instance. For several instances use Redis.

## Likely questions and answers
- **Why an API gateway at all?** One place for security, throttling, routing and monitoring instead of duplicating it in every service.
- **Why JWT?** Stateless, so the gateway can verify tokens without a database call.
- **How do routes change without restart?** The gateway's RouteSyncService fetches the table and writes it through RouteDefinitionWriter, then publishes a RefreshRoutesEvent.
- **What does CI/CD do here?** On every push GitHub Actions builds each service, runs tests, builds Docker images, and smoke-tests the whole stack with docker compose.
- **How do you monitor?** Spring Boot Actuator and Micrometer expose metrics at /actuator/prometheus, Prometheus scrapes them, Grafana charts them.
- **Limitations?** Polling delay, in-memory rate limiter, passwords/secrets in compose for demo only, no HTTPS. Future work: Redis rate limiting, HTTPS, Kubernetes, circuit breaker.
- **Who did what?** Agree this inside the team before the review: e.g. management service, gateway, frontend, DevOps/monitoring.

## Before the review checklist
- [ ] `docker compose up --build` works on the laptop you will present on, and you have run the demo script once.
- [ ] Code pushed to GitHub with at least one green Actions run.
- [ ] Architecture diagram slide (copy the one in README).
- [ ] Literature review slide filled with real references.
- [ ] Every team member can explain the whole flow, not only their part.
