-- Права, необходимые сервису InsCheckR (GetInsPrkState).
-- Замени testuser на реального пользователя, под которым сервис ходит в БД
-- (значение spring.datasource.username из application-test.properties).
--
-- Запускать под владельцем таблиц (или суперпользователем) в ТОЙ ЖЕ базе,
-- к которой подключается сервис.

-- 1. Доступ к самой базе и схеме.
--    Без USAGE на схему таблицы «не видны» даже при выданном SELECT.
--    Имя базы подставь своё (в GRANT нельзя использовать current_database()).
-- grant connect on database ИМЯ_БАЗЫ to testuser;
grant usage on schema public to testuser;

-- 2. Чтение: регистр, документы, прикрепления, ДВН/ОПВ/ЦЗ и справочники.
grant select on
    iperson,        -- регистр ЗЛ: поиск СП (алгоритмы С/Р/H/В), состав СК*, контроль 108
    idoc,           -- документы ЗЛ (алгоритмы C02/H02/V02, контроль 106)
    iprkdept,       -- прикрепления: тег <prk> (mo/podr/modt)
    medree_prdisp,  -- прохождение ДВН/ОПВ/ЦЗ: теги p_disp / p_proph / p_healthc
    spmo,           -- справочник МО   (контроль 111 при type_org=1)
    spsmo,          -- справочник СМО  (контроль 111 при type_org=2)
    spdocper        -- справочник типов документов (контроль 106)
to testuser;

-- 3. Журнал операций — сервис пишет в него каждый запрос (раздел 3.1).
grant select, insert on inschecklog to testuser;
-- bigserial idlog: нужна последовательность, иначе insert падает.
grant usage, select on sequence inschecklog_idlog_seq to testuser;

-- 4. Таблица дневного лимита. Контроль 5 сейчас ОТКЛЮЧЁН (как в старом сервисе),
--    поэтому строки ниже нужны только если лимит будут включать.
-- grant select, insert, update on inschecklimit to testuser;

-- ── Проверка (выполнить ПОД ПОЛЬЗОВАТЕЛЕМ СЕРВИСА) ───────────────────────────
-- Показывает, что именно видит сервис: если has_table_privilege=false —
-- не выдан grant; если таблица не найдена — сервис смотрит в другую схему.
--
-- select current_user, current_database(), current_setting('search_path');
-- select t.tab,
--        to_regclass(t.tab)                                is not null as "таблица_видна",
--        has_table_privilege(current_user, t.tab, 'select')            as "есть_select"
-- from (values ('iperson'),('idoc'),('iprkdept'),('medree_prdisp'),
--              ('spmo'),('spsmo'),('spdocper'),('inschecklog')) as t(tab);
