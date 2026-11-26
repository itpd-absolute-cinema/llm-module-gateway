#!/bin/sh
# Generates /config.js from environment variables every time the container starts,
# so the image itself contains no environment-specific addresses.
set -eu

OUT="${CONFIG_FILE:-/usr/share/nginx/html/config.js}"

fail() { echo "admin-ui: ERROR: $*" >&2; exit 1; }

# Refuses control characters (newlines etc.), then escapes the value for a JS/JSON string literal.
js_string() {
  if [ "$(printf '%s' "$2" | wc -l)" -gt 0 ] || printf '%s' "$2" | LC_ALL=C grep -q '[[:cntrl:]]'; then
    fail "$1 contains control characters or a line break"
  fi
  printf '"%s"' "$(printf '%s' "$2" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g' -e 's/</\\u003c/g' -e 's/>/\\u003e/g' -e 's/&/\\u0026/g')"
}
http_url() {
  case "$2" in http://?*|https://?*) ;; *) fail "$1 must start with http:// or https:// (got: $2)";; esac
}

KEYCLOAK_PUBLIC_URL="${KEYCLOAK_PUBLIC_URL:-}"
[ -n "$KEYCLOAK_PUBLIC_URL" ] || fail "KEYCLOAK_PUBLIC_URL is not set. Set it to the address of Keycloak as seen from the user's browser, e.g. KEYCLOAK_PUBLIC_URL=https://keycloak.example.com"
KEYCLOAK_PUBLIC_URL="${KEYCLOAK_PUBLIC_URL%/}"
http_url KEYCLOAK_PUBLIC_URL "$KEYCLOAK_PUBLIC_URL"

KEYCLOAK_REALM="${KEYCLOAK_REALM:-gateway}"
KEYCLOAK_CLIENT="${KEYCLOAK_CLIENT:-ac-gateway-admin}"
GRAFANA_PUBLIC_URL="${GRAFANA_PUBLIC_URL:-}"
[ -z "$GRAFANA_PUBLIC_URL" ] || http_url GRAFANA_PUBLIC_URL "$GRAFANA_PUBLIC_URL"
GATEWAY_PUBLIC_URL="${GATEWAY_PUBLIC_URL:-}"
[ -z "$GATEWAY_PUBLIC_URL" ] || http_url GATEWAY_PUBLIC_URL "$GATEWAY_PUBLIC_URL"
OPENCODE_CLIENT_ID="${OPENCODE_CLIENT_ID:-gateway}"
API_URL="${API_URL:-}"
PYODIDE_URL="${PYODIDE_URL:-}"

# Escape everything first: a failure inside $(...) must stop the script before anything is written.
J_URL=$(js_string KEYCLOAK_PUBLIC_URL "$KEYCLOAK_PUBLIC_URL") || exit 1
J_REALM=$(js_string KEYCLOAK_REALM "$KEYCLOAK_REALM") || exit 1
J_CLIENT=$(js_string KEYCLOAK_CLIENT "$KEYCLOAK_CLIENT") || exit 1
J_GRAFANA=$(js_string GRAFANA_PUBLIC_URL "$GRAFANA_PUBLIC_URL") || exit 1
J_GATEWAY=$(js_string GATEWAY_PUBLIC_URL "$GATEWAY_PUBLIC_URL") || exit 1
J_OCCLIENT=$(js_string OPENCODE_CLIENT_ID "$OPENCODE_CLIENT_ID") || exit 1
J_API=$(js_string API_URL "$API_URL") || exit 1
J_PYODIDE=$(js_string PYODIDE_URL "$PYODIDE_URL") || exit 1

TMP="$OUT.tmp"
# printf, not echo: some shells (dash/busybox) interpret backslashes in echo arguments
{
  printf '%s\n' "// Generated at container start from environment variables. Do not edit."
  printf '%s\n' "window.__APP_CONFIG__ = Object.freeze({"
  printf '%s\n' "  keycloakUrl: $J_URL,"
  printf '%s\n' "  keycloakRealm: $J_REALM,"
  printf '%s\n' "  keycloakClient: $J_CLIENT,"
  printf '%s\n' "  grafanaUrl: $J_GRAFANA,"
  printf '%s\n' "  gatewayUrl: $J_GATEWAY,"
  printf '%s\n' "  opencodeClientId: $J_OCCLIENT,"
  printf '%s\n' "  apiUrl: $J_API,"
  printf '%s\n' "  pyodideUrl: $J_PYODIDE"
  printf '%s\n' "});"
} > "$TMP"
mv "$TMP" "$OUT"
chmod 644 "$OUT"
printf 'admin-ui: wrote %s (keycloak=%s realm=%s client=%s)\n' "$OUT" "$KEYCLOAK_PUBLIC_URL" "$KEYCLOAK_REALM" "$KEYCLOAK_CLIENT"
