# Vital Air · Hyper-Local Air Quality Intelligence & Navigation

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square&logo=openjdk)](https://oracle.com/java/)
[![Spring Boot 3.3.4](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Render Deploy](https://img.shields.io/badge/Render-Deployment%20Ready-black.svg?style=flat-square&logo=render)](https://render.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

Vital Air is a high-performance **Hyper-Local Air Quality Intelligence & Navigation Platform** built with **Java 21 & Spring Boot 3.3.4**. It predicts real-time Air Quality Index (AQI) in blind spots between official monitoring stations, generates spatial Gaussian pollution heatmaps, defines multi-station AQI severity zones, and calculates **ML-Optimized Safe Routes** to minimize human pollution exposure during commutes.

---

## 🚀 Live Demo & Render Deployment

> 📌 **Live Render Web Application Link:**
> 
> 🔗 **[https://vital-air-java.onrender.com](https://vital-air-java.onrender.com)** *(Paste your Render service URL here after deployment)*

---

## 📸 Key Application Features & UI

### 1. Main Dashboard & Spatial Gaussian Heatmap
A modern 3-column dark dashboard equipped with CartoDB Voyager map tiles and smooth 2D Gaussian spatial interpolation for environmental AQI density.

```
+-----------------------------------------------------------------------------------+
|  [ Vital Air ]   Delhi-NCR                         [ Live Map ] [ Deep Insights ] |
+------------------+-----------------------------------------------+----------------+
| CONTROLS         | MAP VIEW                                      | LIVE DATA      |
| • Region Switch  | 🟢 Zone 1 Good (0-50)  🟡 Zone 2 Moderate     | • Live AQI     |
| • Start / Dest   | 🟠 Zone 3 Sensitive   🔴 Zone 4 Unhealthy     | • Pollutants   |
| • Safe Routing   | 🟣 Zone 5 Severe      🟤 Zone 6 Hazardous     | • 24h Forecast |
| • Simulator      | 🚗 Driving Simulator Active                   | • Hotspots     |
+------------------+-----------------------------------------------+----------------+
```

### 2. Realistic Vehicle Driving Simulator & Zone Entry Alerts
Simulate real commuting trips between origin and destination with real-time speed, distance telemetry, car heading rotation, and **instant Danger Toast Notifications** when crossing into dangerous AQI zones (`Zone 4 Unhealthy`, `Zone 5 Very Unhealthy`, `Zone 6 Hazardous`).

- 🚨 **Danger Warning Toast**: `"HIGH HAZARDOUS EXPOSURE (AQI 220)! Roll up windows & enable AC internal recirculation."`
- ⚠️ **Caution Warning Toast**: `"Entering Zone 3 - Sensitive Air Quality (AQI 135)."`
- 🟢 **Safe Zone Confirmation**: `"Entering Zone 1 - Satisfactory Clean Air Zone."`

### 3. ML-Optimized Safe Route vs. Direct Route
- 🔴 **Direct Route**: Direct path polyline showing higher pollution exposure.
- 🟢 **ML Safe Route**: Solid mint green path rerouting around high-AQI hotspots to achieve up to **35% reduction in inhaled pollution**.

---

## ✨ Key Technical Highlights

- **Spatial Interpolation Engine**: Built with a clean `Strategy` design pattern supporting **IDW (Inverse Distance Weighting)**, **RBF (Radial Basis Function)**, and **Kriging** algorithms for grid prediction down to **20m resolution**.
- **Multi-Provider Failover Architecture**: Automatic fallback chain across **OpenWeatherMap**, **OpenAQ**, **NASA FIRMS**, **TomTom Traffic**, and **Open-Meteo** (keyless free tier).
- **JWT Security & User Auth**: Statetess authentication with Spring Security & JWT (`/api/auth/register`, `/api/auth/login`).
- **High Efficiency Caching & Persistence**: JPA/Hibernate ORM with Spring Data JPA for PostgreSQL (Production) and H2 in-memory DB (Development).
- **Scheduled Background Collectors**: `@Scheduled` tasks automatically fetch ground station sensor data and update spatial history asynchronously.

---

## 🛠 Tech Stack

- **Backend Framework**: Java 21, Spring Boot 3.3.4, Spring MVC, Spring Data JPA, Spring Security, JWT (jjwt)
- **Frontend Stack**: Single Page Application (SPA), HTML5, Vanilla CSS3 (Custom Glassmorphism Design System), Javascript (ES6+)
- **Map & Visualization**: Leaflet.js 1.9.4, CartoDB Voyager Tiles, `Leaflet.heat` (Gaussian spatial interpolation), Chart.js
- **Database**: PostgreSQL 16 (Production) / H2 Database (Development)
- **Build & Cloud Tooling**: Apache Maven, Docker, Docker Compose, Render Blueprint (`render.yaml`)

---

## 📐 Project Architecture

```
vital-air-backend/
├── src/main/java/com/vitalair/
│   ├── config/              # Configuration Properties, CORS, Caching, RestClient
│   ├── controller/          # Thin REST Endpoints (Predict, Heatmap, Zones, Route, Auth)
│   ├── dto/                 # Immutable Request & Response DTOs
│   ├── entity/              # JPA Database Entities (User, SensorReading, AqiZone, RouteQuery)
│   ├── exception/           # Custom Exceptions & Global Exception Handler
│   ├── repository/          # Spring Data JPA Repositories
│   ├── security/            # JWT Authentication Filter & Token Provider
│   ├── service/             # Core Business Logic & Spatial Interpolation Strategies
│   └── util/                # EPA AQI Breakpoint Math & Haversine Distance Calculators
├── src/main/resources/
│   ├── application.yml      # Base Configuration & Regional Reference Data
│   ├── application-prod.yml # Production Environment Overrides (Render / Postgres)
│   └── static/index.html    # Single Page Frontend UI (Bundled Deployment)
├── Dockerfile               # Production Containerization Specification
├── docker-compose.yml       # Local Dev Setup (Postgres + Backend)
├── render.yaml              # Render Deployment Blueprint
└── pom.xml                  # Maven Project Dependencies
```

---

## 🔌 API Endpoint Reference

All public read endpoints return structured JSON data:

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/predict/{lat}/{lon}` | Returns hyper-local AQI prediction and pollutant breakdown. |
| `GET` | `/api/forecast?lat=&lon=&hours=` | 24-hour hourly AQI prediction curve. |
| `GET` | `/api/heatmap?region=` | 2D Spatial interpolation points for map rendering. |
| `GET` | `/api/zones?region=` | Concentric and station-based regional severity zones. |
| `GET` | `/api/route/safe?start_lat=&start_lon=&end_lat=&end_lon=` | Calculates Direct vs. ML-Optimized Safe Route waypoints. |
| `GET` | `/api/sensors?region=` | Ground-truth sensor reading points for the active region. |
| `GET` | `/api/hotspots?region=` | Top high-AQI pollution hotspots. |
| `GET` | `/api/locations/search?query=` | Fast city and waypoint autocomplete. |
| `POST` | `/api/auth/register` | Register new user account. |
| `POST` | `/api/auth/login` | Authenticate user and receive JWT bearer token. |
| `GET` | `/actuator/health` | Service health status check. |

---

## 💻 Local Setup & Development

### Method 1: Using Docker Compose (Recommended)

1. Clone the repository:
   ```bash
   git clone https://github.com/YashRawate/VitalAir_JAVA.git
   cd VitalAir_JAVA/vital-air-backend
   ```

2. Launch Docker Compose (Spins up PostgreSQL + Spring Boot backend):
   ```bash
   docker compose up --build
   ```

3. Open your browser and navigate to:
   ```
   http://localhost:8080
   ```

---

### Method 2: Running via Maven CLI (Zero DB Setup Required)

1. Set the mandatory JWT Secret environment variable in PowerShell:
   ```powershell
   $env:VITALAIR_JWT_SECRET="a-very-secret-jwt-key-with-at-least-32-characters-long!";
   ```

2. Run the application (uses in-memory H2 database):
   ```powershell
   cd vital-air-backend
   mvn spring-boot:run
   ```

3. Access the web interface at `http://localhost:8080`.

---

## ☁️ Deployment Guide (Render)

Deploying VitalAir to [Render](https://render.com) is automated via the included `render.yaml` blueprint:

1. Push your repository to **GitHub**.
2. Log in to your [Render Dashboard](https://dashboard.render.com/) and click **New +** -> **Blueprint**.
3. Connect your `VitalAir_JAVA` GitHub repository.
4. Render will automatically provision:
   - **PostgreSQL Database** (`vitalair-db`)
   - **Spring Boot Web Service** (`vitalair-backend`)
5. Configure Environment Variables in Render:
   - `VITALAIR_JWT_SECRET`: Generate a 32+ character random string.
   - `OPENWEATHER_API_KEY` *(Optional)*: Your OpenWeather key.
   - `TOMTOM_API_KEY` *(Optional)*: Your TomTom Traffic key.
6. Once deployed, copy your Render URL (e.g. `https://vital-air-java.onrender.com`) and paste it into the **Live Demo** section above!

---

## 👥 Acknowledgements

This project was originally conceptualized and created as a team effort during the **TECHNEX'26 Eco-Hackathon**:
- **K Praveen Kumar** (Cloud Infrastructure & AWS)
- **Yash Kumar Rawate** (Backend Development & Integration)
- **Abhijeet Kumar** (Frontend UI/UX)
- **G. Sandhya Reddy** (ECE & Map Integration)

*This repository represents a full backend re-architecture in Java 21 & Spring Boot 3 for production performance, clean enterprise patterns, and portfolio showcase.*

---

## 📄 License

Distributed under the **MIT License**. See `LICENSE` for more information.
