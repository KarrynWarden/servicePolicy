-- Р01: 3 из 4х {метафон(Фам), метафон(Им), метафон(От), Др} + Полис (обязателен).
-- Перенос пакета: метафон-условия на записи b, Др на записи c, Полис на a
-- (в пределах СК*: coalesce(idmain,id)). META_* в IPERSON предрасчитаны Oracle a81.SoundexRUS;
-- метафон входа считается в Java тем же алгоритмом (RussianMetaphone) и приходит как :meta_*.
select a.id as id, max(a.idmain) as idmain
from iperson a
join iperson b on coalesce(b.idmain, b.id) = coalesce(a.idmain, a.id)
join iperson c on coalesce(c.idmain, c.id) = coalesce(a.idmain, a.id)
where a.vpolis = :vpolis
  and (a.npolis = :npolis or (:vpolis = 3 and a.enp = :npolis))
  and (
        (case when b.meta_fam = :meta_fam then 1 else 0 end)
      + (case when b.meta_im  = :meta_im  then 1 else 0 end)
      + (case when b.meta_ot  = :meta_ot  then 1 else 0 end)
      + (case when (cast(:dr as text) is not null and c.dr = to_date(:dr, 'YYYY-MM-DD')) then 1 else 0 end)
      ) >= 3
group by a.id
