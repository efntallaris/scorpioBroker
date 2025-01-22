# NGSI-LD Context Source Registration: Proof of Concept

This document describes the process of registering a Context Source with an NGSI-LD broker and validating its functionality using a custom HTTP server.

## Prerequisites

1. **NGSI-LD Broker**: Ensure an NGSI-LD broker is running and accessible.
2. **Custom HTTP Server**: A custom HTTP server hosted in the `http_server` folder, running on port `9010`.

## Steps

### 1. Run the Custom HTTP Server

Navigate to the `http_server` folder and start the HTTP server. This will act as the endpoint for the Context Source.

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

### 3. Query Temporal Entities

Use the following `curl` command to query temporal entities of type `WeatherObserved`:

```bash
curl --location 'http://{{broker_ip}}:9090/ngsi-ld/v1/temporal/entities?type=WeatherObserved&timerel=between&time=2025-01-01T00%3A00%3A00Z&endTime=2025-01-01T12%3A00%3A00Z' \
--header 'Link: <https://raw.githubusercontent.com/smart-data-models/dataModel.Weather/master/context.jsonld>; rel="http://www.w3.org/ns/json-ld#context"; type="application/ld+json"'
```

This query will be forwarded to the custom HTTP server running at `{{http_server_ip}}:9010`.

## Additional Information

- [NGSI-LD Specifications](https://etsi.org/deliver/etsi_gs/CIM/001_099/009/01.06.01_60/gs_cim009v010601p.pdf)
- [Smart Data Models](https://smartdatamodels.org/)

This proof of concept demonstrates that the NGSI-LD registration and the custom HTTP server integration are functioning as expected.
