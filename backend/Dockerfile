
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

RUN apk add --no-cache maven

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

COPY --from=build /app/target/*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
