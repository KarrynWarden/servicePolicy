-- Раздел 3.2.2: добавление записи лимита (KOL=1, LIMIT=1000).
insert into inschecklimit (ip, dreq, kol, "limit")
values (:ip, :dreq, :kol, :limit)
