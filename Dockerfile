FROM maven:3.6.0-jdk-8-alpine

WORKDIR /app

ADD . /app/

RUN mvn clean install

CMD java -jar target/network-0.0.3-SNAPSHOT-jar-with-dependencies.jar
