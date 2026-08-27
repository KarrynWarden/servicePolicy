-- Контроль 111: код организации должен присутствовать среди кодов справочника,
-- действовавших В ПЕРИОД ЗАПРОСА [date1; date2], а НЕ на текущую дату.
-- Иначе код, актуальный на дату обращения, но закрытый позже, ошибочно
-- считался бы неверным (запрос за 2023 год отрабатывал по справочнику «на сегодня»).
--   type_org=1 (МО)  -> SpMO.code, type_org=2 (СМО) -> SpSMO.code,
--   type_org=3 (другие) -> допустим code_org = 0.
-- Период считается пересекающимся: dbegin <= date2 и dend >= date1.
-- Пустые границы трактуются как «открытый интервал».
select (
  case
    when :type_org = '1' then (select count(*) from spmo
                               where code = cast(:code_org as int)
                                 and coalesce(dbegin, date '0001-01-01') <= cast(:date2 as date)
                                 and coalesce(dend,   date '9999-12-31') >= cast(:date1 as date))
    when :type_org = '2' then (select count(*) from spsmo
                               where code = cast(:code_org as int)
                                 and coalesce(dbegin, date '0001-01-01') <= cast(:date2 as date)
                                 and coalesce(dend,   date '9999-12-31') >= cast(:date1 as date))
    when :type_org = '3' then (case when :code_org = '0' then 1 else 0 end)
    else 0
  end
)::int as cnt
