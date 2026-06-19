-- Признаки p_disp / p_proph / p_healthc (Таблица 3): прохождение мероприятия
-- (DISP — диспансеризация, PROPH — профосмотр, HEALTHC — Центр здоровья)
-- в году даты окончания поиска.
-- TODO: уточнить реальную таблицу/источник этих сведений в РС ЕРЗ.
select count(*)::int
from inscheck_event
where id = :id
  and etype = :etype
  and yr = :yr
