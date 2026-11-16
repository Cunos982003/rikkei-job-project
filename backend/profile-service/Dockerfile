FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY backend/profile-service/ ./
RUN set -eux; \
    sed -i 's/\r$//' gradlew; \
    chmod +x gradlew; \
    ./gradlew --no-daemon clean bootJar -x test; \
    jar="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)"; \
    test -n "$jar"; \
    cp "$jar" /workspace/app.jar

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --create-home app \
    && mkdir -p /app/uploads/logos /app/uploads/recruitments \
    && chown -R app:app /app
COPY --from=build --chown=app:app /workspace/app.jar ./app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
