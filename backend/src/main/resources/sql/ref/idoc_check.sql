-- Контроль 206: документ ЗЛ хранится в IDOC (связь по IDROW, TYPEROW in (1,2)).
-- Совпадение: doctype точно, docser/docnum без учёта регистра (как в пакете 206).
select count(*)::int
from idoc
where idrow = :idrow
  and typerow in (1, 2)
  and doctype = :doctype
  and upper(docser) = upper(:docser)
  and upper(docnum) = upper(:docnum)
