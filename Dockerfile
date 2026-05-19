FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN mvn -q -DskipTests dependency:go-offline

COPY src src

RUN mvn -q clean package -DskipTests

FROM maven:3.9.9-eclipse-temurin-17

WORKDIR /app

COPY --from=builder /build/target/PelisApp-0.0.1-SNAPSHOT.jar /app/pelisapp.jar

EXPOSE 8080

ENV SERVER_ADDRESS=0.0.0.0

ENTRYPOINT ["java", "-jar", "/app/pelisapp.jar"]