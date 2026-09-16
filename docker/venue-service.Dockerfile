# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY tourism-common/pom.xml tourism-common/pom.xml
COPY tourism-pojo/pom.xml tourism-pojo/pom.xml
COPY tourism-order-service/pom.xml tourism-order-service/pom.xml
COPY tourism-coupon-service/pom.xml tourism-coupon-service/pom.xml
COPY tourism-user-service/pom.xml tourism-user-service/pom.xml
COPY tourism-gateway/pom.xml tourism-gateway/pom.xml
COPY tourism-venue-service/pom.xml tourism-venue-service/pom.xml
COPY tourism-common/src tourism-common/src
COPY tourism-pojo/src tourism-pojo/src
COPY tourism-venue-service/src tourism-venue-service/src

RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw \
    && ./mvnw -B -pl tourism-venue-service -am package -DskipTests

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/tourism-venue-service/target/tourism-venue-service-1.0-SNAPSHOT.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
