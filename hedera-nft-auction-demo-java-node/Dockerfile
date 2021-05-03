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

# now, finally copy in the source and build a JAR
COPY ./src /opt/hedera-nft-auction-demo-java-node/src
COPY ./sample-files /opt/hedera-nft-auction-demo-java-node/sample-files

RUN cd /opt/hedera-nft-auction-demo-java-node && ./gradlew --no-daemon assemble

FROM adoptopenjdk:14-jre-hotspot

RUN mkdir -p /srv

# make a place to put our built JAR and copy it to there
COPY --from=build /opt/hedera-nft-auction-demo-java-node/build/libs/hedera-nft-auction-demo-node-1.0.jar /srv/hedera-nft-auction-demo-node-1.0.jar

WORKDIR /demo

CMD "java" "-jar" "/srv/hedera-nft-auction-demo-node-1.0.jar"
