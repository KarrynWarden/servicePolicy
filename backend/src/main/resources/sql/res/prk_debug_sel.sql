-- Диагностика 305: все прикрепления по id (без фильтра периода).
select id, dbeg, dend, typeprk, mo, otdel, dept, subdept
from iprkdept
where id = :id
order by dbeg desc
