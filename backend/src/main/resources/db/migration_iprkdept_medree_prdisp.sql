-- =====================================================================
--  Миграция: таблицы IPRKDEPT (полная структура) и MEDREE_PRDISP.
--  PostgreSQL. Запускать вручную при развёртывании.
--
--  medree на PG не переносится (огромная таблица, остаётся в Oracle),
--  поэтому признаки p_disp/p_proph/p_healthc берутся из MEDREE_PRDISP,
--  которую заполняет ETL-джоб из Oracle.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Прикрепление ЗЛ (PG.IPRKDEPT)
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
    otdel      varchar(4),             -- код отделения МО ОООО (SpOtdel.Code; хранить 4 симв. с нулями)
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
-- Признаки диспансеризации/профосмотра/Центра здоровья (MEDREE_PRDISP)
-- ---------------------------------------------------------------------
create table if not exists medree_prdisp (
    id        bigint not null,         -- идентификатор ЗЛ (IPerson.ID)
    groupcode smallint not null,       -- 1 — диспансеризация, 3 — профосмотр, 2 — Центр здоровья
    year      integer not null,        -- год (EXTRACT(YEAR FROM date_2))
    month     integer                  -- месяц
);
create index if not exists ix_medree_prdisp on medree_prdisp (id, year, groupcode);
