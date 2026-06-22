-- В02: 3 из 4х {метафон(Фам), метафон(Им), метафон(От), (Др или схожее(Мр))} + Документ.
-- Метафон-условия на b, Др/Мр на c, Документ на a (a.id=b.id=c.id).
select a.id as id, max(a.idmain) as idmain
from iperson a
join iperson b on b.id = a.id
join iperson c on c.id = a.id
where a.doctype = :doctype
  and a.docser = :docser
  and a.docnum = :docnum
  and (
        (case when b.meta_fam = :meta_fam then 1 else 0 end)
      + (case when b.meta_im  = :meta_im  then 1 else 0 end)
      + (case when b.meta_ot  = :meta_ot  then 1 else 0 end)
      + (case when (
              (cast(:dr as text) is not null and c.dr = to_date(:dr, 'YYYY-MM-DD'))
           or (cast(:mr as text) is not null and jarowinkler(c.mr, :mr) >= :sim)
            ) then 1 else 0 end)
      ) >= 3
group by a.id
