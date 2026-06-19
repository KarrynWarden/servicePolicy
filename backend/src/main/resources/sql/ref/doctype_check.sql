-- Контроль 106: тип документа из справочника SPDOCPER.
select count(*)::int from spdocper where code = :doctype
