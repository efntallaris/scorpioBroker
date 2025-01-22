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
# Start your server (e.g., using Python's http.server module or a custom script)
python3 -m http.server 9010
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

- Replace `{{broker_ip}}` with the IP address of your NGSI-LD broker.
- Replace `{{http_server_ip}}` with the IP address where your HTTP server is running.


## Additional Information

- [NGSI-LD Specifications](https://etsi.org/deliver/etsi_gs/CIM/001_099/009/01.06.01_60/gs_cim009v010601p.pdf)
- [Smart Data Models](https://smartdatamodels.org/)

This proof of concept demonstrates that the NGSI-LD registration and the custom HTTP server integration are functioning as expected.
