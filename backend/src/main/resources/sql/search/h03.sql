-- H03 (новый раздел): Точный Ф + И/О начинаются на заданные буквы + ДР + СНИЛС.
-- СНИЛС на a, ФИО на b, Др на c (в пределах СК*: coalesce(idmain,id)).
select a.id as id, max(a.idmain) as idmain
from iperson a
join iperson b on coalesce(b.idmain, b.id) = coalesce(a.idmain, a.id)
join iperson c on coalesce(c.idmain, c.id) = coalesce(a.idmain, a.id)
where a.ss = :snils
  and b.fam = :fam
  and left(b.im, 1) = :first_im
  and left(b.ot, 1) = :first_ot
  and cast(:dr as text) is not null and c.dr = to_date(:dr, 'YYYY-MM-DD')
group by a.id
