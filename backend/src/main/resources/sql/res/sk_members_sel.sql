-- 4.4: все записи комплекта полисов СК*.
-- Записи с одинаковым IDMain считаются одним СК*; если IDMain отсутствует —
-- берётся одиночная запись по ID.
select id, idmain, smo, vpolis, fpolis, npolis, enp,
       fam, im, ot, w, dr, mr,
       doctype, docser, docnum, ss,
       dvizit, dbeg, dend, reason, ddeath
from iperson
where (cast(:idmain as bigint) is not null and idmain = cast(:idmain as bigint))
   or (cast(:idmain as bigint) is null and id = :id)
