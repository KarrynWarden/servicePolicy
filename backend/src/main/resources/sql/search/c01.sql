-- С01: схожее(ФИО) + (Др или схожее(Мр)) + Полис
-- "схожее" = similarity (расширение pg_trgm) >= :sim (порог 0.8, п.4.2.5).
-- TODO: по постановке (п.4.2.3) условия, соединённые "+", могут находиться в разных
--       записях IPerson в пределах одного СК. Здесь упрощённо требуется совпадение в
--       одной записи — уточним при отладке на реальных данных.
select id, idmain
from iperson
where similarity(coalesce(fam, '-'), :fam) >= :sim
  and similarity(coalesce(im, '-'),  :im)  >= :sim
  and similarity(coalesce(ot, '-'),  :ot)  >= :sim
  and (
        (cast(:dr as text) is not null and dr = to_date(:dr, 'YYYY-MM-DD'))
     or (cast(:mr as text) is not null and similarity(coalesce(mr, ''), :mr) >= :sim)
      )
  and vpolis = :vpolis
  and (npolis = :npolis or (:vpolis = 3 and enp = :npolis))
