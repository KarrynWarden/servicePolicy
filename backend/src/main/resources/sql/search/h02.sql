-- H02 (новый раздел): Точный Ф + И/О начинаются на заданные буквы + ДР + Документ.
select id, idmain
from iperson
where upper(coalesce(fam, '-')) = :fam
  and left(upper(coalesce(im, '-')), 1) = :first_im
  and left(upper(coalesce(ot, '-')), 1) = :first_ot
  and cast(:dr as text) is not null and dr = to_date(:dr, 'YYYY-MM-DD')
  and doctype = :doctype
  and docser = :docser
  and docnum = :docnum
