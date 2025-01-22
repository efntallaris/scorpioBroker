const express = require('express');
const bodyParser = require('body-parser');
const temporalEntitiesRoutes = require('./routes/TemporalRoutes');
const entitiesRoutes = require('./routes/EntitiesRoutes');

const app = express();
const PORT = 9010;

app.use(bodyParser.json());

app.use('/ngsi-ld/v1/temporal/entities', temporalEntitiesRoutes);
app.use('/ngsi-ld/v1/entities', entitiesRoutes);

app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
