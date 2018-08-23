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

BOOKINGID=$(curl -X POST "http://localhost:8084/?hotelName=TheGrand&flightNumber1=BA123&flightNumber2=RH456" -sS | jq -r ".id")
echo "curl -X PUT http://localhost:8084/`urlencode $BOOKINGID` -sS | jq"

#BOOKINGID=$(curl -X POST "http://localhost:8084/?hotelName=TheGrand&flightNumber1=BA123&flightNumber2=RH456" -sS | jq -r ".id")
#curl -X DELETE http://localhost:8084/`urlencode $BOOKINGID` -sS | jq

