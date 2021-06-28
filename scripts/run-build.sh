#!/bin/sh

cd ..

sudo docker-compose rm -f
sudo docker-compose --profile compile up --remove-orphans -d

echo "Starting log stream in 2s, CTRL+C to stop (will leave containers running)"

sleep 2
sudo docker-compose logs -f --tail=10
