# Setup and Run Instructions

This guide explains how to set up, run, and verify the LLM Gateway MVP from a clean checkout.

## 1. Prerequisites

Install the following tools:

- Git
- Java Development Kit (JDK) 25
- Python, using the version required by the project
- Gradle, if the repository does not include the Gradle Wrapper

Check the installed versions:

```bash
git --version
java --version
python --version
```

If your system uses `python3` instead of `python`, replace `python` with `python3` in the commands below.

## 2. Clone the Repository

```bash
git clone https://github.com/itpd-absolute-cinema/llm-module-gateway.git
cd llm-module-gateway
```

Follow these instructions from the repository root.

## 3. Configure the Environment

Review the example configuration files and the environment variables documented by the project.

If the application requires environment variables or API credentials:

1. Copy the provided example configuration to the expected local configuration file.
2. Set the required values locally.
3. Do not commit credentials, API keys, or other secrets.

The application should fail with a clear error message when required configuration is missing.

## 4. Set Up the Python Environment

Create and activate a virtual environment:

**Linux / macOS**

```bash
python3 -m venv .venv
source .venv/bin/activate
```

**Windows PowerShell**

```powershell
py -m venv .venv
.venv\Scripts\Activate.ps1
```

Install the Python dependencies using the dependency manifest provided by the repository.

For example, if the relevant component has a `requirements.txt` file:

```bash
python -m pip install -r path/to/requirements.txt
```

Replace the example path with the actual path to the dependency file.

## 5. Build the Java Gateway

From the directory containing the Gradle Wrapper, run:

**Linux / macOS**

```bash
./gradlew build
```

**Windows**

```powershell
.\gradlew.bat build
```

If the project uses a root Gradle build, run these commands from the repository root. Otherwise, run them from the Gateway module directory.

The build must complete successfully before proceeding.

## 6. Start the Application

Start the Python Interpreter and Java Gateway using the launch commands defined by the repository.

If they run as separate processes, start the Interpreter first and then the Gateway. Keep their terminal sessions open while testing.

Use the actual application entry points and configuration documented in the repository. Do not assume default ports or entry-point filenames.

## 7. Run the End-to-End Scenario

Send a request through the supported client interface, such as the project CLI or the documented HTTP endpoint.

Verify that:

1. The Java Gateway accepts the request.
2. The request filter plugin processes the incoming request.
3. The routing plugin forwards the request to the configured target LLM.
4. The response filter plugin processes the target's response.
5. The client receives the final processed response.

Use a test LLM endpoint or mock server when appropriate so that the scenario can be verified without relying on a live external service.

## 8. Run Tests

Run the Java and Python tests using the commands configured by the project.

For example, a Gradle project may use:

```bash
./gradlew test
```

A Python project using pytest may use:

```bash
python -m pytest
```

Run the commands from the appropriate module directories and confirm that all required tests pass.

## 9. Troubleshooting

- **Java version error:** verify that JDK 25 is installed and selected.
- **Python import error:** activate the virtual environment and install the component's dependencies.
- **Connection error:** verify that the Interpreter and Gateway are running and that their configured addresses match.
- **LLM request failure:** verify the target endpoint and any required credentials.
- **Plugin loading error:** check the plugin configuration, file paths, and plugin interface.

## Expected Result

A clean checkout can be configured and started by following this guide. A client request passes through the Java Gateway and the Python plugin chain, reaches the configured target LLM, and returns the processed response to the client.

All commands and configuration values in this guide must match the current repository implementation.