-- Контроль 206: документ ЗЛ хранится в IDOC (связь по IDROW, TYPEROW in (1,2)).
-- Возвращает 1, если 206 НЕ нужен (документ совместим либо документов нет), иначе 0.
--
-- Логика пакета: 206 = (doctype<>:doctype OR upper(docser)<>upper(:docser)
--                        OR upper(docnum)<>upper(:docnum)). В SQL сравнение с NULL даёт
-- NULL (не TRUE), поэтому поле с null (в реестре ИЛИ в запросе) НЕ считается
-- несовпадением. Здесь это выражено null-терпимой совместимостью, а отсутствие
-- документов трактуется как «нет несовпадения» (в пакете все поля null → 206 нет).
select case
  when not exists (
        select 1 from idoc where idrow = :idrow and typerow in (1, 2)
       ) then 1
  when exists (
        select 1 from idoc
        where idrow = :idrow and typerow in (1, 2)
          and (cast(:doctype as integer) is null or doctype is null
               or doctype = cast(:doctype as integer))
          and (cast(:docser as text) is null or docser is null
               or upper(docser) = upper(cast(:docser as text)))
          and (cast(:docnum as text) is null or docnum is null
               or upper(docnum) = upper(cast(:docnum as text)))
       ) then 1
  else 0
end
