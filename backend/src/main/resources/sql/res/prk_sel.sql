-- 5.1: прикрепление ЗЛ по ПЗСК*.
-- Перенос пакета: связь по ID; период — пересечение date1<=dend AND dbeg<=date2
-- (оригинал: a0.date1<=b0.dend and b0.dbeg<=a0.date2); среди найденных —
-- Max(Dbeg), затем Max(TypePrk).
select id, dbeg, dend, typeprk, mo, otdel, dept, subdept
from iprkdept
where id = :id
  and dend >= :date1
  and dbeg <= :date2
order by dbeg desc, typeprk desc
limit 1
