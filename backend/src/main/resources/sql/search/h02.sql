-- H02 (новый раздел): Точный Ф + И/О начинаются на заданные буквы + ДР + Документ.
-- Документ на a, ФИО на b, Др на c (a.id=b.id=c.id).
select a.id as id, max(a.idmain) as idmain
from iperson a
join iperson b on b.id = a.id
join iperson c on c.id = a.id
where a.doctype = :doctype
  and a.docser = :docser
  and a.docnum = :docnum
  and b.fam = :fam
  and left(b.im, 1) = :first_im
  and left(b.ot, 1) = :first_ot
  and cast(:dr as text) is not null and c.dr = to_date(:dr, 'YYYY-MM-DD')
group by a.id
