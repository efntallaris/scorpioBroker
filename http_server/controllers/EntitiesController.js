class EntitiesController {
    async getAll(req, res) {
        try {
            res.status(200).json({ message: 'Fetched all entities.' });
        } catch (error) {
            res.status(500).json({ error: 'An error occurred while fetching entities.' });
        }
    }

}

module.exports = new EntitiesController();

