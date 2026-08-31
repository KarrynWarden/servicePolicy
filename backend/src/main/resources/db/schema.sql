-- =====================================================================
--  InsCheck (А14, ON-LINE проверка полисов) — схема PostgreSQL.
--  Перенос с Oracle. Структура таблиц соответствует постановке (Приложение 1)
--  и SQL/полям старого сервиса. Запускать вручную при развёртывании.
--
--  Расширения:
--    pg_trgm       — similarity() для алгоритма «схожее» (порог 0.8).
--    fuzzystrmatch — опционально, если META_* пересчитываются на стороне БД
--                    (в текущей реализации метафон входа считается в Java).
-- =====================================================================

create extension if not exists pg_trgm;
create extension if not exists fuzzystrmatch;

-- ---------------------------------------------------------------------
-- РС ЕРЗ: сведения о застрахованных (IPERSON)
-- ---------------------------------------------------------------------
create table if not exists iperson (
    id        bigint primary key,
    idrow     bigint,                 -- идентификатор строки (связь с IDOC.idrow)
    idmain    bigint,                 -- группировка записей в СК* (п.4.4)
    smo       integer,                -- код СМО
    vpolis    integer,                -- тип полиса (1,2,3)
    fpolis    integer,                -- форма полиса (0..3)
    npolis    varchar(16),
    enp       varchar(16),            -- ЕНП (для vpolis=3)
    fam       varchar(40),
    im        varchar(40),
    ot        varchar(40),
    w         integer,                -- пол (1,2)
    dr        date,                   -- дата рождения
    mr        varchar(160),           -- место рождения
    meta_fam  varchar(40),            -- метафон фамилии (созвучное)
    meta_im   varchar(40),
    meta_ot   varchar(40),
    -- Документы (doctype/docser/docnum) вынесены в таблицу IDOC (связь по idrow).
    ss        varchar(14),            -- СНИЛС
    dvizit    date,                   -- дата заявления
    dbeg      date,                   -- дата начала финансирования
    dend      date,                   -- дата окончания финансирования
    reason    integer,                -- причина окончания финансирования
    ddeath    date                    -- дата смерти
);

-- Индексы для нечёткого поиска ФИО (pg_trgm) и точных совпадений.
create index if not exists ix_iperson_fam_trgm on iperson using gin (fam gin_trgm_ops);
create index if not exists ix_iperson_im_trgm  on iperson using gin (im  gin_trgm_ops);
create index if not exists ix_iperson_ot_trgm  on iperson using gin (ot  gin_trgm_ops);
create index if not exists ix_iperson_meta     on iperson (meta_fam, meta_im, meta_ot);
create index if not exists ix_iperson_polis    on iperson (vpolis, npolis);
create index if not exists ix_iperson_enp      on iperson (enp);
-- Номер полиса сравнивается по значащей части: в Регистре лидирующие нули хранятся
-- непоследовательно, поэтому и запрос, и колонка приводятся через ltrim(..., '0').
-- Нужны функциональные индексы — обычные ix_iperson_polis/ix_iperson_enp под ltrim не подходят.
create index if not exists ix_iperson_polis_nz on iperson (vpolis, ltrim(npolis, '0'));
create index if not exists ix_iperson_enp_nz   on iperson (ltrim(enp, '0'));
create index if not exists ix_iperson_ss       on iperson (ss);
create index if not exists ix_iperson_idmain   on iperson (idmain);
create index if not exists ix_iperson_idrow    on iperson (idrow);
-- Ключ комплекта СК* (п.4.4). Алгоритмы поиска соединяют записи ЗЛ именно по нему
-- (условия, соединённые «+», могут совпасть в РАЗНЫХ записях одного комплекта),
-- поэтому нужен функциональный индекс — обычный ix_iperson_idmain здесь не работает.
create index if not exists ix_iperson_sk       on iperson ((coalesce(idmain, id)));

-- ---------------------------------------------------------------------
-- Документы ЗЛ (IDOC). Связь с iperson по IDROW; для поиска/сверки берутся
-- строки с TYPEROW in (1,2). Контроли: поиск по документу (С02/H02/В02) и 206.
-- ---------------------------------------------------------------------
create table if not exists idoc (
    idrow    bigint,                  -- связь с iperson.idrow
    typerow  integer,                 -- тип строки документа (берутся 1 и 2)
    doctype  integer,
    docser   varchar(10),
    docnum   varchar(20)
);
create index if not exists ix_idoc_idrow on idoc (idrow);

