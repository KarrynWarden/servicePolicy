-- Раздел 3.1: добавление записи в журнал операций InscheckLog.
-- IDLOG — автоинкремент (serial), TIMEOPER — дата/время операции.
insert into inschecklog (timeoper, method, login, ip, inpar, err, typeorg, codeorg, iptfoms)
values (now(), :method, :login, :ip, :inpar, :err, :typeorg, :codeorg, :iptfoms)
