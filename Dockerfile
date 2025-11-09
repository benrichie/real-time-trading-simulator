# Build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy Gradle files for dependency caching
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon

# Copy source code and build the JAR
COPY src src
RUN ./gradlew clean bootJar --no-daemon -x test

# Production stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the port (Render will override with $PORT)
EXPOSE ${PORT:-8080}

# Start the app, using $PORT from environment
CMD ["java", "-jar", "app.jar"]