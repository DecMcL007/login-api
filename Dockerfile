# syntax=docker/dockerfile:1

### Build stage ###
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw -q -B -DskipTests=true dependency:go-offline

COPY src src
RUN ./mvnw -q -B -DskipTests=true clean package && \
    cp target/*.jar app.jar

### Runtime stage ###
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /workspace/app.jar /app/app.jar

EXPOSE 8082
ENV SPRING_PROFILES_ACTIVE=ecs

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
