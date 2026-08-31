-- В04: Полис (одна запись IPERSON).
select a.id as id, max(a.idmain) as idmain
from iperson a
where a.vpolis = :vpolis
  and (ltrim(a.npolis, '0') = :npolis or (:vpolis = 3 and ltrim(a.enp, '0') = :npolis))
group by a.id
