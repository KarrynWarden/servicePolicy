#!/bin/bash
# Сборка war и выкладка на тестовый сервер (по образцу деплоя других сервисов ТФОМС).
# Maven-проект лежит в каталоге backend/, поэтому пути считаем от расположения скрипта.
set -euo pipefail

scriptDir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
projectDir="${scriptDir}/../backend"

projName=InsCheckR
mode=test
host=10.0.100.24
warFile=${projName}.war
user=konkin

# 1. Сборка war с профилем test — в war зашивается spring.profiles.active=test,
#    то есть на сервере активируется application-test.properties (подключение к БД
#    через /opt/tomcat/tfoms/conf/common.properties).
mvn -f "${projectDir}/pom.xml" clean package -P ${mode}

# 2. Копируем war на сервер и раскладываем в Tomcat (Tomcat сам развернёт по контексту /InsCheckR).
scp -i ~${user}/.ssh/${user}_key_rsa "${projectDir}/target/${warFile}" devel@${host}:/home/devel/
ssh -i ~${user}/.ssh/${user}_key_rsa devel@${host} "sudo cp /home/devel/${warFile} /opt/tomcat/webapps/"

echo "готово: http://${host}:8080/${projName}/ws  (WSDL: /${projName}/ws/inscheck.wsdl)"
