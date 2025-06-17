FROM openjdk:17
WORKDIR /app
COPY build/libs/*.jar app.jar
COPY src/main/resources/application.yml application.yml
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.location=file:/app/application.yml"]
