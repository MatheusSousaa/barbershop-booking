FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache tzdata su-exec \
    && addgroup -S -g 10001 app \
    && adduser -S -D -H -u 10001 -G app app \
    && mkdir -p /app/uploads \
    && chown -R app:app /app
ENV TZ=America/Bahia
WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/barbershop-*.jar /app/app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod 755 /app/docker-entrypoint.sh
USER root
EXPOSE 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
