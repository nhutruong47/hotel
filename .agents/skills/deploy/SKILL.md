---
description: How to deploy the hotel booking application to production
---

# Deploying the Application

## Prerequisites
- Java 21 runtime on target server
- SQL Server database accessible
- Production SMTP server for email

## Steps

1. Update `application.properties` for production:
   - Set database connection to production SQL Server
   - Configure SMTP settings for email
   - Set `spring.jpa.hibernate.ddl-auto=none` (use migration scripts)

2. Build the production JAR
```powershell
mvn clean package -DskipTests
```

3. The JAR file will be at `target/hotel-0.0.1-SNAPSHOT.jar`

4. Transfer JAR to production server

5. Run on production server
```bash
java -jar hotel-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Production Checklist
- [ ] CSRF protection enabled
- [ ] HTTPS configured
- [ ] Database backups scheduled
- [ ] Logging configured
- [ ] Monitoring setup (health endpoints)
