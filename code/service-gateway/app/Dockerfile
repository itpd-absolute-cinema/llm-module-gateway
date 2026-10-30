FROM eclipse-temurin:25-jre

RUN apt-get update \
 && apt-get install -y --no-install-recommends python3 \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY build/libs/gateway.jar gateway.jar

ENTRYPOINT ["java", "-jar", "gateway.jar"]