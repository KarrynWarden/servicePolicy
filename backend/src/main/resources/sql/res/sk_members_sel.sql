-- 4.4: все записи комплекта полисов СК*.
-- Записи с одинаковым IDMain считаются одним СК*; если IDMain отсутствует —
-- берётся одиночная запись по ID. Документы (doctype/docser/docnum) в iperson
-- отсутствуют — они в IDOC (проверка 206 идёт отдельным запросом по IDROW).
select id, idrow, idmain, smo, vpolis, fpolis, npolis, enp,
       fam, im, ot, w, dr, mr, ss,
       dvizit, dbeg, dend, reason, ddeath
from iperson
where (cast(:idmain as bigint) is not null and idmain = cast(:idmain as bigint))
   or (cast(:idmain as bigint) is null and id = :id)
