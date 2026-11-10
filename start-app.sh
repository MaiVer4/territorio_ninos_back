#!/bin/bash
set -euo pipefail

if [ -z "${JWT_SECRET:-}" ] || [ "${#JWT_SECRET}" -lt 32 ]; then
  echo "ERROR: defina JWT_SECRET con al menos 32 caracteres. Ej: export JWT_SECRET=\$(openssl rand -base64 48)" >&2
  exit 1
fi
exec ./mvnw spring-boot:run "$@"
