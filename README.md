# CRMConnect

Java 21 / Spring Boot multi-tenant CRM. Run `mvn spring-boot:run` (the dev URL creates `crmconnect_db` automatically with MySQL `root` / `root`), then open `http://localhost:8080`. Flyway applies migrations automatically. Import as an Existing Maven Project in STS. Swagger UI is at `http://localhost:8080/swagger-ui.html`.

## Main workflows

An administrator creates the company tenant through `POST /api/v1/auth/signup`, signs into the browser portal, adds sales team members through `POST /api/v1/users`, captures customer Connections, converts them into Deal Flow records, and schedules follow-up Touchpoints. Every request after login carries a JWT with the tenant ID, and Hibernate scopes tenant-owned records automatically.

The administrator-facing Team Management page is available at `http://localhost:8080/team.html` after login.

Set `SPRING_PROFILES_ACTIVE=prod` and provide `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` for production.
