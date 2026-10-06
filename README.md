# DevOps-Driven API Gateway Management System

Team: Bobba Snehalatha Reddy (lead), Thulasi Edhala, U Harpita, G Bhavani
Guide: Ms. Anjana Devi Akurathi, Dept. of CSE-HTE

A single secured entry point for backend microservices. Admins manage routes from a web console,
the gateway applies them live, and the whole system is built, tested, containerised and monitored
through a DevOps pipeline.

## Architecture

```
React console ──► Gateway (Spring Cloud Gateway :8080) ──► Backend services (e.g. sample-backend :8082)
                      │  JWT check · rate limit · metrics
                      │  pulls route table every 15 s
                      ▼
              Management service (Spring Boot :8081) ──► MySQL
              auth, JWT issue, route CRUD
Prometheus (:9090) scrapes gateway + management ──► Grafana (:3000)
GitHub Actions: build → test → docker build → full-stack smoke test
```

## Run it (needs Docker Desktop)

```
docker compose up --build
```

| URL | What |
|---|---|
| http://localhost:5173 | Admin console (login `admin` / `admin123`) |
| http://localhost:8080 | Gateway |
| http://localhost:9090 | Prometheus |
| http://localhost:3000 | Grafana (`admin` / `admin`) |

Without Docker: start MySQL, then `mvn spring-boot:run` in `management-service`, `sample-backend`,
`gateway-service`, and `npm install && npm run dev` in `frontend`.

## Demo script for the review (5 minutes)

1. `curl localhost:8080/products` → **401** (no token).
2. Login in the console, or: `curl -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}'`
3. `curl localhost:8080/products -H "Authorization: Bearer <token>"` → **200** with products.
4. Click "Send test request" 21 times in the console → **429** (limit is 20/min on that route).
5. In the console, add route `/orders/**` or disable `product-service`. Wait ~15 s. Request again → **404**. This shows live route changes with no restart.
6. Open Grafana → Explore → run the queries below to show traffic.
7. Show the GitHub Actions run (green build, tests, docker build).

Grafana / Prometheus queries:
- Requests per second by route: `sum by (routeId) (rate(spring_cloud_gateway_requests_seconds_count[1m]))`
- Average latency: `rate(spring_cloud_gateway_requests_seconds_sum[1m]) / rate(spring_cloud_gateway_requests_seconds_count[1m])`
- Errors: `sum by (httpStatusCode) (rate(spring_cloud_gateway_requests_seconds_count[1m]))`
(If a metric name differs on your version, open Prometheus → search "gateway" in the metric box.)

## API summary

| Method | Path | Access |
|---|---|---|
| POST | /api/auth/register | public |
| POST | /api/auth/login | public, returns JWT |
| GET | /api/routes | any logged-in user |
| POST / PUT / DELETE | /api/routes[/{id}] | ADMIN |
| GET | /internal/routes | gateway only (shared key) |

## Postman

Create a collection with: Login (save `token`), List routes, Add route, Call `/products` through the gateway.
Use `Authorization: Bearer {{token}}`.
