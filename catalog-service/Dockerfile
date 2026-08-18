FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY shared-web/pom.xml shared-web/pom.xml

RUN mvn -f shared-web/pom.xml dependency:go-offline

COPY shared-web/src shared-web/src

RUN mvn -f shared-web/pom.xml clean install -DskipTests

COPY catalog-service/pom.xml catalog-service/pom.xml

RUN mvn -f catalog-service/pom.xml dependency:go-offline

COPY catalog-service/src catalog-service/src

RUN mvn -f catalog-service/pom.xml clean package -DskipTests


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/catalog-service/target/*.jar app.jar

RUN useradd -m spring

USER spring

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
