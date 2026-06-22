-- С01: схожее(ФИО) + (Др или схожее(Мр)) + Полис.
-- Перенос пакета inscheck.checkAlg: условия, соединённые "+", ищутся в разных
-- записях IPERSON в пределах одного ЗЛ (a.id=b.id=c.id) — Полис на a, ФИО на b,
-- Др/Мр на c. "схожее" = jarowinkler >= :sim (0.8, п.4.2.5).
select a.id as id, max(a.idmain) as idmain
from iperson a
join iperson b on b.id = a.id
join iperson c on c.id = a.id
where a.vpolis = :vpolis
  and (a.npolis = :npolis or (:vpolis = 3 and a.enp = :npolis))
  and jarowinkler(b.fam, :fam) >= :sim
  and jarowinkler(b.im,  :im)  >= :sim
  and jarowinkler(b.ot,  :ot)  >= :sim
  and (
        (cast(:dr as text) is not null and c.dr = to_date(:dr, 'YYYY-MM-DD'))
     or (cast(:mr as text) is not null and jarowinkler(c.mr, :mr) >= :sim)
      )
group by a.id
