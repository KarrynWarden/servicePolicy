-- Контроль 208: сходство мест рождения по jaro_winkler (порог 0.8).
-- Аналог Oracle utl_match.jaro_winkler(upper(a.mr), upper(b.mr)).
select jarowinkler(upper(:mr1), upper(:mr2))
