FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY target/territorio-de-ninos-0.0.1-SNAPSHOT.jar app.jar
RUN chown appuser:appgroup /app/app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
