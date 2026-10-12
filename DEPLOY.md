# MediPanda – Deployment

## 1. Values you must prepare
See `.env.production.example`. There is a single `application.properties` (localhost defaults); on Render you override values with environment variables. Must set: `MONGODB_URI`, `JWT_SECRET`, `PUBLIC_BASE_URL`, `DEMO_SEED_ENABLED=false`, `SEED_ADMIN_PASSWORD`.

| Variable | Required | Where to get it |
|---|---|---|
| `MONGODB_URI` | yes | MongoDB Atlas > Connect > Drivers. Add your host's IP (or 0.0.0.0/0) in Network Access |
| `JWT_SECRET` | yes | `openssl rand -base64 48` |
| `PUBLIC_BASE_URL` | yes | your https URL, no trailing slash (SSLCommerz callbacks use it) |
| `SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD` | first deploy | creates the admin once; keep `DEMO_SEED_ENABLED=false` |
| `BREVO_ENABLED`, `BREVO_API_KEY`, `BREVO_SENDER_EMAIL`, `BREVO_SENDER_NAME` | for OTP + order emails | Brevo > SMTP & API > API keys; sender must be verified |
| `SMS_ENABLED`, `SMS_PROVIDER`, `SMS_API_KEY`, `SMS_SENDER_ID` | for SMS | `brevo` reuses the Brevo key (needs SMS credits; check Brevo supports Bangladesh + sender rules). Otherwise `alphasms` / `bulksmsbd` |
| `FCM_SERVICE_ACCOUNT_FILE` or `FCM_SERVICE_ACCOUNT_JSON` | for push | Firebase > Project settings > Service accounts |
| `FIREBASE_WEB_CONFIG_JSON`, `FIREBASE_VAPID_KEY` | for push | Firebase > Project settings > General (Web app) and Cloud Messaging |
| `OSRM_BASE_URL` | optional | default is the free public OSRM demo (light use). Self-host OSRM for heavy traffic |
| `CLOUDINARY_*` | for image uploads | Cloudinary dashboard |
| `GOOGLE_CLIENT_ID` | for Google login | Google Cloud > Credentials. Add your domain to Authorized JavaScript origins |
| `SSLCOMMERZ_STORE_ID/PASSWORD/SANDBOX` | for online payment | SSLCommerz panel. Set success/fail/cancel/IPN URLs to `PUBLIC_BASE_URL/api/payments/sslcommerz/...` |

Anything left empty simply disables that feature; the app still runs.

## 2. Deploy with Docker (VPS)
```bash
cp .env.production.example .env      # fill in values
mkdir -p secrets && cp firebase.json secrets/   # only if using push
docker compose up -d --build
docker compose logs -f app
```
Put Nginx/Caddy in front for HTTPS (push notifications and geolocation need HTTPS). Caddy example:
```
your-domain.com { reverse_proxy localhost:8081 }
```

## 3. Deploy on Render
1. Push this folder to GitHub. Render > New > **Blueprint** (uses `render.yaml`) or New > Web Service > Docker.
2. Paste the variables from `.env.production.example` in the Environment tab (use `FCM_SERVICE_ACCOUNT_JSON` instead of a file).
3. Health check path: `/api/site`.

## 4. After first deploy
1. Log in as the admin, change the password if you want.
2. Keep `DEMO_SEED_ENABLED=false`. The first start also loads a sample pharmacy/medicine catalogue when the DB is empty; remove it from the admin panel if you don't want it.
3. Test: register (OTP email), place an order (email/push/SMS), open live tracking (route line).

## 5. Security checklist
- Rotate any key that was ever shared in chat or committed (Mongo, Brevo, Cloudinary, SSLCommerz).
- Use `SSLCOMMERZ_SANDBOX=false` only with live credentials.
- Atlas Network Access: restrict to your server IP when possible.
