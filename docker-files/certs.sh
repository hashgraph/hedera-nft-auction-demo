#!/bin/sh
mkdir -p nginx-certs
openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout ./nginx-certs/cert.key -out ./nginx-certs/cert.crt
