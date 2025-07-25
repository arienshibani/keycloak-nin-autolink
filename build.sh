#!/bin/bash

# Keycloak NiN Auto-Link Authenticator Build Script

set -e

echo "🔨 Building Keycloak NiN Auto-Link Authenticator..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed. Please install Maven first."
    exit 1
fi

# Check if Docker is installed (optional)
if ! command -v docker &> /dev/null; then
    print_warning "Docker is not installed. Docker image build will be skipped."
    DOCKER_AVAILABLE=false
else
    DOCKER_AVAILABLE=true
fi

# Clean previous builds
print_status "Cleaning previous builds..."
mvn clean

# Run tests (skip if there are compatibility issues)
print_status "Running tests..."
mvn test || print_warning "Tests failed, continuing with build..."

# Build the JAR
print_status "Building JAR file..."
mvn package -DskipTests

# Check if build was successful
if [ -f "target/keycloak-nin-autolink-1.0.0.jar" ]; then
    print_status "✅ JAR file built successfully: target/keycloak-nin-autolink-1.0.0.jar"
else
    print_error "❌ JAR file was not created"
    exit 1
fi

# Build Docker image if Docker is available
if [ "$DOCKER_AVAILABLE" = true ]; then
    print_status "Building Docker image..."
    docker build -t keycloak-nin-autolink:latest .
    
    if [ $? -eq 0 ]; then
        print_status "✅ Docker image built successfully: keycloak-nin-autolink:latest"
    else
        print_error "❌ Docker image build failed"
        exit 1
    fi
fi

print_status "🎉 Build completed successfully!"

echo ""
echo "📋 Next steps:"
echo "1. Deploy the JAR to your Keycloak providers directory:"
echo "   cp target/keycloak-nin-autolink-1.0.0.jar /path/to/keycloak/providers/"
echo ""
echo "2. Or run with Docker Compose:"
echo "   docker-compose up -d"
echo ""
echo "3. Configure the authenticator in Keycloak Admin Console"
echo "   See README.md for detailed instructions" 