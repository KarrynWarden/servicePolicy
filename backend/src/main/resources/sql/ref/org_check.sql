-- Контроль 111: код организации должен присутствовать среди АКТУАЛЬНЫХ кодов
-- справочника (current_date между dbegin и dend):
--   type_org=1 (МО)  -> SpMO.code, type_org=2 (СМО) -> SpSMO.code,
--   type_org=3 (другие) -> допустим code_org = 0.
select (
  case
    when :type_org = '1' then (select count(*) from spmo
                               where code = cast(:code_org as int)
                                 and current_date between dbegin and dend)
    when :type_org = '2' then (select count(*) from spsmo
                               where code = cast(:code_org as int)
                                 and current_date between dbegin and dend)
    when :type_org = '3' then (case when :code_org = '0' then 1 else 0 end)
    else 0
  end
)::int as cnt
