#!/bin/sh
if [ $# -eq 0 ]
  then
    echo "argument missing. Add compile or image"
    exit
fi

cd ..

if [ "$1" = "compile" ]
  then
    echo "logs for compiled images"
elif [ "$1" = "image" ]
  then
    echo "logs for repository images"
else
    echo "invalid argument, should be compile or image"
    exit
fi

sudo docker-compose -f docker-compose-$1.yaml logs -f --tail=10
