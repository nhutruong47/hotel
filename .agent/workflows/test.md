---
description: How to run tests for the hotel booking application
---

# Running Tests

// turbo-all

## Unit Tests

// turbo
1. Run all unit tests
```powershell
mvn test
```

// turbo
2. Run specific test class
```powershell
mvn test -Dtest=BookingServiceTest
```

// turbo
3. Run tests with coverage report
```powershell
mvn test jacoco:report
```
Coverage report will be at `target/site/jacoco/index.html`

## Integration Tests

// turbo
1. Run integration tests (requires database)
```powershell
mvn verify -P integration-tests
```

## Browser Tests

1. Start the application
2. Open browser to http://localhost:8080
3. Test login flow with admin/admin123
4. Test booking workflow
5. Test admin dashboard
