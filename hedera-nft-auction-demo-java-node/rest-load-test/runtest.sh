#!/bin/sh

rm -f AssertionResults.xml
rm -f GraphResults.csv
rm -f SummaryReport.csv

/Volumes/Samsung_X5/dev/github.com/greg/apache-jmeter-5.3/bin/jmeter -n -f \
-Jauctionid=44 \
-Jbidaccount=0.0.2167090 \
-Jnumthreads=20 \
-Jrampup=60 \
-Jduration=150 \
-Jhost=localhost \
-Jprotocol=https \
-Jport=8081 \
-t client-api-test.jmx
