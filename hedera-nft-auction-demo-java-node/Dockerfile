#
# build
#

FROM adoptopenjdk:14-jdk-hotspot AS build

# download and cache the version of gradle in use
COPY ./gradle /opt/hedera-nft-auction-demo-java-node/gradle
COPY ./gradlew /opt/hedera-nft-auction-demo-java-node/gradlew

RUN cd /opt/hedera-nft-auction-demo-java-node && ./gradlew

# copy in just enough to cache dependencies
COPY ./build.gradle /opt/hedera-nft-auction-demo-java-node/build.gradle
COPY ./settings.gradle /opt/hedera-nft-auction-demo-java-node/settings.gradle

#RUN cd /opt/hedera-nft-auction-demo-java-node && ./gradlew --no-daemon compileJava

# now, finally copy in the source and build a JAR
COPY ./src /opt/hedera-nft-auction-demo-java-node/src
COPY ./sample-files /opt/hedera-nft-auction-demo-java-node/sample-files

#RUN cd /opt/hedera-nft-auction-demo-java-node && ./gradlew flywayMigrate

#RUN cd /opt/hedera-nft-auction-demo-java-node && ./gradlew jooqGenerate

RUN cd /opt/hedera-nft-auction-demo-java-node && ./gradlew --no-daemon build

CMD "ls" "/opt/hedera-nft-auction-demo-java-node/build/libs/"

FROM adoptopenjdk:14-jre-hotspot

# make a place to put our built JAR and copy it to there
WORKDIR /srv
COPY --from=build /opt/hedera-nft-auction-demo-java-node/build/libs/hedera-nft-auction-demo-1.0.jar /srv/hedera-nft-auction-demo-1.0.jar
COPY --from=build /opt/hedera-nft-auction-demo-java-node/sample-files /srv/sample-files

# setup auction
CMD java "-cp" "hedera-nft-auction-demo-1.0.jar" "com.hedera.demo.auction.node.app.EasySetup" "--name=myToken" "--symbol=./sample-files/gold-base64.txt"

# run
CMD java "-jar" "hedera-nft-auction-demo-1.0.jar"
