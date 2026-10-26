# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -ntp dependency:go-offline

COPY lombok.config ./
COPY src src
# Tests run in the project gates (./mvnw verify), not in the image build.
RUN ./mvnw -B -ntp package -DskipTests \
    && cp target/events-backend-*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app

COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
