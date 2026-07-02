# Офлайн-репозиторий Maven

Архив `offline-m2.tgz` — готовый локальный репозиторий Maven под `backend/pom.xml`
(Spring Boot 3.2.1, Java 17). Собран и проверен: проект собирается из него без интернета.

## Как использовать

```
tar xzf offline-m2.tgz
cd backend
mvn -o -Dmaven.repo.local=../offline-m2 clean package
```

Либо влить в основной репозиторий: `cp -rn offline-m2/* ~/.m2/repository/`

Скачать сам архив: открой offline-m2.tgz и нажми Download (raw).
