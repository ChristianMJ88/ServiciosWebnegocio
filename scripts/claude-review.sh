#!/usr/bin/env bash
set -euo pipefail
if [[ $# -ne 2 ]]; then
  echo 'Uso: bash scripts/claude-review.sh solicitud.md revision.md' >&2
  exit 2
fi
request_file="$1"
review_file="$2"
[[ -f "$request_file" ]] || { echo 'No existe la solicitud.' >&2; exit 2; }
[[ ! -e "$review_file" ]] || { echo 'La revisión ya existe; usa otro número.' >&2; exit 2; }
command -v claude >/dev/null || { echo 'Claude Code no está instalado.' >&2; exit 2; }
mkdir -p "$(dirname "$review_file")"
temporary_review="$(mktemp)"
trap 'rm -f "$temporary_review"' EXIT
claude --print --tools Read,Glob,Grep --allowedTools Read,Glob,Grep < "$request_file" > "$temporary_review"
[[ -s "$temporary_review" ]] || { echo 'Claude no produjo una revisión.' >&2; exit 1; }
cp -n "$temporary_review" "$review_file"
echo "Revisión guardada: $review_file"
