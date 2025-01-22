// routes/temporalEntitiesRoutes.js
const express = require('express');
const TemporalEntitiesController = require('../controllers/TemporalEntitiesController');

const router = express.Router();

router.get('/', TemporalEntitiesController.getAll);

module.exports = router;

