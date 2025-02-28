# NGSI-LD Context Source Registrations For Temporal Path: Proof of Concept

This document describes the process of registering a Context Source with an NGSI-LD broker and forwarding all the ngsi-ld queries of the temporal path (/ngsi-ld/v1/temporal/entities) to a third party http server.

## Prerequisites

1. **NGSI-LD Broker**: Ensure an NGSI-LD broker is running and accessible.
2. **Custom HTTP Server**: A custom HTTP server hosted in the `http_server` folder, running on port `9010`.

## Steps

### 1. Run the Custom HTTP Server

Navigate to the `http_server` folder and start the HTTP server. This will act as the endpoint for the Context Source. It will receive ngsi-ld queries and respond according to NGSI-LD spec.

```bash
cd http_server
npm install
node app.js
```

### 2. Register the Context Source

Use the following `curl` command to register the Context Source with the NGSI-LD broker.

```bash
curl --location 'http://{{broker_ip}}:9090/ngsi-ld/v1/csourceRegistrations' \
--header 'Accept: application/ld+json' \
--header 'Content-Type: application/ld+json' \
--data-raw '{
    "id": "urn:ngsi-ld:ContextSourceRegistration:test001",
    "type": "ContextSourceRegistration",
    "information": [
        {
            "entities": [
                {
                    "type": "WeatherObserved"
                }
            ]
        }
    ],
    "operations": ["retrieveTemporal", "queryTemporal"],
    "endpoint": "http://{{http_server_ip}}:9010",
    "@context": [
        "https://raw.githubusercontent.com/smart-data-models/dataModel.Weather/master/context.jsonld"
    ]
}'
```

### 3. Query Temporal Entities

Use the following `curl` command to query temporal entities of type `WeatherObserved`:

```bash
curl --location 'http://{{broker_ip}}:9090/ngsi-ld/v1/temporal/entities?type=WeatherObserved&timerel=between&time=2025-01-01T00%3A00%3A00Z&endTime=2025-01-01T12%3A00%3A00Z' \
--header 'Link: <https://raw.githubusercontent.com/smart-data-models/dataModel.Weather/master/context.jsonld>; rel="http://www.w3.org/ns/json-ld#context"; type="application/ld+json"'
```

This query will be forwarded to the custom HTTP server running at `{{http_server_ip}}:9010`.

## Additional Information

- [NGSI-LD Specifications](https://www.etsi.org/deliver/etsi_gs/CIM/001_099/009/01.05.01_60/gs_cim009v010501p.pdf)
- [Smart Data Models](https://smartdatamodels.org/)
- The HTTP server is designed to behave based on the NGSI-LD specification. It ensures that it follows the NGSI-LD context and data model requirements. For more details, refer to the [NGSI-LD Specifications](https://etsi.org/deliver/etsi_gs/CIM/001_099/009/01.06.01_60/gs_cim009v010601p.pdf).

### HTTP Server Code Structure

The `http_server` folder contains the following structure:

- **controllers/**: Includes endpoints that handle incoming requests. This process receives ngsi-ld queries, processes them, and returns a response in NGSI-LD format. (You can use `req.query` to extract query parameters)
- **routes/**: Defines the routes and maps them to appropriate controllers.
- **services/**: Contains the business logic and helpers to process NGSI-LD queries and generate responses.

