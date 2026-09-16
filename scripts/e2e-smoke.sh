#!/bin/sh
set -eu

# Requires the demo profile data supplied by the Compose order-service container.
BASE_URL="${BASE_URL:-http://localhost:8080}"

attempt=0
until curl -fsS "$BASE_URL/actuator/health" >/dev/null 2>&1; do
  attempt=$((attempt + 1))
  [ "$attempt" -lt 90 ] || { echo 'gateway did not become healthy in time' >&2; exit 1; }
  sleep 2
done

api_code() {
  printf '%s' "$1" | sed -n 's/.*"code":"\([^"]*\)".*/\1/p'
}

require_success() {
  name="$1"
  response="$2"
  code="$(api_code "$response")"
  if [ "$code" != "SUCCESS" ]; then
    message="$(printf '%s' "$response" | sed -n 's/.*"message":"\([^"]*\)".*/\1/p')"
    echo "$name failed: apiCode=${code:-unknown}, message=${message:-unknown}" >&2
    echo "$name response: $response" >&2
    exit 1
  fi
  echo "$name=SUCCESS"
}

request_until_success() {
  name="$1"
  shift
  attempt=0
  while :; do
    response="$(curl -sS "$@" 2>/dev/null || true)"
    if [ "$(api_code "$response")" = "SUCCESS" ]; then
      printf '%s' "$response"
      return
    fi
    attempt=$((attempt + 1))
    [ "$attempt" -lt 60 ] || {
      echo "$name did not become available: apiCode=$(api_code "$response")" >&2
      exit 1
    }
    sleep 2
  done
}

venues="$(request_until_success "venue-query" "$BASE_URL/public/venues")"
require_success "venue-query" "$venues"

visit_date="$(date -d '+2 day' +%F)"
sessions="$(request_until_success "session-query" "$BASE_URL/public/venues/1002/sessions?visitDate=$visit_date")"
require_success "session-query" "$sessions"

login="$(request_until_success "login" -H 'Content-Type: application/json' \
  -d '{"loginName":"tourist_demo","password":"123456"}' \
  "$BASE_URL/auth/login")"
require_success "login" "$login"
token="$(printf '%s' "$login" | sed -n 's/.*"data":"\([^"]*\)".*/\1/p')"
[ -n "$token" ] || { echo 'login did not return a token' >&2; exit 1; }

orders="$(request_until_success "order-service-route" -H "Authorization: Bearer $token" \
  "$BASE_URL/tourist/orders?page=1&size=1")"
require_success "order-service-route" "$orders"

coupons="$(request_until_success "coupon-service-route" -H "Authorization: Bearer $token" \
  "$BASE_URL/tourist/coupons")"
require_success "coupon-service-route" "$coupons"

# This order uses the seeded coupon for venue 1002, covering venue/coupon TCC calls.
create="$(curl -sS -H "Authorization: Bearer $token" -H 'Content-Type: application/json' \
  -d '{"sessionId":4011,"items":[{"visitorId":2201,"sessionTicketTypeId":5012}],"userCouponId":10101}' \
  "$BASE_URL/tourist/orders")"
require_success "order-create" "$create"
order_id="$(printf '%s' "$create" | sed -n 's/.*"id":\([0-9][0-9]*\).*/\1/p')"
[ -n "$order_id" ] || { echo 'order creation did not return an order id' >&2; exit 1; }
echo "order-id=$order_id"

pay="$(curl -fsS -X POST -H "Authorization: Bearer $token" "$BASE_URL/tourist/orders/$order_id/pay")"
require_success "order-pay" "$pay"

detail="$(curl -fsS -H "Authorization: Bearer $token" "$BASE_URL/tourist/orders/$order_id")"
require_success "order-query" "$detail"

refund="$(curl -sS -X POST -H "Authorization: Bearer $token" "$BASE_URL/tourist/orders/$order_id/refund")"
refund_code="$(api_code "$refund")"
echo "order-refund-api-code=${refund_code:-HTTP_ERROR}"

final_detail="$(curl -fsS -H "Authorization: Bearer $token" "$BASE_URL/tourist/orders/$order_id")"
require_success "refund-query" "$final_detail"
status="$(printf '%s' "$final_detail" | sed -n 's/.*"status":"\([^"]*\)".*/\1/p')"
echo "final-order-status=${status:-unknown}"
case "$status" in
  REFUNDING|REFUNDED) ;;
  *)
    echo 'refund request did not move the order into its refund lifecycle' >&2
    exit 1
    ;;
esac
