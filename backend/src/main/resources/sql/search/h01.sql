-- H01 (новый раздел): Точный Ф + И начинается на заданную букву
--                      + О начинается на заданную букву + ДР + Полис.
select id, idmain
from iperson
where upper(coalesce(fam, '-')) = :fam
  and left(upper(coalesce(im, '-')), 1) = :first_im
  and left(upper(coalesce(ot, '-')), 1) = :first_ot
  and cast(:dr as text) is not null and dr = to_date(:dr, 'YYYY-MM-DD')
  and vpolis = :vpolis
  and (npolis = :npolis or (:vpolis = 3 and enp = :npolis))
