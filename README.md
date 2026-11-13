# FreightFox Lane Optimizer

A freight-procurement demo for assigning transporters to trade lanes using the lowest available quotes while covering every lane. The backend is the required Java/Spring Boot REST API; the Next.js dashboard is an optional presentation layer for the requested Vercel demo.

## Optimization rule

The `maxTransporters` value is an **upper bound**, not a requirement to use exactly that many carriers. The solver finds the minimum sum of lane quotes subject to every lane being assigned exactly once and the number of distinct assigned carriers not exceeding the limit. This follows the assignment's “up to a user-specified limit” language and avoids forcing a more expensive carrier into the plan. If any lane has no quote, the API returns `400` with the uncovered lane ID. The exact dynamic-programming solver supports up to 18 carriers per run.

## Stack

- Java 21, Spring Boot 3.4, Spring Web, Bean Validation, Spring Data JPA / JDBC
- PostgreSQL 16 with Flyway-managed schema
- Next.js 15 + React 19 + TypeScript for the optional dashboard
- JUnit 5 for the optimizer tests; Swagger UI at `/swagger-ui.html`

## Run locally

Prerequisites: Java 21, Maven 3.9+, Node.js 20+, npm, and Docker Desktop.

1. Start PostgreSQL: `docker compose up -d postgres`
2. Start API: `cd backend` then `mvn spring-boot:run`
3. In another terminal: `cd frontend`, `npm install`, then `npm run dev`
4. Open `http://localhost:3000`. The sample dataset is preloaded in the dashboard; press **Optimize assignments**.
5. API docs: `http://localhost:8080/swagger-ui.html`

Run backend tests with `cd backend && mvn test`. Build the frontend with `cd frontend && npm run build`.

## API

### Submit lanes and quotes

`POST /api/v1/transporters/input` (201)

```json
{
  "lanes": [{"id":"Lane 1","origin":"Mumbai","destination":"Delhi"}],
  "transporters": [{"id":"T1","name":"Transporter T1","laneQuotes":[{"laneId":"Lane 1","quote":20835}]}]
}
```

Input is validated for duplicate IDs, unknown lane references, duplicate quotes from a carrier for the same lane, negative costs, and missing required fields. Each submission is stored as a separate optimization run.

### Optimize assignments

`POST /api/v1/transporters/assignment`

```json
{"maxTransporters": 3}
```

Returns the chosen carrier and quote for every lane, total quoted cost, and per-carrier summary. An exact DP search compares carrier sets and assignment decisions; it is not a greedy per-lane cheapest-quote selection.

### Read latest result

`GET /api/v1/transporters/assignment/latest`

### Error format

```json
{"error":"BAD_REQUEST","message":"Lane Lane 6 has no transporter quote and cannot be covered","timestamp":"2026-11-12T00:00:00Z"}
```

## Deploy

### Backend (Render or Railway)

- `render.yaml` provisions a free Render web service and Postgres database. In Render, choose **New → Blueprint**, connect the GitHub repository, and confirm the two free resources. For the initial deploy, set the prompted `APP_CORS_ORIGIN` to `https://*.vercel.app`; after the Vercel deployment is live, narrow it to your exact production origin (e.g. `https://your-project.vercel.app`).
- The backend Dockerfile compiles with Java 21 and starts the Spring Boot API. Render injects the database connection settings; Flyway creates the schema on startup. Keep `spring.jpa.hibernate.ddl-auto=validate` enabled.
- Render free web services spin down after 15 minutes idle, so the first request may take about a minute. Free Postgres databases expire after 30 days; this is suitable for assessment/demo use, not durable production storage.

### Frontend (Vercel)

- Import this repository and set the Vercel **Root Directory** to `frontend` (Next.js auto-detection). Leave the framework preset as Next.js.
- Add `NEXT_PUBLIC_API_URL` with the deployed backend base URL, for example `https://freightfox-api.onrender.com` (no trailing slash). Apply it to Production, Preview, and Development as needed; this value is embedded at build time, so redeploy after changing it.
- Deploy. Configure the backend `APP_CORS_ORIGIN` to this exact Vercel origin (including the production domain).

The assessment brief explicitly asks for REST APIs without a UI. The dashboard is an additional demo convenience; the Java endpoints remain independently testable with Postman and Swagger.

## Demo narrative (Loom)

1. Introduce FreightFox's freight-procurement use case and the lane quote matrix.
2. Show the sample 9 lanes / 7 carriers and the configurable carrier cap.
3. Run the optimizer and explain why the result can use fewer than the cap while still minimizing total cost.
4. Inspect assignments, carrier subtotals, total cost, and run ID.
5. Open Swagger, show the REST contracts, and mention validation plus the exact solver's JUnit coverage.
