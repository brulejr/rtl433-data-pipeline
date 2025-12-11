# MongoDB Docker Setup

This provides a containerized MongoDB database service using Docker.

### Usage
Run using the Docker compose plugin.

```bash
docker compose up -d
```

Or, run using Docker Compose itself.
```bash
docker-compose up -d
```

### Services
Service     | Port  |
------------|-------|
mongodb     | 27017 |


### Volumes

Volume          | Description
----------------|--------------------------
`mongodb_data`  | The MongoDB data location


### Resources

TBD

### License

The project is licensed under the Apache-2.0 License.
