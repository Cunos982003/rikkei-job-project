FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY backend/audit-service/ ./
RUN set -eux; \
    sed -i 's/\r$//' gradlew; \
    chmod +x gradlew; \
    ./gradlew --no-daemon clean bootJar -x test; \
    jar="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)"; \
    test -n "$jar"; \
    cp "$jar" /workspace/app.jar

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --create-home app \
    && chown -R app:app /app
COPY --from=build --chown=app:app /workspace/app.jar ./app.jar
USER app
EXPOSE 8088
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
