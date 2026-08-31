-- Контроль 108: наличие номера полиса в Регистре.
-- Для полиса единого образца (vpolis=3) допускается совпадение по ЕНП (enp).
select count(*)::int
from iperson
where (vpolis = :vpolis and ltrim(npolis, '0') = :npolis)
   or (:vpolis = 3 and ltrim(enp, '0') = :npolis)
