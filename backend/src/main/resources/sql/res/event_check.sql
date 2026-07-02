-- Признаки p_disp / p_proph / p_healthc (Таблица 3): наличие записи в MEDREE_PRDISP
-- по ЗЛ и коду группы (1 — диспансеризация, 2 — профосмотр, 3 — Центр здоровья)
-- за нужный год.
select count(*)::int
from medree_prdisp
where id = :id
  and groupcode = :groupcode
  and year = :yr
