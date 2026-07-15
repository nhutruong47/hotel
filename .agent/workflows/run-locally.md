---
description: How to run the hotel booking application locally
---

# Running the Application Locally

// turbo-all

## Prerequisites
- Java 21 installed
- SQL Server running with database `HotelDB`
- Maven installed

## Steps

1. Ensure SQL Server is running and `HotelDB` database exists
```powershell
sqlcmd -S localhost -U sa -P 12345 -Q "CREATE DATABASE HotelDB"
```

2. Navigate to project directory
```powershell
cd d:\sab\hotel
```

// turbo
3. Clean and compile the project
```powershell
mvn clean compile
```

// turbo
4. Run the application
```powershell
mvn spring-boot:run
```

5. Access the application at http://localhost:8080

## Default Accounts
- Admin: username `admin`, password `admin123`
- User: Register via the application
