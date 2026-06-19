-- С02: схожее(ФИО) + (Др или схожее(Мр)) + Документ
select id, idmain
from iperson
where similarity(coalesce(fam, '-'), :fam) >= :sim
  and similarity(coalesce(im, '-'),  :im)  >= :sim
  and similarity(coalesce(ot, '-'),  :ot)  >= :sim
  and (
        (cast(:dr as text) is not null and dr = to_date(:dr, 'YYYY-MM-DD'))
     or (cast(:mr as text) is not null and similarity(coalesce(mr, ''), :mr) >= :sim)
      )
  and doctype = :doctype
  and docser = :docser
  and docnum = :docnum
