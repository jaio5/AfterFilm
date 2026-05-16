FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

# Copy only Maven files first to leverage Docker layer cache for dependencies
COPY pom.xml mvnw ./
COPY .mvn .mvn

RUN mvn -B -DskipTests dependency:go-offline

# Copy sources and build
COPY src ./src

RUN mvn -B clean package -DskipTests -Dproject.build.outputTimestamp=20240501

########################################
## Runtime image (slim JRE)
########################################
FROM eclipse-temurin:17-jre

LABEL org.opencontainers.image.title="PelisApp"
LABEL org.opencontainers.image.licenses="MIT"

ENV APP_HOME=/app
WORKDIR ${APP_HOME}

# Create non-root user
RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/* \
	&& addgroup --system app \
	&& adduser --system --ingroup app app || true

# Copy jar from builder
COPY --from=builder /workspace/target/PelisApp-0.0.1-SNAPSHOT.jar ./pelisapp.jar

# Expose port used by the app
EXPOSE 8080

# Tunable Java options (override at runtime)
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV SPRING_PROFILES_ACTIVE=prod

# Healthcheck (requires actuator in production)
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run as non-root
USER app

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/pelisapp.jar"]