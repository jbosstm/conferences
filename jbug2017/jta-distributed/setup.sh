#!/bin/sh

set -ex

function unzipTo {
  local TEMP=`mktemp -d`
  unzip -d "${TEMP}" "${1}"
  local DIR=`echo ${TEMP}/*`
  mv "${DIR}" "${2}"
}

WFLY_ZIP="wildfly-11.0.0.Final.zip"
WFLY_DOWNLOAD="http://download.jboss.org/wildfly/11.0.0.Final/${WFLY_ZIP}"

if [ 'x' = "x${JBOSS_HOME_1}" ] || [ 'x' = "x${JBOSS_HOME_1}" ]; then
    [ ! -f "$WFLY_ZIP" ] && wget "${WFLY_DOWNLOAD}"
    if [ 'x' = "x${JBOSS_HOME_1}" ]; then
        export JBOSS_HOME_1="${PWD}/jboss1"
        rm -rf "${JBOSS_HOME_1}"
        unzipTo "${WFLY_ZIP}" "${JBOSS_HOME_1}"
    fi
    if [ 'x' = "x${JBOSS_HOME_2}" ]; then
        export JBOSS_HOME_2="${PWD}/jboss2"
        rm -rf "${JBOSS_HOME_2}"
        unzipTo "${WFLY_ZIP}" "${JBOSS_HOME_2}"
    fi
fi 


if [ ! -d "${JBOSS_HOME_1}" ] || [ ! -f "${JBOSS_HOME_1}/jboss-modules.jar" ]; then echo "JBOSS_HOME_1 at '${JBOSS_HOME_1}' is not a jboss directory" && exit 1; fi
if [ ! -d "${JBOSS_HOME_2}" ] || [ ! -f "${JBOSS_HOME_2}/jboss-modules.jar" ]; then echo "JBOSS_HOME_2 at '${JBOSS_HOME_2}' is not a jboss directory" && exit 1; fi

"${JBOSS_HOME_1}/bin/add-user.sh" -a -u admin123 -p Password1! --silent
"${JBOSS_HOME_2}/bin/add-user.sh" -a -u admin123 -p Password1! --silent

cp standalone-full-client.xml "${JBOSS_HOME_1}/standalone/configuration/"
cp standalone-full-server.xml "${JBOSS_HOME_2}/standalone/configuration/"

cp postgresql*.jar "${JBOSS_HOME_1}/standalone/deployments/postgresql-driver.jar"
cp postgresql*.jar "${JBOSS_HOME_2}/standalone/deployments/postgresql-driver.jar"

cd dockerfile
docker build -t postgresql-ts-presentation-jta-dist .
docker run -p 5432:5432 -d --rm postgresql-ts-presentation-jta-dist
docker run -p 5433:5432 -d --rm postgresql-ts-presentation-jta-dist
cd -

cd wfly-client
mvn clean install; cp target/wfly-client.war "${JBOSS_HOME_1}/standalone/deployments/"
cd -
cd wfly-server
mvn clean install; cp target/wfly-server.jar "${JBOSS_HOME_2}/standalone/deployments/"
cd -

cd "${JBOSS_HOME_1}/"
./bin/standalone.sh -c standalone-full-client.xml &
cd "${JBOSS_HOME_2}/"
./bin/standalone.sh -c standalone-full-server.xml -Djboss.socket.binding.port-offset=100 &
cd -

echo "Waiting for 20 seconds"
sleep 20

echo "\n\n\nSERVER_1: ${JBOSS_HOME_1}\n#################################\n"
tail -n 10 ${JBOSS_HOME_1}/standalone/log/server.log
echo "\n\n\nSERVER_2: ${JBOSS_HOME_2}\n#################################\n"
tail -n 10 ${JBOSS_HOME_2}/standalone/log/server.log

echo "\n\n\nRunning servers:\n"
jobs

curl -I -X GET http://localhost:8080/wfly-client/client

