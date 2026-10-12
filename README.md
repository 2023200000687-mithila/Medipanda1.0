# MediPanda

Medicine delivery platform (Spring Boot 3 / Java 21, MongoDB, vanilla JS frontend).

- Roles: customer, pharmacy, rider, admin. Email OTP via Brevo, Google sign-in, SSLCommerz payments, Cloudinary uploads.
- Maps: OpenStreetMap (Leaflet) with OSRM road routing and ETA (`/api/location/route`, `/api/location/order/{id}/route`).
- Notifications: order placed, payment confirmed, medicine packed, delivered -> in-app + email (Brevo) + push (FCM) + SMS (Brevo SMS / BD gateways).

## Run locally
```bash
# needs Java 21 + MongoDB; see .env.local.example
mvn spring-boot:run   # localhost defaults, no profile needed
```
Local secrets can live in `src/main/resources/application-local.properties` (gitignored). With `DEMO_SEED_ENABLED=true` demo accounts are created (admin: `admin@medipanda.com`, password from `SEED_ADMIN_PASSWORD`, default `Admin@12345`).

## Deploy
See **DEPLOY.md** (Docker, Render/Railway) and **.env.production.example** (all values).
