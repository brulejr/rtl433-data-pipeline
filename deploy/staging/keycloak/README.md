# Setup KeyCloak Staging Server
Keycloak is an open source Identity and Access Management solution targeted towards modern applications and services.

A staging version server is provided in this repository. It should be run on a Linux staging server using Docker Compose.

# Running the server
To start the development service, run the following command from this directory:
```shell
docker compose up -d
```

To stop the development service, run the following command from this directory:
```shell
docker compose down
```

Running the following command will tell if the service is up and running:
```shell
docker ps
```

To view the logs of the KeyCloak server, run the following command from this directory:
```shell
docker logs -f keycloak
```

# Administration Console
The administration console can be accessed at the following URL:
- http://localhost:8181/auth/admin

The administrative credentials are:
- Username: `cat secret/keycloak_admin_userid.txt`
- Password: `cat secret/keycloak_admin_passwd.txt`

# Configuring KeyCloak server
This staging server is pre-configured with a realm named `rtl433dp`. Details of the realm can be found in the
`import/rtl433p-realm.json` file.

In summary, the following users are created:

| Username   | Password   | Roles  |
|:-----------|:-----------|:-------|
| testadmin  | testadmin  | admin  |
| testuser   | testuser   | user   |
| testsysadm | testsysadm | sysadm |

The roles assigned to the users have the following permissions:

| Permission               | Role - sysadm | Role - admin | Role - user | 
|:-------------------------|:-------------:|:------------:|:-----------:|
| `model:get`              |       X       |      X       |      X      |
| `model:list`             |       X       |      X       |      X      |
| `model:search`           |       X       |      X       |      X      |
| `model:update`           |       X       |      X       |             |
| `knowndevice:list`       |       X       |      X       |      X      |
| `recommendation:list`    |       X       |      X       |      X      |
| `recommendation:promote` |       X       |      X       |             |
| `actuator:read`          |       X       |      X       |      X      |
| `actuator:admin`         |       x       |              |             |

Note that role are an internal concept of KeyCloak and are not exposed to the application. Only permissions are granted
based on the roles assigned to the user.

## Additional Manual Setup
The following steps are required to configure the OAuth 2.0 client for the application.

- Navigate to the **Clients** panel. Select the **rtl433dp-api** client. Set the following settings:

| Field               | Value                            |
|:--------------------|:---------------------------------|
| Valid redirect URIs | http://{{staging}}:5001/callback |
| Web origins         | http://{{staging}}:5001            |

# Bruno OAuth 2.0 Configuration
The following configuration is required for the OAuth 2.0 authentication flow.

| Field | Value                                                                 |
|:------|:----------------------------------------------------------------------|
| Grant Type | `Authorization Code`                                                  |
| Callback URL | `{{rtl433_data_pipeline_url}}/login/oauth2/code/keycloak`                |
| Authorization URI | `{{keycloak_url}}/auth/realms/rtl433dp/protocol/openid-connect/auth`  |
| Access Token URL | `{{keycloak_url}}/auth/realms/rtl433dp/protocol/openid-connect/token` |
| Client ID | `rtl433dp-api`                                                        |
| Client Secret | `<super-secret>`                                                       |
| Add Credentials to | `Base Auth Header`                                                |

Note that both the `{{rtl433-data-pipeline-url}}` and `{{keycloak-url}}` variables are defined in the selected environment.

# Resources

Articles
- [Keycloak with Spring Boot and Kotlin- Introduction](https://codersee.com/keycloak-with-spring-boot-and-kotlin-introduction/)