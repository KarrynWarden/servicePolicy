-- Диагностика контроля 111 «Код для данного типа организации указан неверно».
-- Повод: на запрос type_org=1, code_org=10 новый сервис даёт 111, старый — нет.
--
-- ВАЖНО: выполнять ПОД ПОЛЬЗОВАТЕЛЕМ СЕРВИСА (spring.datasource.username) и в той же
-- базе, к которой он подключается. Под своим логином результат может отличаться.
--
-- Как читать: каждый запрос печатает готовый вердикт, смотреть надо на него.

-- 1. Кто я и какую таблицу вижу под именем spmo.
select current_user                                as "пользователь",
       current_database()                          as "база",
       current_setting('search_path')              as "search_path",
       coalesce(to_regclass('spmo')::text, '— НЕ НАЙДЕНА —') as "какая_spmo_видна";

-- 2. Наполнен ли справочник вообще. Если строк 0 — 111 будет возникать ВСЕГДА,
--    и причина не в коде 10, а в незаполненном справочнике.
select count(*) as "всего_строк_в_spmo",
       case when count(*) = 0
            then 'СПРАВОЧНИК ПУСТ -> 111 возникает на любой код_org'
            else 'справочник заполнен' end as "вердикт"
from spmo;

-- 3. Ровно тот запрос, который выполняет сервис (контроль 111, type_org=1).
--    Если "строк_найдено" = 0 — сервис прав, ошибка 111 обоснована.
select 10 as "проверяемый_code_org",
       (select count(*) from spmo where code = cast('10' as int)) as "строк_найдено",
       case when exists (select 1 from spmo where code = cast('10' as int))
            then 'КОД ЕСТЬ -> 111 выдаётся ЗРЯ (ошибка на нашей стороне)'
            else 'КОДА НЕТ -> 111 обоснована (старый сервис был мягче)'
       end as "вердикт"
union all
select 47,
       (select count(*) from spmo where code = cast('47' as int)),
       case when exists (select 1 from spmo where code = cast('47' as int))
            then 'КОД ЕСТЬ -> 111 выдаётся ЗРЯ (ошибка на нашей стороне)'
            else 'КОДА НЕТ -> 111 обоснована (старый сервис был мягче)'
       end;

-- 4. Период актуальности на дату запроса (date1/date2 = 2023-01-01).
--    Сейчас сервис даты НЕ учитывает; здесь видно, повлияло бы это или нет.
select code, dbegin, dend,
       case when dbegin <= date '2023-01-01' and dend >= date '2023-01-01'
            then 'действует на 2023-01-01'
            else 'НЕ действует на 2023-01-01' end as "актуальность"
from spmo
where code in (10, 47)
order by code, dbegin;

-- 5. Нет ли ДРУГОГО справочника МО (в комментариях схемы упоминается SpMu)
--    и не лежит ли spmo в нескольких схемах сразу.
select n.nspname as "схема", c.relname as "таблица",
       (select count(*) from pg_attribute a
         where a.attrelid = c.oid and a.attname = 'code' and a.attnum > 0) as "есть_колонка_code"
from pg_class c
join pg_namespace n on n.oid = c.relnamespace
where c.relkind in ('r','v','m','f','p')
  and (c.relname ilike 'spmo%' or c.relname ilike 'spmu%' or c.relname ilike 'sp_mo%')
order by 1, 2;

-- 6. Перекрёстная проверка по фактическим данным: встречается ли МО 10 (и 47)
--    в реальных прикреплениях. Если нет ни одной записи — код и правда «не боевой»,
--    значит 111 по существу верна, а мягкость старого сервиса была недоработкой.
select mo as "код_МО", count(*) as "записей_в_iprkdept"
from iprkdept
where mo in (10, 47)
group by mo
order by mo;
