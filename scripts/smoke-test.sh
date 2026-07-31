#!/usr/bin/env bash
# ============================================================================
# File: scripts/smoke-test.sh
# TICKET-ADV153 — End-to-end smoke test for the full 7-service stack
# Run from repo root: bash scripts/smoke-test.sh
# Requires: docker, docker compose, curl, and jq (or py/python/node for JSON parsing)
# ============================================================================
set -euo pipefail

TRADE_REF="SMK-20260730-0001"
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
PROM_URL="${PROM_URL:-http://localhost:9090}"
GRAFANA_URL="${GRAFANA_URL:-http://localhost:3000}"

# Resolve a working JSON parser (jq preferred; Windows Git Bash often lacks jq)
JSON_TOOL=""
if command -v jq >/dev/null 2>&1; then
  JSON_TOOL="jq"
elif command -v py >/dev/null 2>&1 && py -3 -c "import json" >/dev/null 2>&1; then
  JSON_TOOL="py"
elif command -v python >/dev/null 2>&1 && python -c "import json" >/dev/null 2>&1; then
  JSON_TOOL="python"
elif command -v python3 >/dev/null 2>&1 && python3 -c "import json" >/dev/null 2>&1; then
  JSON_TOOL="python3"
elif command -v node >/dev/null 2>&1; then
  JSON_TOOL="node"
else
  echo "[step 0] jq, python, py, or node required for JSON parsing FAILED"
  exit 1
fi

fail() {
  echo "[step $1] $2 FAILED"
  exit 1
}

wait_healthy() {
  local step="$1"
  local container="$2"
  local attempts="$3"
  local sleep_secs="$4"
  local status="starting"
  for _ in $(seq 1 "$attempts"); do
    status=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo starting)
    [[ "$status" == "healthy" ]] && return 0
    sleep "$sleep_secs"
  done
  fail "$step" "$container not healthy (last status: $status)"
}

extract_field() {
  local key="$1"
  case "$JSON_TOOL" in
    jq) jq -r ".${key}" ;;
    py) py -3 -c "import json,sys; print(json.load(sys.stdin)['${key}'])" ;;
    python|python3)
      "$JSON_TOOL" -c "import json,sys; print(json.load(sys.stdin)['${key}'])"
      ;;
    node)
      node -e "let d='';process.stdin.on('data',c=>d+=c);process.stdin.on('end',()=>{const j=JSON.parse(d);process.stdout.write(String(j['${key}']))})"
      ;;
  esac
}

assert_prom_up() {
  case "$JSON_TOOL" in
    jq) jq -e '.data.result[0].value[1]=="1"' >/dev/null ;;
    py) py -3 -c "import json,sys; d=json.load(sys.stdin); assert d['data']['result'][0]['value'][1]=='1'" ;;
    python|python3)
      "$JSON_TOOL" -c "import json,sys; d=json.load(sys.stdin); assert d['data']['result'][0]['value'][1]=='1'"
      ;;
    node)
      node -e "let d='';process.stdin.on('data',c=>d+=c);process.stdin.on('end',()=>{const j=JSON.parse(d);if(j.data.result[0].value[1]!=='1')process.exit(1)})"
      ;;
  esac
}

assert_grafana_uid() {
  case "$JSON_TOOL" in
    jq) jq -e '.uid=="reconx-prometheus"' >/dev/null ;;
    py) py -3 -c "import json,sys; d=json.load(sys.stdin); assert d.get('uid')=='reconx-prometheus'" ;;
    python|python3)
      "$JSON_TOOL" -c "import json,sys; d=json.load(sys.stdin); assert d.get('uid')=='reconx-prometheus'"
      ;;
    node)
      node -e "let d='';process.stdin.on('data',c=>d+=c);process.stdin.on('end',()=>{if(JSON.parse(d).uid!=='reconx-prometheus')process.exit(1)})"
      ;;
  esac
}

echo "▶ 1/7  Bringing the stack up..."
docker compose down -v >/dev/null 2>&1 || true
docker compose up -d --build
echo "  Waiting up to 90s for backend to be healthy..."
wait_healthy 1 reconx-backend 18 5
echo "  ✓ backend healthy"

echo "▶ 2/7  Logging in as trader..."
TOKEN=$(curl -fsS -X POST "${BACKEND_URL}/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"trader@db.com","password":"trader123"}' | extract_field token)
[[ -n "$TOKEN" && "$TOKEN" != "null" ]] || fail 2 "login (missing JWT token)"
echo "  ✓ JWT acquired"

echo "▶ 3/7  Posting a trade..."
TRADE=$(curl -fsS -X POST "${BACKEND_URL}/api/v1/trades" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d "{\"tradeRef\":\"${TRADE_REF}\",\"instrumentId\":1,\"counterpartyId\":1,\"assetClass\":\"EQUITY\",\"side\":\"BUY\",\"quantity\":100,\"price\":245.5,\"tradeDate\":\"2026-06-02\"}")
TRADE_ID=$(echo "$TRADE" | extract_field id)
[[ -n "$TRADE_ID" && "$TRADE_ID" != "null" ]] || fail 3 "trade POST (missing id)"
echo "  ✓ trade created: id=${TRADE_ID}"

echo "▶ 4/7  Confirming Kafka event..."
KAFKA_OK=false
for _ in $(seq 1 6); do
  if docker exec reconx-kafka kafka-console-consumer \
      --bootstrap-server localhost:9092 \
      --topic trade-events \
      --from-beginning \
      --max-messages 20 \
      --timeout-ms 10000 2>/dev/null | grep -q "${TRADE_REF}"; then
    KAFKA_OK=true
    break
  fi
  sleep 3
done
[[ "$KAFKA_OK" == "true" ]] || fail 4 "Kafka trade-events (no message for ${TRADE_REF})"
echo "  ✓ trade-event found on topic"

echo "▶ 5/7  Confirming Postgres audit row..."
AUDIT_COUNT="0"
for _ in $(seq 1 10); do
  AUDIT_COUNT=$(docker exec reconx-postgres psql -U reconx_user -d reconx -tAc \
    "SELECT COUNT(*) FROM audit_log WHERE trade_ref='${TRADE_REF}';" | tr -d '[:space:]')
  [[ "$AUDIT_COUNT" != "0" && -n "$AUDIT_COUNT" ]] && break
  sleep 2
done
[[ "$AUDIT_COUNT" != "0" && -n "$AUDIT_COUNT" ]] || fail 5 "Postgres audit_log (no row for ${TRADE_REF})"
echo "  ✓ audit row present (count=${AUDIT_COUNT})"

echo "▶ 6/7  Confirming Prometheus scrape..."
wait_healthy 6 reconx-prometheus 12 5
PROM_QUERY='up{job="reconx-backend"}'
if ! curl -fsS -G "${PROM_URL}/api/v1/query" --data-urlencode "query=${PROM_QUERY}" | assert_prom_up; then
  fail 6 "Prometheus target DOWN for job reconx-backend"
fi
echo "  ✓ Prometheus scraping backend"

echo "▶ 7/7  Confirming Grafana datasource..."
wait_healthy 7 reconx-grafana 12 5
if ! curl -fsS -u admin:admin "${GRAFANA_URL}/api/datasources/uid/reconx-prometheus" | assert_grafana_uid; then
  fail 7 "Grafana datasource reconx-prometheus"
fi
echo "  ✓ Grafana datasource provisioned"

echo
echo "✅  All 7 checks green — stack is demo-ready."
