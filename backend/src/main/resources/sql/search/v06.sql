-- В06: метафон(ФИО) + Др. Перенос пакета: метафон ФИО на записи a, Др на записи b (a.id=b.id).
select b.id as id, max(a.idmain) as idmain
from iperson a
join iperson b on b.id = a.id
where a.meta_fam = :meta_fam
  and a.meta_im = :meta_im
  and a.meta_ot = :meta_ot
  and cast(:dr as text) is not null and b.dr = to_date(:dr, 'YYYY-MM-DD')
group by b.id
