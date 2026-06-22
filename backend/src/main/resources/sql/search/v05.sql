-- В05: ФИО + Др. Перенос пакета: точное ФИО на записи a, Др на записи b (a.id=b.id).
-- :fam/:im/:ot приходят в верхнем регистре («-» если пусто).
select b.id as id, max(a.idmain) as idmain
from iperson a
join iperson b on b.id = a.id
where a.fam = :fam
  and a.im = :im
  and a.ot = :ot
  and cast(:dr as text) is not null and b.dr = to_date(:dr, 'YYYY-MM-DD')
group by b.id
