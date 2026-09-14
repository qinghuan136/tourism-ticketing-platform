# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY tourism-common/pom.xml tourism-common/pom.xml
COPY tourism-pojo/pom.xml tourism-pojo/pom.xml
COPY tourism-server/pom.xml tourism-server/pom.xml
COPY tourism-common/src tourism-common/src
COPY tourism-pojo/src tourism-pojo/src
COPY tourism-server/src tourism-server/src

RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw \
    && ./mvnw -B -pl tourism-server -am package -DskipTests

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY --from=build /workspace/tourism-server/target/tourism-server-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
