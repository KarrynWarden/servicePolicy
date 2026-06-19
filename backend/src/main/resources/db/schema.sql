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
    doctype   integer,
    docser    varchar(10),
    docnum    varchar(20),
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
create index if not exists ix_iperson_ss       on iperson (ss);
create index if not exists ix_iperson_idmain   on iperson (idmain);

-- ---------------------------------------------------------------------
-- Прикрепление ЗЛ (IPRKDEPT)
-- ---------------------------------------------------------------------
create table if not exists iprkdept (
    id       bigint,                  -- связь с iperson.id
    dbeg     date,
    dend     date,
    typeprk  integer,                 -- тип прикрепления (1 — основное)
    mo       integer,                 -- код МО
    otdel    varchar(4),              -- код отделения (ОООО)
    dept     integer,                 -- код участка (УУ)
    subdept  integer                  -- код ФАП (ПП)
);
create index if not exists ix_iprkdept_id on iprkdept (id);

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
-- Признаки диспансеризации/профосмотра/Центра здоровья
-- (источник для p_disp/p_proph/p_healthc). TODO: согласовать с РС ЕРЗ.
-- ---------------------------------------------------------------------
create table if not exists inscheck_event (
    id     bigint,
    etype  varchar(10),               -- DISP / PROPH / HEALTHC
    yr     integer
);
create index if not exists ix_event_id on inscheck_event (id, etype, yr);

-- ---------------------------------------------------------------------
-- Справочники
-- ---------------------------------------------------------------------
create table if not exists spmo    (code integer primary key);   -- МО
create table if not exists spsmo   (code integer primary key);   -- СМО
create table if not exists spdocper(code integer primary key);   -- типы документов
