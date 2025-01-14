# ScorpioBroker Clone

This repository is a **modified version of [ScorpioBroker](https://github.com/ScorpioBroker/ScorpioBroker)**.  
It introduces functionality to **forward temporal queries from source registrations** to another broker or process.

---

## Features

- **Temporal Query Forwarding**  
  Allows seamless redirection of temporal queries between brokers or external processes.

---

## Getting Started

### Prerequisites
Ensure you have the following installed:
- **Maven**
- **Docker** and **Docker Compose**

---

### Build and Run

Follow these steps to build and run the broker:

1. **Build the project with Maven:**
   ```bash
   sudo mvn clean package -DskipTests -Pdocker
   ```

2. **Start the broker using Docker Compose:**
   ```bash
   sudo docker compose -f docker-compose-dist.yml up --build
   ```

3. **Access the broker:**  
   The broker is now running on port **9090**.

---

### Additional Resources
For more details about the original project, visit the [ScorpioBroker GitHub repository](https://github.com/ScorpioBroker/ScorpioBroker).

---
