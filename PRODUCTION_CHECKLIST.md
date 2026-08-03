# ============================================================
# PRODUCTION DEPLOYMENT CHECKLIST
# ============================================================

## Quick Start Commands

### Option 1: Docker Compose (Recommended)
```bash
# 1. Copy environment file
cp .env.production .env

# 2. Edit .env with your values
# IMPORTANT: Change passwords and API keys

# 3. Start all services
docker-compose up -d --build

# 4. Check status
docker-compose ps

# 5. View logs
docker-compose logs -f backend
```

### Option 2: Quick Start Script
```bash
# Windows
start-dev.bat

# Linux/Mac
chmod +x start-dev.sh && ./start-dev.sh
```

### Option 3: Manual
```bash
# Start database
docker-compose up -d db

# Wait for DB, then start backend
docker-compose up -d backend

# Start frontend
docker-compose up -d frontend
```

### Backend (hotel/)
```bash
# Database
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=postgresql://your-db-host:5432/hotel
SPRING_DATASOURCE_USERNAME=hotel_user
SPRING_DATASOURCE_PASSWORD=your-secure-password

# Admin Credentials (REQUIRED - app will not start without)
APP_ADMIN_USERNAME=admin
APP_ADMIN_PASSWORD=YourStrongP@ssw0rd123!  # Min 12 chars, uppercase, lowercase, digit, special

# Payment Gateway (Stripe)
STRIPE_SECRET_KEY=<STRIPE_SECRET_KEY>
STRIPE_WEBHOOK_SECRET=<STRIPE_WEBHOOK_SECRET>
STRIPE_ENABLED=true

# Email (SMTP)
SMTP_HOST=smtp.sendgrid.net
SMTP_PORT=587
SMTP_USERNAME=apikey
SMTP_PASSWORD=SG.xxxxxxxxxxxxxxxxxxxxxxx
APP_EMAIL_ENABLED=true

# CORS (Your production domain)
APP_CORS_ALLOWED_ORIGINS=https://yourdomain.com

# Session Security (HTTPS required)
SESSION_COOKIE_SECURE=true
```

### Frontend (frontend/)
```bash
# Stripe Publishable Key
NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=pk_live_xxxxxxxxxxxxxxxxxxxx

# API Base URL
NEXT_PUBLIC_API_URL=https://api.yourdomain.com
```

## 2. Database Setup

### Create PostgreSQL Database
```sql
CREATE DATABASE hotel;
CREATE USER hotel_user WITH PASSWORD 'your-secure-password';
GRANT ALL PRIVILEGES ON DATABASE hotel TO hotel_user;
```

### Run Migrations
```bash
cd hotel
./mvnw flyway:migrate -Dspring.profiles.active=prod
```

## 3. Stripe Setup

### 3.1 Get API Keys
1. Go to https://dashboard.stripe.com/apikeys
2. Copy your Live Secret Key (sk_live_...)
3. Copy your Live Publishable Key (pk_live_...)

### 3.2 Setup Webhook
1. Go to https://dashboard.stripe.com/webhooks
2. Add endpoint: `https://api.yourdomain.com/api/v1/payments/webhook/stripe`
3. Select events:
   - `checkout.session.completed`
   - `payment_intent.succeeded`
   - `payment_intent.payment_failed`
   - `charge.refunded`
4. Copy the signing secret (whsec_...)

### 3.3 Production Considerations
- Use Stripe Radar for fraud protection
- Enable 3D Secure for additional security
- Set up Stripe Sigma for reporting

## 4. Email Setup

### Option A: SendGrid (Recommended)
1. Sign up at https://sendgrid.com
2. Create API Key with Mail Send permissions
3. Use these settings:
   ```
   SMTP_HOST=smtp.sendgrid.net
   SMTP_PORT=587
   SMTP_USERNAME=apikey
   SMTP_PASSWORD=SG.your-api-key
   ```

### Option B: Amazon SES
1. Verify domain in SES
2. Create SMTP credentials
3. Use provided settings

### Option C: Gmail (Not Recommended for Production)
1. Enable 2-Factor Authentication
2. Create App Password
3. Note: Gmail has sending limits

## 5. Build & Deploy

### Backend
```bash
cd hotel
./mvnw clean package -Dspring.profiles.active=prod
java -jar target/hotel-backend.jar
```

### Frontend
```bash
cd frontend
npm install
npm run build
npm start
```

### Docker (Optional)
```bash
# Build image
docker build -t nhu-villas-backend ./hotel
docker build -t nhu-villas-frontend ./frontend

# Run with docker-compose
docker-compose up -d
```

## 6. Security Checklist

- [ ] Admin password changed from default
- [ ] Database password is strong and unique
- [ ] Stripe keys are live keys (not test)
- [ ] Email credentials are production-ready
- [ ] CORS restricted to your domain only
- [ ] HTTPS enforced (redirect HTTP to HTTPS)
- [ ] Session cookies are secure and HTTP-only
- [ ] HSTS enabled
- [ ] Error messages don't expose internal details
- [ ] Rate limiting is configured

## 7. Monitoring Setup

### Health Check
```
GET https://api.yourdomain.com/actuator/health
```

### Metrics (Optional - add Prometheus/Grafana)
```bash
management.endpoints.web.exposure.include=health,info,metrics,prometheus
```

## 8. Troubleshooting

### Email not sending
1. Check SMTP credentials
2. Verify email addresses are valid
3. Check spam folder
4. Review logs for errors

### Payment failures
1. Verify Stripe keys are correct
2. Check webhook is configured
3. Review Stripe Dashboard for error logs

### Database connection issues
1. Verify database is accessible
2. Check connection pool settings
3. Ensure migrations ran successfully

## 9. Support

For issues, check:
- Application logs
- Stripe Dashboard logs
- Database connection logs
- Email provider delivery reports
