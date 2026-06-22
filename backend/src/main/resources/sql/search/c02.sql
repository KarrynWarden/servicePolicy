-- С02: схожее(ФИО) + (Др или схожее(Мр)) + Документ.
-- Условия в разных записях одного ЗЛ (a.id=b.id=c.id): Документ на a, ФИО на b, Др/Мр на c.
select a.id as id, max(a.idmain) as idmain
from iperson a
join iperson b on b.id = a.id
join iperson c on c.id = a.id
where a.doctype = :doctype
  and a.docser = :docser
  and a.docnum = :docnum
  and jarowinkler(b.fam, :fam) >= :sim
  and jarowinkler(b.im,  :im)  >= :sim
  and jarowinkler(b.ot,  :ot)  >= :sim
  and (
        (cast(:dr as text) is not null and c.dr = to_date(:dr, 'YYYY-MM-DD'))
     or (cast(:mr as text) is not null and jarowinkler(c.mr, :mr) >= :sim)
      )
group by a.id
