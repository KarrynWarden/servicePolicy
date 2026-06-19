-- Контроль 108: наличие номера полиса в Регистре.
-- Для полиса единого образца (vpolis=3) допускается совпадение по ЕНП (enp).
select count(*)::int
from iperson
where (vpolis = :vpolis and npolis = :npolis)
   or (:vpolis = 3 and enp = :npolis)