-- ---------------------------------------------------------------------
-- Прикрепление ЗЛ (IPRKDEPT). Полная структура по постановке (PG.IPRKDEPT).
-- Сервис использует подмножество полей (id, dbeg, dend, dvizit, ddepart,
-- typeprk, mo, otdel, dept, subdept) — п.5 и формирование podr (п.6.1).
-- ---------------------------------------------------------------------
create table if not exists iprkdept (
    idrw       bigint generated always as identity primary key, -- идентификатор записи (автоинкремент)
    id         bigint not null,        -- идентификатор комплекта ЗЛ (IPerson.ID)
    typeprk    smallint,               -- тип прикрепления (1 — по АПП, 3 — доврачебная в ФАП)
    dbeg       date,                   -- дата загрузки прикрепления
    dend       date,                   -- дата загрузки открепления
    dvizit     date,                   -- дата прикрепления
    ddepart    date,                   -- дата открепления
    mo         integer,                -- код МО (SpMu.Code)
    otdel      varchar(4),             -- код отделения МО ОООО (SpOtdel.Code, хранить 4 симв. с нулями)
    dept       integer,                -- код участка МО УУ (SpDept.Code)
    subdept    integer,                -- код пункта/ФАП ПП (SpSubDept.Code)
    methprk    smallint,               -- способ прикрепления (1 — террит.-участк., 2 — по заявлению)
    sourcebeg  smallint,               -- источник сведений о прикреплении (1-СМО, 2-ТФОМС, 3-МО)
    sourceend  smallint,               -- источник сведений об откреплении (1-СМО, 2-ТФОМС, 3-МО)
    reason     smallint,               -- причина открепления ЗЛ (SpIPrkReason.Code)
    idlogbeg   bigint,                 -- журнал загрузки о прикреплении (IPrkLog.IDLog)
    idlogend   bigint,                 -- журнал загрузки об откреплении (IPrkLog.IDLog)
    lastupdate date                    -- дата последнего обновления записи (заполняется триггером)
);
create index if not exists ix_iprkdept_id on iprkdept (id);

-- Триггер: проставляет lastupdate текущей датой на insert/update.
create or replace function iprkdept_set_lastupdate() returns trigger as $$
begin
    new.lastupdate := current_date;
    return new;
end;
$$ language plpgsql;

drop trigger if exists trg_iprkdept_lastupdate on iprkdept;
create trigger trg_iprkdept_lastupdate
    before insert or update on iprkdept
    for each row execute function iprkdept_set_lastupdate();

-- ---------------------------------------------------------------------
-- Журнал операций (INSCHECKLOG), раздел 3.1
-- ---------------------------------------------------------------------
create table if not exists inschecklog (
    idlog    bigserial primary key,
    timeoper timestamp not null default now(),
    method   varchar(50),
    login    varchar(100),
    ip       varchar(45),
    inpar    text,
    err      varchar(500),
    typeorg  varchar(1),
    codeorg  varchar(4),
    iptfoms  varchar(45)
);

-- ---------------------------------------------------------------------
-- Счётчик запросов по IP (INSCHECKLIMIT), раздел 3.2
-- ---------------------------------------------------------------------
create table if not exists inschecklimit (
    ip      varchar(45) primary key,
    dreq    date,
    kol     integer,
    "limit" integer
);

-- ---------------------------------------------------------------------
-- Признаки диспансеризации/профосмотра/Центра здоровья (MEDREE_PRDISP).
-- Источник для p_disp/p_proph/p_healthc. Заполняется ETL-джобом из Oracle
-- (таблицы medree на PG нет). GROUPCODE: 1 — диспансеризация, 3 — профосмотр,
-- 2 — Центр здоровья.
-- ---------------------------------------------------------------------
create table if not exists medree_prdisp (
    id        bigint not null,         -- идентификатор ЗЛ (IPerson.ID)
    groupcode smallint not null,       -- 1 — дисп., 3 — проф., 2 — центр здоровья
    year      integer not null,        -- год (EXTRACT(YEAR FROM date_2))
    month     integer                  -- месяц
);
create index if not exists ix_medree_prdisp on medree_prdisp (id, year, groupcode);

-- ---------------------------------------------------------------------
-- Справочники
-- ---------------------------------------------------------------------
-- SpMO/SpSMO: коды с периодом актуальности (dbegin..dend) — контроль 111.
-- Границы периода заполнены всегда (NULL в dbegin/dend не бывает).
create table if not exists spmo  (code integer, dbegin date not null, dend date not null);   -- МО
create table if not exists spsmo (code integer, dbegin date not null, dend date not null);   -- СМО
create index if not exists ix_spmo_code  on spmo  (code);
create index if not exists ix_spsmo_code on spsmo (code);

create table if not exists spdocper(code integer primary key);   -- типы документов (контроль 106)
