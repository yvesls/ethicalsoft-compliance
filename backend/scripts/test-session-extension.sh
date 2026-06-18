#!/bin/bash

# Session Extension API Testing Script
# This script provides cURL commands to test the session extension endpoint

BASE_URL="${1:-http://localhost:8080}"
ACCESS_TOKEN="${2:-your_access_token_here}"
REFRESH_TOKEN="${3:-your_refresh_token_here}"

echo "Session Extension API Testing Script"
echo "======================================"
echo "Base URL: $BASE_URL"
echo ""

# Color codes for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Successful Extension
echo -e "${BLUE}Test 1: Successful Session Extension${NC}"
echo "Command:"
echo "curl -X POST $BASE_URL/api/auth/extend-session \\"
echo "  -H \"Authorization: Bearer $ACCESS_TOKEN\" \\"
echo "  -H \"Content-Type: application/json\" \\"
echo "  -d '{\"refreshToken\": \"$REFRESH_TOKEN\"}'"
echo ""
echo "Running..."
curl -X POST "$BASE_URL/api/auth/extend-session" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}" \
  -w "\nHTTP Status: %{http_code}\n\n"

# Test 2: Invalid Token
echo -e "${BLUE}Test 2: Invalid Refresh Token${NC}"
echo "Running..."
curl -X POST "$BASE_URL/api/auth/extend-session" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "invalid_token_xyz"}' \
  -w "\nHTTP Status: %{http_code}\n\n"

# Test 3: Missing Token
echo -e "${BLUE}Test 3: Missing Refresh Token${NC}"
echo "Running..."
curl -X POST "$BASE_URL/api/auth/extend-session" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}' \
  -w "\nHTTP Status: %{http_code}\n\n"

# Test 4: Missing Authorization
echo -e "${BLUE}Test 4: Missing Authorization Header${NC}"
echo "Running..."
curl -X POST "$BASE_URL/api/auth/extend-session" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}" \
  -w "\nHTTP Status: %{http_code}\n\n"

echo -e "${GREEN}Tests completed!${NC}"
echo ""
echo "Usage:"
echo "  ./test-session-extension.sh [BASE_URL] [ACCESS_TOKEN] [REFRESH_TOKEN]"
echo ""
echo "Examples:"
echo "  ./test-session-extension.sh"
echo "  ./test-session-extension.sh http://localhost:8080 your_token your_refresh"
