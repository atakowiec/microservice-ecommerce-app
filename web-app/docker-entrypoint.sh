#!/bin/sh
set -eu

export BACKEND_URL="${BACKEND_URL:-http://localhost:8000}"
envsubst '${BACKEND_URL}' \
  < /usr/share/nginx/html/runtime-env.template.js \
  > /tmp/env.js

exec nginx -c /etc/nginx/nginx.conf -g 'daemon off;'
