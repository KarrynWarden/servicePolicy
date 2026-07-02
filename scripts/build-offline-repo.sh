#!/usr/bin/env bash
# Собирает ПЕРЕНОСИМЫЙ офлайн-репозиторий Maven под проект backend/ на ПК С интернетом.
# Результат (offline-m2.tgz) переносится флешкой на офлайн-ПК.
#
# Запускать из корня репозитория на машине с интернетом:
#   bash scripts/build-offline-repo.sh
#
# На офлайн-ПК:
#   tar xzf offline-m2.tgz                       # появится каталог offline-m2
#   cd backend
#   mvn -o -Dmaven.repo.local=../offline-m2 clean package
#   # либо влить в основной репозиторий:  cp -rn offline-m2/* ~/.m2/repository/
set -euo pipefail
HERE="$(cd "$(dirname "$0")/.." && pwd)"
REPO="$HERE/offline-m2"
cd "$HERE/backend"

rm -rf "$REPO"
# Полная сборка в изолированный репозиторий тянет и зависимости, и плагины.
mvn -Dmaven.repo.local="$REPO" -DskipTests clean package
mvn -Dmaven.repo.local="$REPO" dependency:go-offline

# Проверка: собирается ли проект из этого репозитория БЕЗ сети.
mvn -o -Dmaven.repo.local="$REPO" -DskipTests clean package

cd "$HERE"
tar czf offline-m2.tgz -C "$HERE" offline-m2
echo
echo "Готово: $HERE/offline-m2.tgz ($(du -h offline-m2.tgz | cut -f1))"
echo "Перенеси на офлайн-ПК и распакуй рядом с проектом, затем:"
echo "  cd backend && mvn -o -Dmaven.repo.local=../offline-m2 clean package"
