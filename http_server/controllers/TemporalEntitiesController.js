class TemporalEntitiesController {
    async getAll(req, res) {
        try {
	    console.log({query:req.query})
            const response = [
                {
                    id: "urn:ngsi-ld:WeatherObserved:1555",
                    type: "WeatherObserved",
                    refDevice: {
                        type: "Relationship",
                        object: "urn:ngsi-ld:Device:TBD",
                        instanceId: "urn:ngsi-ld:3f71a3f3-3922-4523-918e-c835b95b86b7"
                    },
                    location: {
                        type: "GeoProperty",
                        value: {
                            type: "Point",
                            coordinates: [-4.754444444, 41.640833333]
                        },
                        instanceId: "urn:ngsi-ld:d65b3bd0-062a-4ba4-b48d-f7f91dcbebb9"
                    },
                    "@context": [
                        "https://raw.githubusercontent.com/smart-data-models/dataModel.Weather/master/context.jsonld",
                        "https://uri.etsi.org/ngsi-ld/v1/ngsi-ld-core-context.jsonld"
                    ]
                }
            ];
	    console.log({"hi":response})
            res.status(200).json(response);
        } catch (error) {
            res.status(500).json({ error: 'An error occurred while fetching temporal entities.' });
        }
    }

}

module.exports = new TemporalEntitiesController();

