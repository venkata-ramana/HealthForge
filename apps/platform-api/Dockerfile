FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

ENV SERVER_PORT=8080

COPY --from=build /workspace/target/platform-api-0.1.0-SNAPSHOT.jar /app/platform-api.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/platform-api.jar"]
