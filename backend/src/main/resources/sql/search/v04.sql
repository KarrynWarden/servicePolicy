-- В04: Полис (одна запись IPERSON).
select a.id as id, max(a.idmain) as idmain
from iperson a
where a.vpolis = :vpolis
  and (a.npolis = :npolis or (:vpolis = 3 and a.enp = :npolis))
group by a.id
