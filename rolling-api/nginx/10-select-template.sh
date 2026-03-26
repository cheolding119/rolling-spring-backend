#!/bin/sh
set -eu

CERT_DIR="/etc/letsencrypt/live/${SERVER_NAME}"
TARGET="/etc/nginx/templates/default.conf.template"
HTTP_TEMPLATE="/opt/nginx-templates/default-http.conf.template"
HTTPS_TEMPLATE="/opt/nginx-templates/default-https.conf.template"

if [ -f "${CERT_DIR}/fullchain.pem" ] && [ -f "${CERT_DIR}/privkey.pem" ]; then
  cp "${HTTPS_TEMPLATE}" "${TARGET}"
  echo "nginx template: HTTPS"
else
  cp "${HTTP_TEMPLATE}" "${TARGET}"
  echo "nginx template: HTTP"
fi
