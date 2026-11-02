#!/usr/bin/env bash
# End-to-end smoke test of the whole stack through the gateway.
# Usage: bash scripts/smoke-test.sh [gateway-url]
# Default: http://localhost:8080

set -u

BASE="${1:-http://localhost:8080}"
pass=0
fail=0

code() {
  curl -s -o /dev/null -w "%{http_code}" "$@"
}

check() {
  # name expected actual
  if [ "$2" = "$3" ]; then
    echo "PASS  $1 (HTTP $3)"
    pass=$((pass + 1))
  else
    echo "FAIL  $1 (expected $2, got $3)"
    fail=$((fail + 1))
  fi
}

login() {
  # username password -> prints token
  curl -s -X POST "$BASE/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}" |
    sed -nE 's/.*"token"[[:space:]]*:[[:space:]]*"([^"]+)".*/\1/p'
}

echo "Waiting for the gateway at $BASE ..."

TOKEN=""

for i in $(seq 1 40); do
  TOKEN=$(login admin admin123)
  [ -n "$TOKEN" ] && break
  sleep 5
done

if [ -z "$TOKEN" ]; then
  echo "FAIL  could not log in through the gateway"
  exit 1
fi

AUTH="Authorization: Bearer $TOKEN"

echo "Waiting for the gateway to load the route table ..."

for i in $(seq 1 40); do
  [ "$(code "$BASE/products" -H "$AUTH")" = "200" ] && break
  sleep 3
done

echo
echo "--- Security ---"

check "no token is rejected" 401 \
  "$(code "$BASE/products")"

check "invalid token is rejected" 401 \
  "$(code "$BASE/products" -H 'Authorization: Bearer not-a-real-token')"

check "wrong password is rejected" 401 \
  "$(code -X POST "$BASE/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin","password":"wrong"}')"

echo "--- Routing through the gateway ---"

for p in products orders users payments inventory; do
  check "GET /$p with a valid token" 200 \
    "$(code "$BASE/$p" -H "$AUTH")"
done

echo "--- Management API and roles ---"

# Admin can list routes
check "admin can list routes" 200 \
  "$(code "$BASE/api/routes" -H "$AUTH")"

# Create a unique normal user for every test run
CI_USER="ci-user-${GITHUB_RUN_ID:-local}-$$"
CI_PASSWORD="ci-pass-123"

# Register normal user
REGISTER_CODE=$(code -X POST "$BASE/api/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$CI_USER\",\"password\":\"$CI_PASSWORD\"}")

if [ "$REGISTER_CODE" != "200" ] && [ "$REGISTER_CODE" != "201" ]; then
  echo "FAIL  normal user registration (expected 200/201, got $REGISTER_CODE)"
  fail=$((fail + 1))
else
  echo "PASS  normal user registration (HTTP $REGISTER_CODE)"
  pass=$((pass + 1))
fi

# Login as normal user
USER_TOKEN=$(login "$CI_USER" "$CI_PASSWORD")

if [ -z "$USER_TOKEN" ]; then
  echo "FAIL  normal user login: token not received"
  fail=$((fail + 1))
else

  # Verify that the normal user's token is valid
  check "normal user token is valid" 200 \
    "$(code "$BASE/api/routes" \
      -H "Authorization: Bearer $USER_TOKEN")"

  # Verify that normal user cannot add routes
  check "normal user cannot add a route" 403 \
    "$(code -X POST "$BASE/api/routes" \
      -H "Authorization: Bearer $USER_TOKEN" \
      -H 'Content-Type: application/json' \
      -d '{"routeId":"x","path":"/x/**","uri":"http://x"}')"
fi

echo "--- Rate limiting ---"

limited=0

for i in $(seq 1 30); do
  [ "$(code "$BASE/products" -H "$AUTH")" = "429" ] && \
    limited=$((limited + 1))
done

if [ "$limited" -gt 0 ]; then
  verdict=yes
else
  verdict=no
fi

check "product route returns 429 after its limit ($limited blocked)" \
  yes "$verdict"

echo
echo "$pass passed, $fail failed"

[ "$fail" -eq 0 ]
