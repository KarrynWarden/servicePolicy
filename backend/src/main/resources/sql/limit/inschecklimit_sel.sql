-- Раздел 3.2.1: выбор записи лимита по IP запроса.
-- "limit" — зарезервированное слово, поэтому квотируется и выдаётся под алиасом.
select ip, dreq, kol, "limit" as limit
from inschecklimit
where ip = :ip
