FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create directories
RUN mkdir -p /mods /config && \
    addgroup -S a4sync && \
    adduser -S -G a4sync a4sync && \
    chown -R a4sync:a4sync /mods /config /app

# Copy files
COPY target/a4sync-server-1.0-SNAPSHOT.jar app.jar
COPY src/main/resources/application-example.properties /config/application.properties

# Expose the server port
EXPOSE 8080

# Mount points
VOLUME ["/mods", "/config"]

# Switch to non-root user
USER a4sync

# Set default JVM options
ENV JAVA_OPTS="-Xmx2G -Xms512M"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.config.location=file:/config/application.properties"]
