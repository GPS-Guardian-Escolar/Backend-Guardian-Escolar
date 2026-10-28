FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system guardian && useradd --system --gid guardian guardian
COPY --from=build /workspace/target/*.jar app.jar
RUN chown guardian:guardian app.jar
USER guardian
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
