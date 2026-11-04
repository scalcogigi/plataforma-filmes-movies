#!/bin/sh
set -eu
: "${MOVIES_DOMAIN:?Defina MOVIES_DOMAIN sem protocolo}"
: "${USERS_DOMAIN:?Defina USERS_DOMAIN sem protocolo}"
origin=${FRONTEND_ORIGIN:-https://plataforma-filmes-frontend.vercel.app}
for domain in "$MOVIES_DOMAIN" "$USERS_DOMAIN"; do
  case "$domain" in *://*|*/*) echo "Use apenas o nome DNS: $domain" >&2; exit 1;; esac
done
curl --fail --silent --show-error --max-time 20 "https://$MOVIES_DOMAIN/filmes" >/dev/null
status=$(curl --silent --show-error --max-time 20 -o /dev/null -w '%{http_code}' "https://$USERS_DOMAIN/users/me")
[ "$status" = 401 ] || { echo "Users sem token: esperado 401, recebido $status" >&2; exit 1; }
for endpoint in "https://$MOVIES_DOMAIN/filmes/1/avaliacoes/me" "https://$USERS_DOMAIN/users/me"; do
  headers=$(curl --fail --silent --show-error --max-time 20 -D - -o /dev/null -X OPTIONS \
    -H "Origin: $origin" -H 'Access-Control-Request-Method: PUT' \
    -H 'Access-Control-Request-Headers: authorization,content-type' "$endpoint")
  printf '%s\n' "$headers" | tr -d '\r' | grep -Fiqx "Access-Control-Allow-Origin: $origin" || {
    echo "Origem não autorizada em $endpoint" >&2; exit 1;
  }
done
echo 'HTTPS e preflights das duas APIs validados.'
