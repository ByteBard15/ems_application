FROM eclipse-temurin:21-jdk-alpine AS runtime-config
WORKDIR /app
COPY ./config/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]

FROM eclipse-temurin:21-jdk-alpine AS runtime-auth
WORKDIR /app
COPY ./auth/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM eclipse-temurin:21-jdk-alpine AS runtime-employee
WORKDIR /app
COPY ./employee/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM eclipse-temurin:21-jdk-alpine AS runtime-gateway
WORKDIR /app
COPY ./gateway/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM eclipse-temurin:21-jdk-alpine AS runtime-discovery
WORKDIR /app
COPY ./discovery/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
