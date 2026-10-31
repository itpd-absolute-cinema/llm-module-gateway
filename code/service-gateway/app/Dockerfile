FROM eclipse-temurin:25-jdk AS build
ARG JAVA_TOOL_OPTIONS
WORKDIR /src
COPY . .
RUN ./gradlew :app:bootJar --no-daemon

FROM eclipse-temurin:25-jre

RUN apt-get update \
 && apt-get install -y --no-install-recommends python3 \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /src/app/build/libs/gateway.jar gateway.jar

ENTRYPOINT ["java", "-jar", "gateway.jar"]
