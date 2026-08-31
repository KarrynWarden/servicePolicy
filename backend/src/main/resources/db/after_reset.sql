-- ЗАПУСКАТЬ ПОСЛЕ КАЖДОГО ЕЖЕНЕДЕЛЬНОГО СБРОСА ТЕСТОВОЙ БД ДО СОСТОЯНИЯ БОЕВОЙ.
--
-- Сброс возвращает боевую схему, в которой нет ни наших индексов, ни прав для
-- пользователя сервиса. Без индексов поиск деградирует катастрофически
-- (замер на 300 тыс. строк: алгоритм С03 — 0.13 мс с индексом против 3017 мс без него),
-- без прав — сервис отдаёт errcode 500.
--
-- Скрипт идемпотентный: повторный запуск безопасен.
-- Порядок: сначала под ВЛАДЕЛЬЦЕМ таблиц (или суперпользователем) — индексы и права.
-- Замени testuser на реального пользователя сервиса.

-- ── 1. Права ─────────────────────────────────────────────────────────────────
grant usage on schema public to testuser;

grant select on
    iperson,        -- регистр ЗЛ
    idoc,           -- документы ЗЛ
    iprkdept,       -- прикрепления: тег <prk>
    medree_prdisp,  -- ДВН/ОПВ/ЦЗ: p_disp / p_proph / p_healthc
    spmo,           -- справочник МО  (контроль 111)
    spsmo,          -- справочник СМО (контроль 111)
    spdocper        -- типы документов (контроль 106)
to testuser;

grant select, insert on inschecklog to testuser;
grant usage, select on sequence inschecklog_idlog_seq to testuser;

-- ── 2. Индексы ───────────────────────────────────────────────────────────────
-- concurrently — чтобы не блокировать таблицы; выполнять ВНЕ транзакции
-- (в psql запускать файл без -1 / без BEGIN).

-- Ключ комплекта СК*: алгоритмы соединяют записи ЗЛ по нему (механика «+»).
-- САМЫЙ ВАЖНЫЙ индекс: без него поиск не просто медленный, а неработоспособный.
create index concurrently if not exists ix_iperson_sk
    on iperson ((coalesce(idmain, id)));

-- Номер полиса сравнивается по значащей части (лидирующие нули в Регистре
-- хранятся непоследовательно), поэтому нужны функциональные индексы.
create index concurrently if not exists ix_iperson_polis_nz
    on iperson (vpolis, ltrim(npolis, '0'));
create index concurrently if not exists ix_iperson_enp_nz
    on iperson (ltrim(enp, '0'));

-- Алгоритмы С02/H02/В02 ищут ЗЛ по документу, а не по idrow.
create index concurrently if not exists ix_idoc_doc
    on idoc (doctype, docser, docnum);

-- Базовые индексы, которые могут отсутствовать в боевой схеме.
create index concurrently if not exists ix_iperson_meta   on iperson (meta_fam, meta_im, meta_ot);
create index concurrently if not exists ix_iperson_ss     on iperson (ss);
create index concurrently if not exists ix_iperson_idrow  on iperson (idrow);
create index concurrently if not exists ix_iprkdept_id    on iprkdept (id);

analyze iperson;
analyze idoc;

-- ── 3. Проверка (выполнить ПОД ПОЛЬЗОВАТЕЛЕМ СЕРВИСА) ────────────────────────
-- select t.tab,
--        to_regclass(t.tab) is not null                      as "таблица_видна",
--        has_table_privilege(current_user, t.tab, 'select')   as "есть_select"
-- from (values ('iperson'),('idoc'),('iprkdept'),('medree_prdisp'),
--              ('spmo'),('spsmo'),('spdocper'),('inschecklog')) as t(tab);
--
-- select indexname from pg_indexes
--  where indexname in ('ix_iperson_sk','ix_iperson_polis_nz','ix_iperson_enp_nz','ix_idoc_doc');
