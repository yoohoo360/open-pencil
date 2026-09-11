# Build stage
FROM maven:3.9-eclipse-temurin-23 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:23-jre
WORKDIR /app

USER root
RUN apt-get update && apt-get install -y curl lsof \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -r spring && useradd -r -g spring spring

COPY --from=build /app/target/*.jar /app/app.jar

USER spring:spring


EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD sh -c 'curl -f http://127.0.0.1:8080/actuator/health || exit 1'

ENTRYPOINT ["java", "-jar", "app.jar"]