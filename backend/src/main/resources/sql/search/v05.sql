-- В05: ФИО + Др (точное совпадение).
select id, idmain
from iperson
where upper(coalesce(fam, '-')) = :fam
  and upper(coalesce(im, '-')) = :im
  and upper(coalesce(ot, '-')) = :ot
  and cast(:dr as text) is not null and dr = to_date(:dr, 'YYYY-MM-DD')
