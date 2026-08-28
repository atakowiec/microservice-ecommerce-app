FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY shared-web/pom.xml shared-web/pom.xml

RUN mvn -f shared-web/pom.xml dependency:go-offline

COPY shared-web/src shared-web/src

RUN mvn -f shared-web/pom.xml clean install -DskipTests

COPY api-gateway/pom.xml api-gateway/pom.xml

RUN mvn -f api-gateway/pom.xml dependency:go-offline

COPY api-gateway/src api-gateway/src

RUN mvn -f api-gateway/pom.xml clean package -DskipTests


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/api-gateway/target/*.jar app.jar

RUN useradd -m spring

USER spring

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "app.jar"]
