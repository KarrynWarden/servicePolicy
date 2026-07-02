#!/usr/bin/env bash
# Инвентаризация локального Maven-репозитория (~/.m2/repository).
# Выводит список артефактов group:artifact:version, которые уже есть на ПК,
# с пометкой jar/pom-only. Результат можно переслать для сверки с нужными.
#
# Использование:
#   bash scripts/m2-inventory.sh [путь_к_репозиторию] > m2-inventory.txt
set -euo pipefail
REPO="${1:-$HOME/.m2/repository}"
if [ ! -d "$REPO" ]; then
    echo "Не найден репозиторий: $REPO" >&2
    exit 1
fi
find "$REPO" -type f -name '*.pom' | while read -r pom; do
    dir="$(dirname "$pom")"
    rel="${dir#"$REPO"/}"
    version="$(basename "$rel")"
    artifact="$(basename "$(dirname "$rel")")"
    group="$(dirname "$(dirname "$rel")")"
    group="${group//\//.}"
    if ls "$dir"/*.jar >/dev/null 2>&1; then kind=jar; else kind=pom-only; fi
    echo "$group:$artifact:$version ($kind)"
done | sort -u
