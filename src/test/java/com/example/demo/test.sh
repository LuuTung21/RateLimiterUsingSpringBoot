#!/bin/bash

for i in {1..25}
do
   # -s: silent, -o /dev/null: discard body, -w: write-out format
   response_code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/test)
   echo "Request #$i: Status $response_code"
   sleep 0.1 # Small delay between requests
done