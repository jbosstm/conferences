#!/usr/bin/env bash

urlencode() {
    # urlencode <string>
    old_lc_collate=$LC_COLLATE
    LC_COLLATE=C
    
    local length="${#1}"
    for (( i = 0; i < length; i++ )); do
        local c="${1:i:1}"
        case $c in
            [a-zA-Z0-9.~_-]) printf "$c" ;;
            *) printf '%%%02X' "'$c" ;;
        esac
    done
    
    LC_COLLATE=$old_lc_collate
}

set -e

if [ -z $SVRONLY ]; then
    mvn clean install $@
fi

[ $DEBUG ] || DEBUG=0
[ $SVRONLY ] || SVRONLY=0
  
export PORT=8787
export JDWP=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=
function getDebugArgs {
  [ $DEBUG -eq 1 ] && echo "$JDWP"$1 || echo ""
}
[ $NARAYANA_VERSION ] || NARAYANA_VERSION=5.7.0.Final

java $(getDebugArgs $PORT) -jar narayana-full-$NARAYANA_VERSION/rts/lra/lra-coordinator-swarm.jar -Dswarm.http.port=8080 -Dswarm.transactions.object-store-path=../parent &
ID1=$!
((PORT++))
java $(getDebugArgs $PORT) -jar narayana-full-$NARAYANA_VERSION/rts/lra/lra-coordinator-swarm.jar -Dswarm.http.port=8081 -Dswarm.transactions.object-store-path=../subordinate &
ID2=$!
((PORT++))
java $(getDebugArgs $PORT) -jar hotel-service/target/lra-test-swarm.jar -Dswarm.http.port=8082 &
ID3=$!
((PORT++))
java $(getDebugArgs $PORT) -jar flight-service/target/lra-test-swarm.jar -Dswarm.http.port=8083 -Dlra.http.port=8081 &
ID4=$!
((PORT++))
java $(getDebugArgs $PORT) -jar trip-controller/target/lra-test-swarm.jar -Dswarm.http.port=8084 -Dlra.http.port=8080 &
ID5=$!
((PORT++))

if [ $SVRONLY -ne 1 ]; then
    echo "Waiting for all the servers to start"
    sleep 30

    BOOKINGID=$(curl -X POST "http://localhost:8084/?hotelName=TheGrand&flightNumber1=BA123&flightNumber2=RH456" -sS | jq -r ".id")
    curl -X PUT http://localhost:8084/`urlencode $BOOKINGID` -sS | jq
    BOOKINGID=$(curl -X POST "http://localhost:8084/?hotelName=TheGrand&flightNumber1=BA123&flightNumber2=RH456" -sS | jq -r ".id")
    curl -X DELETE http://localhost:8084/`urlencode $BOOKINGID` -sS | jq
fi

if [ $DEBUG -eq 1 ] || [ $SVRONLY -eq 1 ]; then
  echo "Processes are still running ($ID1 $ID2 $ID3 $ID4 $ID5) press any key to end them" && read
fi
kill -9 $ID1 $ID2 $ID3 $ID4 $ID5