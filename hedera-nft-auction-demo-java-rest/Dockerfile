#
# build
#

FROM adoptopenjdk:14-jdk-hotspot AS build

# download and cache the version of gradle in use
COPY ./gradle /opt/hedera-nft-auction-demo-java-rest/gradle
COPY ./gradlew /opt/hedera-nft-auction-demo-java-rest/gradlew

RUN cd /opt/hedera-nft-auction-demo-java-rest && ./gradlew

# copy in just enough to cache dependencies
COPY ./build.gradle /opt/hedera-nft-auction-demo-java-rest/build.gradle
COPY ./settings.gradle /opt/hedera-nft-auction-demo-java-rest/settings.gradle

#RUN cd /opt/hedera-nft-auction-demo-java-rest && ./gradlew --no-daemon compileJava

# now, finally copy in the source and build a JAR
COPY ./src /opt/hedera-nft-auction-demo-java-rest/src

#RUN cd /opt/hedera-nft-auction-demo-java-rest && ./gradlew --no-daemon flywayMigrate

#RUN cd /opt/hedera-nft-auction-demo-java-rest && ./gradlew jooqGenerate

RUN cd /opt/hedera-nft-auction-demo-java-rest && ./gradlew --no-daemon build

CMD "ls" "/opt/hedera-nft-auction-demo-java-rest/build/libs/"

FROM adoptopenjdk:14-jre-hotspot

# make a place to put our built JAR and copy it to there
WORKDIR /srv
COPY --from=build /opt/hedera-nft-auction-demo-java-rest/build/libs/hedera-nft-auction-demo-1.0.jar /srv/hedera-nft-auction-demo-1.0.jar

# run the web service
CMD java "-jar" "hedera-nft-auction-demo-1.0.jar"
EXPOSE 8081
