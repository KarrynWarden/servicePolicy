# Как тестировать сервис GetInsPrkState (SOAP)

Это **SOAP**-сервис (как старый `.asmx`), а не REST — поэтому Swagger не подходит
(он документирует REST-контроллеры, которых здесь нет). «Документация» SOAP — это
**WSDL**, а вызывать метод удобно через SoapUI / Postman / curl.

Базовые адреса (по умолчанию порт 8080):
- Эндпоинт (сюда шлём запросы): `http://localhost:8080/ws`
- WSDL (контракт — что принимает/отдаёт): `http://localhost:8080/ws/inscheck.wsdl`

## 1. Посмотреть контракт
Открой в браузере `http://localhost:8080/ws/inscheck.wsdl` — увидишь описание
операции `GetInsPrkState`, структуру `query` (вход) и `answer` (выход).
Браузер только показывает контракт, вызвать метод из него нельзя.

## 2. Дымовой тест без базы (сервис жив?)
Структурная проверка входа идёт до обращения к БД, поэтому этот тест работает,
даже если PostgreSQL не поднят:
```bash
curl -H 'Content-Type: text/xml; charset=utf-8' \
     --data-binary @docs/soap-examples/request-smoke.xml \
     http://localhost:8080/ws
```
Ожидаемый ответ (nrec пустой → ошибка 4):
```xml
<answer><err><errcode>4</errcode>
  <errtext>Не все обязательные поля заполнены: nrec</errtext></err></answer>
```

## 3. Полный запрос (нужна поднятая БД с данными)
```bash
curl -H 'Content-Type: text/xml; charset=utf-8' \
     --data-binary @docs/soap-examples/request-full.xml \
     http://localhost:8080/ws
```
В ответе — `ack`, найденные `<err>`, `<alg>`, блок `<ins>` (страховая
принадлежность), `<prk>` (прикрепление) и признаки `p_disp/p_proph/p_healthc`.
Поля запроса можно менять прямо в `docs/soap-examples/request-full.xml`.

## 4. SoapUI (ближе всего к «потыкать как в Swagger»)
1. File → New SOAP Project.
2. Initial WSDL: `http://localhost:8080/ws/inscheck.wsdl` → OK.
3. Слева раскрой `GetInsPrkState` → Request 1 — SoapUI сам сгенерит шаблон запроса.
4. Заполни поля внутри `<query>` и нажми ▶ (Submit) — справа увидишь ответ.

## 5. Postman
Метод POST, URL `http://localhost:8080/ws`, Body → raw → XML, вставить содержимое
`request-full.xml`. Header `Content-Type: text/xml; charset=utf-8`.

## Подключение к базе
Для реальных ответов нужна PostgreSQL с таблицами (iperson, iprkdept, medree_prdisp,
spmo/spsmo, spdocper) и данными. Строка подключения — через переменные окружения:
```bash
DB_URL=jdbc:postgresql://<хост>:5432/<база> DB_USER=<user> DB_PASSWORD=<pass> \
  java -jar target/inscheck-2.0.0.jar
```
Схема таблиц: `backend/src/main/resources/db/schema.sql` и миграция
`backend/src/main/resources/db/migration_iprkdept_medree_prdisp.sql`.
