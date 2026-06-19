-- В06: метафон(ФИО) + Др.
select id, idmain
from iperson
where meta_fam = :meta_fam
  and meta_im = :meta_im
  and meta_ot = :meta_ot
  and cast(:dr as text) is not null and dr = to_date(:dr, 'YYYY-MM-DD')
