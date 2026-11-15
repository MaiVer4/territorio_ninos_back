#!/usr/bin/env bash
# Detecta qué docs de contexto (docs/context/*.md) quedaron desactualizados
# respecto al código. Cada doc registra en su cabecera el commit con el que se
# sincronizó ("commit <sha>"); el script lista los archivos vigilados que cambiaron
# desde ese commit (más los cambios sin commitear).
#
# Uso:  ./scripts/check-context-drift.sh            (backend + frontend hermano)
#       FRONT_DIR=/ruta/front ./scripts/check-context-drift.sh
# Salida: código 0 si todo está al día, 1 si algún doc necesita actualización.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")" && pwd)"
BACK_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
FRONT_DIR="${FRONT_DIR:-$BACK_DIR/../territorio-front}"
PKG="src/main/java/com/software/territoriodeninos"
drift=0

check() { # repo doc pathspec...
  local repo="$1" doc="$2"; shift 2
  local file="$repo/docs/context/$doc"
  if [[ ! -f "$file" ]]; then echo "⚠️  $doc: no existe ($file)"; drift=1; return; fi
  local sha
  sha=$(grep -m1 -oE 'commit [0-9a-f]{7,40}' "$file" | awk '{print $2}')
  if [[ -z "$sha" ]] || ! git -C "$repo" cat-file -e "$sha^{commit}" 2>/dev/null; then
    echo "⚠️  $doc: cabecera sin commit válido → revisar completo"; drift=1; return
  fi
  local changed
  changed=$( { git -C "$repo" diff --name-only "$sha" HEAD -- "$@"
               git -C "$repo" status --porcelain -- "$@" | awk '{print $NF}'; } | sort -u)
  if [[ -n "$changed" ]]; then
    echo "🔄 $doc (sincronizado en $sha) — cambiaron:"
    echo "$changed" | sed 's/^/     - /'
    drift=1
  else
    echo "✅ $doc al día ($sha)"
  fi
}

echo "== Backend: $BACK_DIR"
check "$BACK_DIR" DATABASE_SCHEMA.md "$PKG/entity" "$PKG/util" "src/main/resources/db/migration" \
      "$PKG/config/PostgresConstraintInitializer.java"
check "$BACK_DIR" API_CONTRACTS.md "$PKG/controller" "$PKG/dto" "$PKG/exception" "$PKG/security" \
      "$PKG/config/SecurityConfig.java" "$PKG/config/WebSocketConfig.java"
check "$BACK_DIR" BACKEND_MAP.md "$PKG/service" "$PKG/repository" "$PKG/mapper" "$PKG/config" \
      "src/main/resources/application.properties" "pom.xml" "Dockerfile"

if [[ -d "$FRONT_DIR/.git" ]]; then
  echo "== Frontend: $FRONT_DIR"
  check "$FRONT_DIR" FRONTEND_MAP.md "src/app/app.routes.ts" "src/app/app.config.ts" "src/app/core" \
        "src/app/features" "src/app/shared" "src/environments" "package.json" "angular.json"
else
  echo "ℹ️  Frontend no encontrado en $FRONT_DIR (usa FRONT_DIR=...)"
fi

exit $drift
