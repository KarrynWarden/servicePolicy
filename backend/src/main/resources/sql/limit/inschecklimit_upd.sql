-- Раздел 3.2.2: изменение записи лимита.
-- Если DREQ < тек.даты (новый день) — KOL=1, иначе KOL=KOL+1; DREQ=тек.дата.
update inschecklimit
set kol = case when dreq < :dreq then 1 else kol + 1 end,
    dreq = :dreq
where ip = :ip
