# rtl433-data-pipeline

Pipeline for processing [`rtl_433`](https://github.com/merbanan/rtl_433) messages and turning them into structured events for home automation (e.g. Home Assistant via MQTT), with support for deduplication, fingerprinting, model tracking, and downstream analytics.

> **Status:** Early-stage / personal project. APIs, configuration, and deployment layout may change without notice.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Clone & Build](#clone--build)
- [Running with Docker](#running-with-docker)
    - [Build a Local Image](#build-a-local-image)
    - [Using docker-compose](#using-docker-compose)
- [Configuration](#configuration)
- [Development Notes](#development-notes)
    - [Project Layout](#project-layout)
    - [Running Locally (without Docker)](#running-locally-without-docker)
    - [Testing](#testing)
- [Issue Management & Contributing](#issue-management--contributing)
- [Roadmap & Ideas](#roadmap--ideas)
- [License](#license)
- [Acknowledgements](#acknowledgements)

---

## Overview

This service sits between **rtl_433** (the SDR decoder) and your home-automation ecosystem. It takes decoded RF messages (typically JSON over MQTT or another transport), runs them through a **reactive Kotlin/Spring pipeline**, and produces normalized, deduplicated, and fingerprinted events that are easier to consume downstream.

Typical use case:

1. `rtl_433` listens on 433 MHz (or related bands) and outputs JSON.
2. `rtl433-data-pipeline` ingests those messages.
3. The pipeline dedupes, fingerprints, and classifies devices/events.
4. The results are stored and/or published to systems like **Home Assistant** (often via MQTT).

---

## Features

High-level features (some present, some planned):

- **Reactive ingestion**
    - Kotlin + Spring WebFlux–style pipelines.
    - EventBus-based orchestration of pipeline stages.

- **Deduplication**
    - Detects repeated bursts from devices (e.g. Acurite sensors that send 3 readings per publish).
    - Emits a single canonical message for a burst/window.

- **Fingerprinting**
    - Generates **structural fingerprints** for device payloads.
    - Helps cluster similar devices and identify new/unknown models.
    - Backed by a configurable “fingerprint datafill” concept (include/exclude certain fields).

- **Model & device catalog**
    - Tracks devices by `(model, id)` and optionally by name/type/area.
    - Distinguishes between **unknown**, **recommended**, and **promoted/known** devices.

- **Recommendation & anomaly hooks**
    - Designed to plug into DL4J / ML-based recommendation and anomaly detection engines.
    - Can be extended to suggest likely device types, locations, or detect unusual patterns.

- **Home Automation friendly**
    - Intended to integrate with Home Assistant via MQTT discovery (or generated discovery payloads).
    - Allows richer, typed entities than raw rtl_433 topics.

---

## Architecture

At a high level, the pipeline looks like this:

```text
           +-------------+
           |  rtl_433    |
           | (docker or  |
           |  bare metal)|
           +-------------+
                  |
                  v
           [MQTT / JSON / HTTP]
                  |
                  v
       +-----------------------------+
       | rtl433-data-pipeline       |
       |                             |
       |  1. Ingestion               |
       |  2. Deduplication           |
       |  3. Fingerprinting          |
       |  4. Model / device catalog  |
       |  5. Recommendation / ML     |
       |  6. Publishing (MQTT, etc.) |
       +-----------------------------+
                  |
                  v
        [Home Assistant / DB / Other]
```

Internally, the service is:

- Written in **Kotlin**, using immutable data classes where possible.
- Structured as a **reactive workflow** with an EventBus for orchestration.
- Backed by **Gradle Kotlin DSL** build configuration.
- Designed to live within the broader `io.jrb.labs` ecosystem and work alongside other microservices.

---

## Getting Started

### Prerequisites

You’ll generally want:

- **JDK 21+**
- **Gradle wrapper** (included in the repo, use `./gradlew`)
- **Docker** and **Docker Compose / docker compose** (recommended)
- An SDR + `rtl_433` instance:
    - `rtl_433` sending JSON or MQTT to this pipeline.
- Optionally:
    - **MongoDB** (or whichever persistence backend you wire up for models/events).
    - **MQTT broker** (e.g. Mosquitto) if you’re using MQTT in/out.

### Clone & Build

```bash
git clone https://github.com/brulejr/rtl433-data-pipeline.git
cd rtl433-data-pipeline

# Build the project
./gradlew clean build
```

This will produce a runnable JAR in `build/libs`.

---

## Running with Docker

This project includes a `docker` folder with Docker-related assets (Dockerfile, compose files, etc.). The exact layout may evolve, but the typical pattern looks like:

- `docker/Dockerfile` – container image definition for the pipeline.
- `docker/compose/` – `docker-compose` example(s) tying together:
    - `rtl_433` container
    - MQTT broker
    - MongoDB / other backing services
    - `rtl433-data-pipeline` itself

> **Note:** Paths and filenames may differ slightly in your local clone; use them as a guide.

### Build a Local Image

From the project root:

```bash
# Build the application
./gradlew clean build

# Build Docker image (adjust tag as desired)
docker build -t rtl433-data-pipeline:local -f docker/Dockerfile .
```

### Using docker-compose

A typical `docker compose` setup might look like this (example):

```yaml
version: "3.9"

services:
  mqtt:
    image: eclipse-mosquitto:2
    container_name: mqtt
    restart: unless-stopped
    ports:
      - "1883:1883"

  mongo:
    image: mongo:8
    container_name: mongo
    restart: unless-stopped
    ports:
      - "27017:27017"

  rtl_433:
    image: hertzg/rtl_433:latest
    container_name: rtl_433
    restart: unless-stopped
    devices:
      # Adjust for your host's SDR device
      - "/dev/bus/usb:/dev/bus/usb"
    command: >
      rtl_433
      -F json
      -F mqtt://mqtt:1883,retain=0,devices=rtl_433/

  rtl433-data-pipeline:
    image: rtl433-data-pipeline:local
    container_name: rtl433-data-pipeline
    restart: unless-stopped
    depends_on:
      - mqtt
      - mongo
    environment:
      # Example Spring configuration overrides
      - SPRING_PROFILES_ACTIVE=prod
      - RTL433_MQTT_BROKER_HOST=mqtt
      - RTL433_MQTT_BROKER_PORT=1883
      - MONGODB_URI=mongodb://mongo:27017/rtl433
    ports:
      - "8080:8080"
```

Bring the stack up:

```bash
cd docker/compose
docker compose up -d
```

Adjust service names, env vars, and paths to match the actual compose files in this repo.

---

## Configuration

Configuration is managed via Spring Boot’s usual mechanisms: `application.yml`, profiles, and environment variables.

Typical configuration domains you’ll see:

- **Ingestion**
    - Transport: MQTT / HTTP / other.
    - Topics or endpoints to listen on (e.g. `rtl_433/#`).
    - JSON decoding and error handling.

- **Deduplication**
    - Window size or “burst” duration for treating events as duplicates.
    - Optional model-specific rules (e.g., special handling for Acurite sensors).

- **Fingerprinting**
    - Inclusion/exclusion rules for properties when generating fingerprints.
    - Tuning for how “strict” fingerprints should be.

- **Persistence**
    - MongoDB connection string, database name, and collection names.
    - TTLs / indexes for high-volume collections.

- **Publishing**
    - Outbound MQTT broker configuration.
    - Topic mapping from internal events to Home Assistant or other consumers.

You can override most settings via environment variables when running in Docker, for example:

```bash
docker run   -e SPRING_PROFILES_ACTIVE=prod   -e RTL433_MQTT_BROKER_HOST=mqtt   -e RTL433_MQTT_BROKER_PORT=1883   rtl433-data-pipeline:local
```

For the complete list of configuration options, refer to the `application.yml` and any profile-specific configs in `src/main/resources`.

---

## Development Notes

### Project Layout

Rough layout:

```text
rtl433-data-pipeline/
  docker/                 # Dockerfile / docker-compose examples
  src/
    main/
      kotlin/             # Kotlin source (pipeline, services, events)
      resources/          # application.yml, logging, etc.
    test/
      kotlin/             # Unit & integration tests
  build.gradle.kts        # Gradle Kotlin DSL config
  settings.gradle.kts
  gradle.properties
```

The code is Kotlin-first and intended to use:

- Immutable data classes for domain models.
- Sealed classes for workflow outcomes and event hierarchies.
- A reactive EventBus / workflow engine for each stage.

### Running Locally (without Docker)

You can run the app directly with Gradle:

```bash
./gradlew bootRun
```

Or using the built JAR:

```bash
java -jar build/libs/rtl433-data-pipeline-*.jar
```

Make sure your MQTT broker, Mongo (if used), and rtl_433 are running and configured to talk to this service.

### Testing

Run tests with:

```bash
./gradlew test
```

The goal is to cover:

- Deduplication logic (windowing, edge cases).
- Fingerprinting behavior (include/exclude rules).
- EventBus & workflow orchestration (success/skip/fail paths).
- Any ML / recommendation hooks in isolation.

---

## Issue Management & Contributing

### Filing Issues

Use **GitHub Issues** on this repository to track:

- 🐛 **Bugs** – things that are broken or behave unexpectedly.
- ✨ **Enhancements** – new pipeline stages, new outputs, better configuration, etc.
- 📄 **Documentation** – missing or unclear README sections, config examples, diagrams.
- 🧪 **Testing / CI** – requests for additional test coverage or automation.

When opening an issue, please include:

- Environment (OS, Docker/non-Docker, JDK version).
- How you’re running `rtl_433` and the pipeline (commands, compose file, etc.).
- Logs and, if possible, **redacted** sample payloads.

### Pull Requests

PRs are welcome, especially if you’re building on top of this system in your own lab.

General guidelines:

- Keep changes focused and small.
- Add tests where applicable.
- Update documentation / README if your change adds behavior or config.
- Use clear commit messages (conventional commits are a plus but not required).

---

## Roadmap & Ideas

Some possible next steps for the project:

- **Home Assistant MQTT Discovery**
    - Auto-generate discovery topics for recognized devices.
    - Template-based mapping from fingerprints to HA entities.

- **Anomaly Detection**
    - Integrate DL4J or similar for detecting unusual device patterns.
    - Alerting pipeline for “anomalous” events (frequency, values, gaps).

- **Richer Recommendation Engine**
    - Suggest device names, areas, and types for newly seen `(model, id)` combos.
    - Web or REST UI for promoting suggestions to “known devices”.

- **Observability**
    - Metricized workflow stages (timings, error counts, dedupe hit rates).
    - Structured logging across all steps.

---

## License

This project is licensed under the **MIT License**.  
See [`LICENSE`](./LICENSE) for the full text.

---

## Acknowledgements

- [`rtl_433`](https://github.com/merbanan/rtl_433) – the core SDR decoder this pipeline is built around.
- The broader Home Assistant / MQTT ecosystem for patterns and inspiration.
- The `io.jrb.labs` family of services and libraries that this project is designed to complement.