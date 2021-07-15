#!/bin/sh
mkdir -p nginx-certs
openssl req -newkey rsa:2048 -new -nodes -x509 -days 3650 -keyout key.pem -out cert.pem
openssl x509 -in cert.pem -out nginx-certs/cert.crt
openssl rsa -in key.pem -out nginx-certs/cert.key
