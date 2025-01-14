import requests
import json

url = "http://localhost:9090/ngsi-ld/v1/temporal/entities"

payload = json.dumps({
  "id": "urn:ngsi-ld:WeatherObserved:1",
  "type": "WeatherObserved",
  "location": {
    "type": "GeoProperty",
    "value": {
      "type": "Point",
      "coordinates": [
        -4.754444444,
        41.640833333
      ]
    }
  },
  "refDevice": {
    "type": "Relationship",
    "object": "urn:ngsi-ld:Device:TBD"
  },
  "temperature": {
    "type": "Property",
    "value": 5.5,
    "observedAt": "2024-11-30T07:00:00.00Z"
  },
  "@context": [
    "https://raw.githubusercontent.com/smart-data-models/dataModel.Weather/master/context.jsonld"
  ]
})
headers = {
  'Accept': 'application/ld+json',
  'Content-Type': 'application/ld+json'
}

response = requests.request("POST", url, headers=headers, data=payload)

print(response.text)
print(response.status_code)

