# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline -B
COPY src/ src/
RUN ./mvnw -q package -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S resilire && adduser -S resilire -G resilire
COPY --from=build /build/target/*.jar app.jar
USER resilire
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]
