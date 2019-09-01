#!/bin/bash
endpoint=https://62pu6qnh23.execute-api.ap-northeast-1.amazonaws.com/prod/metrics

while :; do
  v1=$(($RANDOM & 100))
  v2=$(($RANDOM & 100))
  v3=$(($RANDOM & 100))
  v4=$(($RANDOM & 100))
  curl -d "{\"firstName\": \"John\", \"lastName\": \"Doe\", \"values\" : \"$v1,$v2,$v3,$v4\" }" $endpoint
  echo 
  sleep 60
done
