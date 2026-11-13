FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/lane-optimizer-1.0.0.jar app.jar
COPY entrypoint.sh /app/entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["/bin/sh", "/app/entrypoint.sh"]
