-- С02: схожее(ФИО) + (Др или схожее(Мр)) + Документ.
-- Документ хранится в IDOC (связь по IDROW, TYPEROW in (1,2)) — на iperson поля
-- doctype/docser/docnum отсутствуют. Условия в разных записях одного ЗЛ (a.id=b.id=c.id).
select a.id as id, max(a.idmain) as idmain
from iperson a
join idoc da on da.idrow = a.idrow and da.typerow in (1, 2)
join iperson b on b.id = a.id
join iperson c on c.id = a.id
where da.doctype = :doctype
  and da.docser = :docser
  and da.docnum = :docnum
  and jarowinkler(b.fam, :fam) >= :sim
  and jarowinkler(b.im,  :im)  >= :sim
  and jarowinkler(b.ot,  :ot)  >= :sim
  and (
        (cast(:dr as text) is not null and c.dr = to_date(:dr, 'YYYY-MM-DD'))
     or (cast(:mr as text) is not null and jarowinkler(c.mr, :mr) >= :sim)
      )
group by a.id
