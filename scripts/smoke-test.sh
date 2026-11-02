#!/usr/bin/env bash
# End-to-end smoke test of the whole stack through the gateway.
# Usage: bash scripts/smoke-test.sh [gateway-url]     (default http://localhost:8080)
set -u
BASE="${1:-http://localhost:8080}"
pass=0; fail=0

code() { curl -s -o /dev/null -w "%{http_code}" "$@"; }
check() {  # name expected actual
  if [ "$2" = "$3" ]; then echo "PASS  $1 (HTTP $3)"; pass=$((pass+1))
  else echo "FAIL  $1 (expected $2, got $3)"; fail=$((fail+1)); fi
}
login() {  # username password -> prints token
  curl -s -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p'
}

echo "Waiting for the gateway at $BASE ..."
TOKEN=""
for i in $(seq 1 40); do
  TOKEN=$(login admin admin123)
  [ -n "$TOKEN" ] && break
  sleep 5
done
if [ -z "$TOKEN" ]; then echo "FAIL  could not log in through the gateway"; exit 1; fi
AUTH="Authorization: Bearer $TOKEN"

echo "Waiting for the gateway to load the route table ..."
for i in $(seq 1 40); do
  [ "$(code "$BASE/products" -H "$AUTH")" = "200" ] && break
  sleep 3
done

echo
echo "--- Security ---"
check "no token is rejected"          401 "$(code "$BASE/products")"
check "invalid token is rejected"     401 "$(code "$BASE/products" -H 'Authorization: Bearer not-a-real-token')"
check "wrong password is rejected"    401 "$(code -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' -d '{"username":"admin","password":"wrong"}')"

echo "--- Routing through the gateway ---"
for p in products orders users payments inventory; do
  check "GET /$p with a valid token" 200 "$(code "$BASE/$p" -H "$AUTH")"
done

echo "--- Management API and roles ---"
check "admin can list routes" 200 "$(code "$BASE/api/routes" -H "$AUTH")"
curl -s -o /dev/null -X POST "$BASE/api/auth/register" -H 'Content-Type: application/json' -d '{"username":"ci-user","password":"ci-pass-123"}'
USER_TOKEN=$(login ci-user ci-pass-123)
check "normal user cannot add a route" 403 "$(code -X POST "$BASE/api/routes" -H "Authorization: Bearer $USER_TOKEN" -H 'Content-Type: application/json' -d '{"routeId":"x","path":"/x/**","uri":"http://x"}')"

echo "--- Rate limiting ---"
limited=0
for i in $(seq 1 30); do
  [ "$(code "$BASE/products" -H "$AUTH")" = "429" ] && limited=$((limited+1))
done
if [ "$limited" -gt 0 ]; then verdict=yes; else verdict=no; fi
check "product route returns 429 after its limit ($limited blocked)" yes "$verdict"

echo
echo "$pass passed, $fail failed"
[ "$fail" -eq 0 ]
