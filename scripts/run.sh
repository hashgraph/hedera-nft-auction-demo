#!/bin/sh

if [ $# -eq 0 ]
  then
    echo "arguments missing. Add compile or image"
    exit
fi

cd ..

if [ "$1" = "compile" ]
  then
    echo "running compiled images"
elif [ "$1" = "image" ]
  then
    echo "pulling images from repository"
else
    echo "invalid argument, should be compile or image"
    exit
fi

sudo docker-compose -f docker-compose-$1.yaml rm -f
sudo docker-compose -f docker-compose-$1.yaml build
sudo docker-compose -f docker-compose-$1.yaml up --remove-orphans -d

echo "Starting log stream, CTRL+C to stop (will leave containers running)"

sudo docker-compose -f docker-compose-$1.yaml logs -f
