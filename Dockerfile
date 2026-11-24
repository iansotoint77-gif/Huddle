FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM tomcat:10.1-jdk17
RUN groupadd --system tomcat \
    && useradd --system --gid tomcat --home-dir /usr/local/tomcat --shell /usr/sbin/nologin tomcat \
    && chown -R tomcat:tomcat /usr/local/tomcat

COPY --from=build --chown=tomcat:tomcat /build/target/huddle.war /usr/local/tomcat/webapps/ROOT.war

ENV TZ=Europe/Madrid
EXPOSE 8080
USER tomcat