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

case "$(uname)" in
   CYGWIN*) export NARAYANA_INSTALL_LOCATION=`cygpath -w $(pwd)/narayana-full-5.7.0.Final/` ;;
   *)       export NARAYANA_INSTALL_LOCATION=$(pwd)/narayana-full-5.7.0.Final/ ;;
esac

oc get projects | grep javaone
if [ $? = 0 ]; then
  echo "Waiting for javaone to terminate"
  exit
fi

set -e

# Create a new project
oc new-project javaone

minishift console

read -p "Make sure you create the two storage volumes lra-coordinator-logs and flight-lra-coordinator-logs"

# Deploy the LRA coordinator
cd lra-coordinator && rm -rf target/ && mkdir target
oc new-build --binary --name=lra-coordinator -l app=lra-coordinator
cp $NARAYANA_INSTALL_LOCATION/rts/lra/lra-coordinator-swarm.jar target/
oc start-build lra-coordinator --from-dir=. --follow
oc new-app lra-coordinator -l app=lra-coordinator
oc volume dc/lra-coordinator --add -t persistentVolumeClaim --claim-name lra-coordinator-logs -m /data
oc expose service lra-coordinator
# Deploy the Flight LRA coordinator
oc new-build --binary --name=flight-lra-coordinator -l app=flight-lra-coordinator
cp $NARAYANA_INSTALL_LOCATION/rts/lra/lra-coordinator-swarm.jar target/
oc start-build flight-lra-coordinator --from-dir=. --follow
oc new-app flight-lra-coordinator -l app=flight-lra-coordinator
oc volume dc/flight-lra-coordinator --add -t persistentVolumeClaim --claim-name flight-lra-coordinator-logs -m /data
oc expose service flight-lra-coordinator
cd ..

# Deploy the Flight Service
cd flight-service
oc new-build --binary --name=flight -l app=flight
oc start-build flight --from-dir=. --follow
oc new-app flight -l app=flight
oc expose service flight
cd ..
# Deploy the Hotel Service
cd hotel-service
oc new-build --binary --name=hotel -l app=hotel
oc start-build hotel --from-dir=. --follow
oc new-app hotel -l app=hotel
oc expose service hotel
cd ..
# Deploy the Trip Controller
cd trip-controller
oc new-build --binary --name=trip -l app=trip
oc start-build trip --from-dir=. --follow
oc new-app trip -l app=trip
oc expose service trip
cd ..

# You can then run the client:
echo "Waiting for app to deploy"
sleep 30
set +e
until oc log `oc get pods | grep Running | grep ^trip | awk '{ print $1 }'` | grep "WildFly Swarm is Ready"
do
    echo "Waiting for trip to start"
    sleep 5
done
set -e

BOOKINGID=$(curl -X POST "http://trip-javaone.`minishift ip`.nip.io/" -sS | jq -r ".id")
curl -X PUT http://trip-javaone.`minishift ip`.nip.io/`urlencode $BOOKINGID` -sS | jq

echo -e "\n\n\n"
BOOKINGID=$(curl -X POST "http://trip-javaone.`minishift ip`.nip.io/?hotelName=TheGrand&flightNumber1=BA123&flightNumber2=RH456" -sS | jq -r ".id")
echo "The booking ID for http://trip-javaone.`minishift ip`.nip.io/?hotelName=TheGrand&flightNumber1=BA123&flightNumber2=RH456 was: $BOOKINGID"
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
echo "Confirming with curl -X PUT http://trip-javaone.`minishift ip`.nip.io/`urlencode $BOOKINGID`"
curl -X PUT http://trip-javaone.`minishift ip`.nip.io/`urlencode $BOOKINGID` -sS | jq
echo ""