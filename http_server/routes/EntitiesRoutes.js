const express = require('express');
const EntitiesController = require('../controllers/EntitiesController');

const router = express.Router();

router.get('/', EntitiesController.getAll);

module.exports = router;

