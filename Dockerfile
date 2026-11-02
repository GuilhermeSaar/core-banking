FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/

RUN --mount=type=cache,target=/root/.m2 ./mvnw --batch-mode -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre

RUN groupadd --system spring && useradd --system --gid spring spring

WORKDIR /app

COPY --from=build --chown=spring:spring /workspace/target/*.jar app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
