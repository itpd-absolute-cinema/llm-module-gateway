# Setup and Run Instructions

This guide explains how to set up, run, and verify the LLM Gateway MVP from a clean checkout.

## Run with Docker Compose

Install Docker with the Compose plugin. From the repository root, copy `.env.example` to `.env`, set a
unique admin key, set `IMAGE_TAG=latest` in `.env`, and start the services:

```sh
docker compose pull
docker compose up -d
```

The gateway listens on the port in `GATEWAY_PORT`. The default provider is a
stub: it echoes the request rather than calling a real model. Test it with:

```sh
curl -fsS http://localhost:8080/chat/completions \
  -H 'Content-Type: application/json' \
  -d '{"model":"demo","messages":[{"role":"user","content":"Hello"}]}'
```

Replace `8080` with your configured port. See the [plugin management API](api/plugin-management.md)
for uploading and enabling plugins. Stop the services with `docker compose down`;
this keeps the database and plugin volumes. See [deployment](deployment.md) for
VM deployments.

`latest` can change. For a reproducible deployment, use the commit tag
published by CI; see [deployment](deployment.md).

To build local changes, set `IMAGE_TAG=local` in `.env`, build the gateway JAR
with JDK 25, then build and start both application images:

```sh
(cd code/service-gateway && ./gradlew :app:bootJar)
docker compose up --build -d
```

The default `local` tag keeps development builds separate from published images.

## Local development without Compose

### 1. Prerequisites

Install Git, JDK 25, Python 3.12 and MongoDB 8. The repository includes the
Gradle Wrapper. Start MongoDB locally on port 27017 before starting the gateway.

### 2. Clone the Repository

```sh
git clone https://github.com/itpd-absolute-cinema/llm-module-gateway.git
cd llm-module-gateway
```

The commands below assume a POSIX shell and begin in the repository root.

### 3. Configure the Environment

Choose a local admin key and a shared directory for plugin files:

```sh
export GATEWAY_ADMIN_API_KEY=local-development-only
export GATEWAY_PLUGINS_DIR="$PWD/.local-plugins"
export PLUGINS_DIR="$GATEWAY_PLUGINS_DIR"
mkdir -p "$PLUGINS_DIR"
```

Do not use the example key outside local development or commit real secrets.

### 4. Set Up the Python Environment

In a terminal, install the Plugin Manager dependencies and run it:

```sh
cd code/plugin-manager
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
uvicorn app.main:app --port 8000
```

The plugin paths are read at startup. Keep this terminal open.

### 5. Build the Java Gateway

In a second terminal, from `code/service-gateway`, run:

```sh
./gradlew :app:build
```

### 6. Start the Application

In the same terminal, export the values from step 3 and run:

```sh
./gradlew :app:bootRun
```

The defaults connect to MongoDB at `localhost:27017`, the Plugin Manager at
`localhost:8000`, and the stub provider. The gateway listens on port 8080.
The gateway logs request content at DEBUG by default; set
`LOGGING_LEVEL_RU_INNO_ITPD_GATEWAY=INFO` to suppress those messages.

### 7. Run the End-to-End Scenario

Use the chat request in [Run with Docker Compose](#run-with-docker-compose).
See the [plugin management API](api/plugin-management.md) for managing filters.
The stub does not call a real model.

### 8. Run Tests

Run the gateway tests with `./gradlew :app:test` from `code/service-gateway`.
See the [prototype instructions](https://github.com/itpd-absolute-cinema/llm-module-gateway/blob/main/prototype/README.md)
for its separate suite.

### 9. Troubleshooting

Check JDK 25, MongoDB, the Plugin Manager, the shared plugin path and the
configured ports if startup or requests fail.

## Expected Result

The gateway accepts requests and returns a response from the stub provider;
enabled plugins process requests and responses.

## Week 4 Follow-up

The [Week 3 delivery action](https://github.com/itpd-absolute-cinema/llm-module-gateway/blob/main/reports/week-03/meeting-report.md#action-points)
and [follow-up note](architecture/plugin-chain.md) describe the earlier prototype.
The prototype has not been verified as a released or customer-accepted MUP.
