-- Контроль 111: код организации по справочникам.
-- 1 — МО (SpMO), 2 — СМО (SpSMO), 3 — другие (code_org = 0).
select (
  case
    when :type_org = '1' then (select count(*) from spmo  where code = cast(:code_org as int))
    when :type_org = '2' then (select count(*) from spsmo where code = cast(:code_org as int))
    when :type_org = '3' then (case when :code_org = '0' then 1 else 0 end)
    else 0
  end
)::int as cnt
