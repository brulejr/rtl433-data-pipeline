# RTL433 Setup
Create the directory structure
```bash
mkdir -p rtl_433/mosquitto
```
Create the following `docker-compose.yml` file on a Raspberry Pi:
```yaml
version: "3"

services:

  mosquitto:
    image: eclipse-mosquitto
    hostname: mosquitto
    container_name: mosquitto
    restart: unless-stopped
    ports:
      - "1883:1883"
      - "9001:9001"
    volumes:
      - ./mosquitto.conf:/mosquitto/config/mosquitto.conf

  rtl_433:
    container_name: rtl_433
    image: hertzg/rtl_433:latest
    restart: unless-stopped
    devices:
      - "/dev/bus/usb/002/006"
    command:
      - "-Mtime:unix:usec:utc"
      - "-Mlevel"
      - "-Fmqtt://mosquitto:1883,retain=1"
```
Create the following `mosquitto/mosquitto.conf` file
```yaml
allow_anonymous true
listener 1883
listener 9001
protocol websockets
persistence true
persistence_file mosquitto.db
persistence_location /mosquitto/data/
```
Start the service by running the following
```bash
docker compose up -d
```

# Resources
- [Docker - RTL_433 Image](https://hub.docker.com/r/hertzg/rtl_433)
- [GitHub - rtl_433_docker](https://github.com/hertzg/rtl_433_docker)
- [GitHub - rtl_433](https://github.com/merbanan/rtl_433)
- [Documentation - rtl_433](https://triq.org/rtl_433/)