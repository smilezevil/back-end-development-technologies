FROM eclipse-temurin:21-jre

WORKDIR /app
COPY target/*.jar app.jar

RUN useradd --system --no-create-home spring
USER spring

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]