-- С03: схожее(ФИО) + (Др или схожее(Мр)) + СНИЛС
select id, idmain
from iperson
where similarity(coalesce(fam, '-'), :fam) >= :sim
  and similarity(coalesce(im, '-'),  :im)  >= :sim
  and similarity(coalesce(ot, '-'),  :ot)  >= :sim
  and (
        (cast(:dr as text) is not null and dr = to_date(:dr, 'YYYY-MM-DD'))
     or (cast(:mr as text) is not null and similarity(coalesce(mr, ''), :mr) >= :sim)
      )
  and ss = :snils
