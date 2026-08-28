#!/bin/bash

# ============================================================
# Nhu Villas Backend Deployment Script
# ============================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}   Nhu Villas Backend Deployment${NC}"
echo -e "${GREEN}========================================${NC}"

# Check environment
check_env() {
    echo -e "\n${YELLOW}Checking environment...${NC}"
    
    if [ -z "$SPRING_DATASOURCE_URL" ]; then
        echo -e "${RED}ERROR: SPRING_DATASOURCE_URL not set${NC}"
        exit 1
    fi
    
    if [ -z "$STRIPE_SECRET_KEY" ]; then
        echo -e "${YELLOW}WARNING: STRIPE_SECRET_KEY not set - payments will use mock mode${NC}"
    else
        echo -e "${GREEN}✓ Stripe configured${NC}"
    fi
    
    if [ -z "$APP_ADMIN_PASSWORD" ]; then
        echo -e "${RED}ERROR: APP_ADMIN_PASSWORD must be set in production${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Environment check passed${NC}"
}

# Build application
build_app() {
    echo -e "\n${YELLOW}Building application...${NC}"
    
    cd "$(dirname "$0")"
    
    ./mvnw clean package -Dspring.profiles.active=prod -DskipTests
    
    if [ -f target/hotel-backend.jar ]; then
        echo -e "${GREEN}✓ Build successful${NC}"
    else
        echo -e "${RED}ERROR: Build failed${NC}"
        exit 1
    fi
}

# Run database migrations
run_migrations() {
    echo -e "\n${YELLOW}Running database migrations...${NC}"
    
    ./mvnw flyway:migrate -Dspring.profiles.active=prod
    
    echo -e "${GREEN}✓ Migrations completed${NC}"
}

# Start application
start_app() {
    echo -e "\n${YELLOW}Starting application...${NC}"
    
    # Check if already running
    if pgrep -f "hotel-backend.jar" > /dev/null; then
        echo -e "${YELLOW}Application already running. Stopping...${NC}"
        pkill -f "hotel-backend.jar"
        sleep 5
    fi
    
    # Start in background
    nohup java -jar target/hotel-backend.jar \
        --spring.profiles.active=prod \
        > logs/app.log 2>&1 &
    
    APP_PID=$!
    echo $APP_PID > .app.pid
    
    echo -e "${GREEN}✓ Application started (PID: $APP_PID)${NC}"
    
    # Wait for startup
    echo -e "${YELLOW}Waiting for application to start...${NC}"
    sleep 10
    
    # Health check
    for i in {1..30}; do
        if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
            echo -e "${GREEN}✓ Application is healthy${NC}"
            return 0
        fi
        sleep 2
    done
    
    echo -e "${RED}ERROR: Application failed to start${NC}"
    cat logs/app.log
    exit 1
}

# Deploy function
deploy() {
    check_env
    build_app
    run_migrations
    start_app
    
    echo -e "\n${GREEN}========================================${NC}"
    echo -e "${GREEN}   Deployment Complete!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo -e "Logs: logs/app.log"
    echo -e "PID: $(cat .app.pid)"
}

# Stop function
stop() {
    echo -e "${YELLOW}Stopping application...${NC}"
    
    if [ -f .app.pid ]; then
        PID=$(cat .app.pid)
        if ps -p $PID > /dev/null 2>&1; then
            kill $PID
            rm .app.pid
            echo -e "${GREEN}✓ Application stopped${NC}"
        else
            echo -e "${YELLOW}Application not running${NC}"
        fi
    else
        pkill -f "hotel-backend.jar" 2>/dev/null || true
        echo -e "${GREEN}✓ Application stopped${NC}"
    fi
}

# Restart function
restart() {
    stop
    sleep 3
    start_app
}

# Show logs
logs() {
    if [ -f logs/app.log ]; then
        tail -f logs/app.log
    else
        echo -e "${RED}No logs found${NC}"
    fi
}

# Show status
status() {
    if pgrep -f "hotel-backend.jar" > /dev/null; then
        PID=$(pgrep -f "hotel-backend.jar")
        echo -e "${GREEN}✓ Application running (PID: $PID)${NC}"
        
        # Health status
        if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
            echo -e "${GREEN}✓ Health check passed${NC}"
        else
            echo -e "${RED}✗ Health check failed${NC}"
        fi
    else
        echo -e "${RED}✗ Application not running${NC}"
    fi
}

# Main
case "${1:-deploy}" in
    deploy)
        deploy
        ;;
    start)
        start_app
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    logs)
        logs
        ;;
    status)
        status
        ;;
    migrate)
        run_migrations
        ;;
    *)
        echo "Usage: $0 {deploy|start|stop|restart|logs|status|migrate}"
        exit 1
        ;;
esac
