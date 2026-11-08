# --- Build ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# --- Runtime ---
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/co2-agent-1.0.0.jar app.jar
# Il piano gratuito di Render ha 512 MB di RAM
ENV JAVA_TOOL_OPTIONS="-Xmx350m"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
