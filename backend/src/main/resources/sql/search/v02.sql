-- В02: 3 из 4х {метафон(Фам), метафон(Им), метафон(От), (Др или схожее(Мр))} + Документ.
-- Документ — из IDOC (связь по IDROW, TYPEROW in (1,2)). Метафон на b, Др/Мр на c.
select a.id as id, max(a.idmain) as idmain
from iperson a
join idoc da on da.idrow = a.idrow and da.typerow in (1, 2)
join iperson b on b.id = a.id
join iperson c on c.id = a.id
where da.doctype = :doctype
  and da.docser = :docser
  and da.docnum = :docnum
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
