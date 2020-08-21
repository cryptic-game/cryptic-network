FROM maven:3.6.3-adoptopenjdk-11 AS builder

WORKDIR /app
COPY . /app/
RUN mvn clean install

FROM adoptopenjdk/openjdk11:alpine-jre

ENV CRYPTIC_HOME /opt/cryptic
ENV DATA_DIR /data

RUN set -o errexit -o nounset \
    && mkdir -p ${DATA_DIR} \

WORKDIR ${DATA_DIR}
COPY --from=builder /app/target/network-0.2.0-jar-with-dependencies.jar ${CRYPTIC_HOME}/network.jar

ENTRYPOINT ["java", "-jar", "/opt/cryptic/network.jar"]
