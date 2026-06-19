-- 5.1: прикрепление ЗЛ по ПЗСК*.
-- ID записи IPerson = IPrkDept.ID; период поиска [date1;date2] внутри [dbeg;dend];
-- среди найденных — Max(Dbeg), затем Max(TypePrk).
select id, dbeg, dend, typeprk, mo, otdel, dept, subdept
from iprkdept
where id = :id
  and dbeg <= :date1
  and dend >= :date2
order by dbeg desc, typeprk desc
limit 1
