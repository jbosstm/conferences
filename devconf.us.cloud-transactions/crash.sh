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

[ $NARAYANA_VERSION ] || NARAYANA_VERSION=5.9.0.Final

echo "Running example with crash"
BOOKINGID=$(curl -X POST "http://trip-transactionsmicroservices.`minishift ip`.nip.io/?hotelName=TheGrand&flightNumber1=BA123&flightNumber2=RH456" -sS | jq -r ".id")
echo "The booking ID for http://trip-transactionsmicroservices.`minishift ip`.nip.io/?hotelName=TheGrand&flightNumber1=BA123&flightNumber2=RH456 was: $BOOKINGID"
cd lra-coordinator
oc start-build lra-coordinator --from-dir=. --follow
cd -
echo "Waiting for the coordinator to recover"
sleep 15
set +e
until oc log `oc get pods | grep Running | grep ^lra-coordinator | awk '{ print $1 }'` | grep "WildFly Swarm is Ready"
do
    echo "Waiting for lra to start"
    sleep 5
done
set -e

echo -e "\n\n\n"
echo "Confirming with curl -X PUT http://trip-transactionsmicroservices.`minishift ip`.nip.io/`urlencode $BOOKINGID`"
curl -X PUT http://trip-transactionsmicroservices.`minishift ip`.nip.io/`urlencode $BOOKINGID` -sS | jq
echo "Ran fine"
echo ""
