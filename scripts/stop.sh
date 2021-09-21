#!/bin/sh

if [ $# -eq 0 ]
  then
    echo "argument missing. Add compile or image"
    exit
fi

cd ..

sudo docker-compose -f docker-compose-$1.yaml down --remove-orphans
