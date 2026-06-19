-- В03: 3 из 4х параметров {метафон(Фам), метафон(Им), метафон(От), (Др или схожее(Мр))} + СНИЛС.
select id, idmain
from iperson
where (
        (case when meta_fam = :meta_fam then 1 else 0 end)
      + (case when meta_im  = :meta_im  then 1 else 0 end)
      + (case when meta_ot  = :meta_ot  then 1 else 0 end)
      + (case when (
              (cast(:dr as text) is not null and dr = to_date(:dr, 'YYYY-MM-DD'))
           or (cast(:mr as text) is not null and similarity(coalesce(mr, ''), :mr) >= :sim)
            ) then 1 else 0 end)
      ) >= 3
  and ss = :snils
