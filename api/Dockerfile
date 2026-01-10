FROM gradle:8.14.3-jdk21 AS build
WORKDIR /app
# Resolve dependencies in their own layer so code changes don't download them again.
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
RUN gradle dependencies --no-daemon -q > /dev/null
COPY src src
RUN gradle installDist --no-daemon -q -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/install/habits-api ./
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=UTC"
EXPOSE 8080
ENTRYPOINT ["/app/bin/habits-api"]
