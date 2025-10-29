# syntax=docker/dockerfile:1

### Build stage ###
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy build descriptors first to leverage Docker layer cache
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw

# Warm the Maven cache & download deps
RUN ./mvnw -q -B -DskipTests=true dependency:go-offline

# Copy source and build
COPY src src
RUN ./mvnw -q -B -DskipTests=true clean package

# Normalise the jar name for the runtime stage
RUN ./mvnw -q -B -DskipTests=true clean package && \
    cp target/*.jar app.jar

### Runtime stage ###
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built app
COPY --from=build /workspace/app.jar /app/app.jar

# Spring Boot default port
EXPOSE 8080

# Sensible JVM defaults for containers; tweak as needed
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=ecs

# If you use Spring profiles or DB creds, pass them via env at runtime
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
