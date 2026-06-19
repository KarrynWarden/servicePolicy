-- Р01: 3 из 4х параметров {метафон(Фам), метафон(Им), метафон(От), Др} + Полис (обязателен).
-- "метафон/созвучное" (п.3.1.4.3) = равенство с предрасчитанными колонками META_*.
-- Метафон входного значения считается в Java (RussianMetaphone) и приходит как :meta_*.
select id, idmain
from iperson
where (
        (case when meta_fam = :meta_fam then 1 else 0 end)
      + (case when meta_im  = :meta_im  then 1 else 0 end)
      + (case when meta_ot  = :meta_ot  then 1 else 0 end)
      + (case when (cast(:dr as text) is not null and dr = to_date(:dr, 'YYYY-MM-DD')) then 1 else 0 end)
      ) >= 3
  and vpolis = :vpolis
  and (npolis = :npolis or (:vpolis = 3 and enp = :npolis))
