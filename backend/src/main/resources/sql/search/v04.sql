-- В04: Полис.
select id, idmain
from iperson
where vpolis = :vpolis
  and (npolis = :npolis or (:vpolis = 3 and enp = :npolis))
