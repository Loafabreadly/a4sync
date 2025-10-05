FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create directory for mods
RUN mkdir -p /mods

# Copy the jar file
COPY target/a4sync-server-1.0-SNAPSHOT.jar app.jar

# Expose the server port
EXPOSE 8080

# Mount point for mod files
VOLUME ["/mods"]

ENTRYPOINT ["java", "-jar", "app.jar"]
