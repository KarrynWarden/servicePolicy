-- H02 (новый раздел): Точный Ф + И/О начинаются на заданные буквы + ДР + Документ.
-- Документ — из IDOC (связь по IDROW, TYPEROW in (1,2)). a.id=b.id=c.id.
select a.id as id, max(a.idmain) as idmain
from iperson a
join idoc da on da.idrow = a.idrow and da.typerow in (1, 2)
join iperson b on b.id = a.id
join iperson c on c.id = a.id
where da.doctype = :doctype
  and da.docser = :docser
  and da.docnum = :docnum
  and b.fam = :fam
  and left(b.im, 1) = :first_im
  and left(b.ot, 1) = :first_ot
  and cast(:dr as text) is not null and c.dr = to_date(:dr, 'YYYY-MM-DD')
group by a.id
