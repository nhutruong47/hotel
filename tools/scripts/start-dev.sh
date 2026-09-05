#!/bin/bash
# ============================================================
# Nhu Villas - Quick Start Script (Linux/Mac)
# ============================================================

set -e

echo ""
echo "============================================================"
echo "   Nhu Villas - Development Setup"
echo "============================================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check Docker
echo -e "${YELLOW}[1/4]${NC} Checking Docker..."
if ! command -v docker &> /dev/null; then
    echo -e "${RED}ERROR: Docker is not installed${NC}"
    exit 1
fi

if ! docker ps &> /dev/null; then
    echo -e "${RED}ERROR: Docker daemon is not running${NC}"
    echo "Please start Docker Desktop and wait for it to be ready."
    exit 1
fi
echo -e "${GREEN}OK:${NC} Docker is running"

# Check .env file
echo ""
echo -e "${YELLOW}[2/4]${NC} Checking configuration..."
if [ ! -f ".env" ]; then
    echo "WARNING: .env file not found. Creating from template..."
    cp ".env.production" ".env"
fi
echo -e "${GREEN}OK:${NC} Configuration loaded"

# Build and start
echo ""
echo -e "${YELLOW}[3/4]${NC} Building and starting services..."
echo "This may take a few minutes on first run..."
echo ""

docker-compose up -d --build

if [ $? -ne 0 ]; then
    echo ""
    echo -e "${RED}ERROR: Failed to start services${NC}"
    echo "Check logs with: docker-compose logs"
    exit 1
fi

# Wait for services
echo ""
echo -e "${YELLOW}[4/4]${NC} Waiting for services to be ready..."

# Check backend health
max_attempts=60
attempt=0

while [ $attempt -lt $max_attempts ]; do
    attempt=$((attempt + 1))
    
    if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}OK:${NC} Backend is ready!"
        break
    fi
    
    if [ $((attempt % 10)) -eq 0 ]; then
        echo "Waiting for backend... ($attempt/$max_attempts)"
    fi
    
    sleep 2
done

if [ $attempt -eq $max_attempts ]; then
    echo -e "${RED}ERROR: Backend failed to start after ${max_attempts} seconds${NC}"
    echo "Check logs with: docker-compose logs backend"
    exit 1
fi

# Final status
echo ""
echo "============================================================"
echo -e "   ${GREEN}All services started successfully!${NC}"
echo "============================================================"
echo ""
echo "   Frontend:   http://localhost:3000"
echo "   Backend:    http://localhost:8080"
echo "   API:        http://localhost:8080/api/v1"
echo "   H2 Console: http://localhost:8080/h2-console"
echo ""
echo "   Admin Login: admin / Admin123!"
echo ""
echo "   To view logs: docker-compose logs -f"
echo "   To stop:      docker-compose down"
echo "   To restart:    docker-compose restart"
echo ""
echo "============================================================"
echo ""
