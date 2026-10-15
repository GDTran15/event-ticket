FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S eventhub && adduser -S eventhub -G eventhub
COPY --from=build --chown=eventhub:eventhub /workspace/target/eventhub-0.0.1-SNAPSHOT.jar app.jar
USER eventhub
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
