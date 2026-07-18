FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY domain ./domain
COPY datasource ./datasource
COPY rehearsal-api ./rehearsal-api

RUN chmod +x gradlew \
    && ./gradlew :rehearsal-api:bootJar -x test -x asciidoctor --no-daemon \
    && rm -f rehearsal-api/build/libs/*-plain.jar

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S app && adduser -S app -G app
USER app

WORKDIR /app
COPY --from=builder /workspace/rehearsal-api/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
