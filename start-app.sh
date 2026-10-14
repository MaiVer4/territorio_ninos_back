#!/bin/bash
if [ -z "$JWT_SECRET" ]; then
  echo "ERROR: defina JWT_SECRET (mínimo 32 caracteres). Ej: export JWT_SECRET=\$(openssl rand -base64 48)" >&2
  exit 1
fi
mvn spring-boot:run
