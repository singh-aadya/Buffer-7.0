# 🌿 SmartAgri Routing Engine — Cost + Carbon Optimizer

> **DSA-focused full-stack project** | Dijkstra's Algorithm | Multi-Objective Scoring | Dynamic Simulation
>
---

## 📋 Table of Contents

1. [Problem Statement](#-problem-statement)
2. [Tech Stack](#-tech-stack)
3. [DSA Concepts](#-dsa-concepts)
4. [Architecture](#-architecture)
5. [Complexity Analysis](#-complexity-analysis)
6. [API Reference](#-api-reference)
7. [How to Run](#-how-to-run)
8. [Sample Data](#-sample-data)
9. [SDG Impact](#-sdg-impact)

---

##  Problem Statement

Agricultural supply chains in India suffer from:
- **30% post-harvest loss** due to poor routing and logistics
- **High carbon footprint** from non-optimized vehicle routes
- **No dynamic adaptation** when roads flood, traffic spikes, or blockades occur

**SmartAgri Routing Engine** models the supply chain as a **weighted directed graph** and applies **Dijkstra's Algorithm** with **multi-objective scoring** to simultaneously minimize:

```
score = Wc × cost + Wt × time + Wco₂ × carbon
```

Where weights `Wc + Wt + Wco₂ = 1.0` are configurable per delivery priority.

---

## 🛠 Tech Stack

| Layer     | Technology                          |
|-----------|-------------------------------------|
| Backend   | Spring Boot 3.2, Java 17            |
| Algorithm | Dijkstra's + PriorityQueue          |
| Frontend  | React 18, Vite                      |
| Graph UI  | SVG (custom canvas, no library)     |
| Styling   | Inline CSS (zero dependencies)      |

---

> <img width="1747" height="916" alt="image" src="https://github.com/user-attachments/assets/40e3bc5c-28c7-40b1-8b8d-57b78e9a2099" />


> <img width="1716" height="900" alt="image" src="https://github.com/user-attachments/assets/08a533d2-845b-4a73-be04-cd2f78b9139f" />


> <img width="1591" height="918" alt="image" src="https://github.com/user-attachments/assets/179fdf4d-b47c-435d-b256-5bfc09598ff8" />


> <img width="1542" height="563" alt="image" src="https://github.com/user-attachments/assets/31f81980-40c0-421c-b9cf-09cad4894230" />


> <img width="1346" height="809" alt="image" src="https://github.com/user-attachments/assets/6e1b3c37-e33c-4da0-89f2-79a40e94ab5c" />


> <img width="1117" height="834" alt="image" src="https://github.com/user-attachments/assets/befa3eca-d040-4ef1-803d-18505ebff897" />


> <img width="1548" height="410" alt="image" src="https://github.com/user-attachments/assets/97e0524a-65cb-4e0b-a07b-52e0cbb6fc3d" />



> <img width="1401" height="707" alt="image" src="https://github.com/user-attachments/assets/e04678f3-3653-4cc7-8470-ac728b87a094" />

## 🧩 DSA Concepts

### 1. Graph — Adjacency List

```
Data Structure: HashMap<String, List<Edge>>
Key   = Node ID (String)
Value = List<Edge> of outgoing edges from that node
```

**Why Adjacency List over Adjacency Matrix?**

| Property        | Adjacency Matrix | Adjacency List (Ours) |
|-----------------|------------------|-----------------------|
| Space           | O(V²)            | **O(V + E)**          |
| Get neighbors   | O(V)             | **O(degree)**         |
| Best for        | Dense graphs     | **Sparse graphs** ✓   |

Agricultural road networks are **sparse** — a farm connects to 2–3 roads, not all 8 nodes. Our `HashMap` gives O(1) average lookup of any node's neighbors.

---

### 2. Dijkstra's Algorithm (CORE)

```
Algorithm: Single-Source Shortest Path (SSSP)
Input:  Weighted directed graph G(V, E), source node s
Output: Minimum-score path to destination d
```

#### Why Dijkstra (not BFS / DFS / Bellman-Ford)?

| Algorithm     | Weights  | Time        | Use Case                |
|---------------|----------|-------------|-------------------------|
| BFS           | ✗ None   | O(V + E)    | Unweighted graphs only  |
| DFS           | ✗ None   | O(V + E)    | No shortest path guarantee |
| Bellman-Ford  | ✓ Any    | O(VE)       | Negative weights        |
| **Dijkstra**  | ✓ ≥ 0    | **O((V+E) log V)** | **Our case ✓** |

All edge weights (cost, time, carbon) are **non-negative** → Dijkstra is optimal.

#### Algorithm Steps

```
1. Initialize:
   distanceMap = {all nodes → ∞}, distanceMap[source] = 0
   parentMap   = {} (for path reconstruction)
   edgeUsedMap = {} (for metric accumulation)
   PriorityQueue pq = {(source, score=0)}  ← min-heap

2. While pq is not empty:
   a. Pop (currentNode, score) = pq.poll()  ← O(log V)
   b. If score > distanceMap[currentNode]: SKIP (stale entry)
   c. If currentNode == destination: BREAK (early termination)
   d. For each edge (currentNode → neighbor):
      - Skip if edge.blocked == true
      - edgeScore = Wc×cost + Wt×time + Wco₂×carbon
      - newScore  = distanceMap[currentNode] + edgeScore
      - If newScore < distanceMap[neighbor]:   ← RELAXATION
          distanceMap[neighbor] = newScore
          parentMap[neighbor]   = currentNode
          edgeUsedMap[neighbor] = edge
          pq.offer((neighbor, newScore))       ← O(log V)

3. Reconstruct path:
   Backtrack parentMap from destination → source, then reverse.
```

#### Key correctness property:
> *"Once a node is popped from the min-heap, its distance is FINAL."*
> This greedy property holds because all edge weights ≥ 0.

---

### 3. Priority Queue (Min-Heap)

```java
// Min-heap: lowest multi-objective score extracted first
PriorityQueue<NodeScore> pq = new PriorityQueue<>();

// NodeScore.compareTo() orders by ascending score
// Heap guarantees O(log V) insert and O(log V) extract-min
```

**Also used for DeliveryOrder dispatch queue:**

```java
// DeliveryOrder implements Comparable<DeliveryOrder>
// Priority: CRITICAL > HIGH > MEDIUM > LOW
// Tiebreak: perishable first, then older orders first
PriorityQueue<DeliveryOrder> dispatchQueue = new PriorityQueue<>();
```

---

### 4. Multi-Objective Scoring Formula

```
score = Wc × cost(INR) + Wt × time(hours) + Wco₂ × carbon(kgCO₂)

Default weights:  Wc = 0.4,  Wt = 0.3,  Wco₂ = 0.3
Constraint:       Wc + Wt + Wco₂ = 1.0

Tuning examples:
  Carbon-first:   Wc=0.2, Wt=0.2, Wco₂=0.6  (eco-priority delivery)
  Speed-first:    Wc=0.2, Wt=0.7, Wco₂=0.1  (emergency perishables)
  Cost-first:     Wc=0.7, Wt=0.15, Wco₂=0.15 (budget logistics)
```

---

### 5. Dynamic Simulation (Graph Weight Updates)

```
Event → Modify edge weights in adjacency list → Re-run Dijkstra

Events:
  TRAFFIC:  edge.timeHours  *= multiplier (1.5–2.5x)
  FLOOD:    edge.blocked     = true       (skip in relaxation)
  WEATHER:  edge.*           *= multiplier (all metrics)
  BLOCKADE: edge.blocked     = true
  FESTIVAL: edge.timeHours  *= 1.3x

DSA Insight: Dijkstra must be RE-RUN from scratch after any weight
change — there's no O(1) incremental update for shortest paths.
This is why Google Maps continuously re-computes routes in real-time.
```

---

## 🏗 Architecture

```
smartagri/
├── backend/                           # Spring Boot
│   └── src/main/java/com/smartagri/
│       ├── SmartAgriApplication.java  # Entry point + graph init
│       ├── model/
│       │   ├── Node.java              # Graph vertex (implements Comparable)
│       │   ├── Edge.java              # Directed weighted edge
│       │   ├── Vehicle.java           # Transport vehicle
│       │   ├── DeliveryOrder.java     # implements Comparable (PriorityQueue)
│       │   └── RouteResult.java       # Dijkstra output + metrics
│       ├── service/
│       │   ├── GraphService.java      # Adjacency list + graph operations
│       │   ├── RouteService.java      # Dijkstra's Algorithm (CORE)
│       │   ├── SimulationService.java # Dynamic edge weight updates
│       │   └── MetricsService.java    # Impact tracking + dashboard
│       └── controller/
│           └── RouteController.java   # REST API endpoints
│
├── frontend/                          # React + Vite
│   ├── index.html
│   └── src/
│       ├── main.jsx
│       └── App.jsx                    # Complete SPA (4 pages)
│
└── README.md
```

---

## ⏱ Complexity Analysis

| Operation              | Data Structure        | Time Complexity    | Space |
|------------------------|-----------------------|--------------------|-------|
| Add node               | HashMap               | O(1) avg           | O(1)  |
| Add edge               | ArrayList             | O(1)               | O(1)  |
| Get neighbors          | HashMap               | O(1) avg           | —     |
| **Dijkstra (full)**    | **PriorityQueue + HashMap** | **O((V+E) log V)** | **O(V)** |
| Path reconstruction    | HashMap (parentMap)   | O(V)               | O(V)  |
| Heap insert            | PriorityQueue         | O(log V)           | —     |
| Heap extract-min       | PriorityQueue         | O(log V)           | —     |
| DeliveryOrder queue    | PriorityQueue         | O(log n)           | O(n)  |

**For our graph (V=8, E=14):**
```
Dijkstra: O((8+14) × log 8) = O(22 × 3) = O(66 operations)
→ Effectively constant-time for this graph size
→ Scales to V=1000, E=5000 city networks without algorithmic changes
```

---

## 🌐 API Reference

### POST `/route/optimize`
Run Dijkstra's algorithm with multi-objective scoring.

**Request:**
```json
{
  "sourceNodeId": "FARM_PUNE",
  "destinationNodeId": "MARKET_DADAR",
  "weightCost": 0.4,
  "weightTime": 0.3,
  "weightCarbon": 0.3
}
```

**Response:**
```json
{
  "success": true,
  "algorithm": "Dijkstra's Algorithm (Multi-Objective Scoring)",
  "complexity": "O((V + E) log V)",
  "ecoPointsEarned": 90,
  "totalEcoPoints": 90,
  "result": {
    "path": [...],
    "totalCostINR": 2450,
    "totalTimeHours": 3.7,
    "totalCarbonKg": 34.8,
    "baselineCostINR": 3480,
    "costSavedPercent": 29.6,
    "timeReducedPercent": 30.2,
    "carbonReducedPercent": 29.7,
    "pathDescription": "Pune Farm Hub → Pune Warehouse → ...",
    "routeFound": true
  }
}
```

### POST `/simulate/event`
Apply a dynamic event and re-route.

```json
{
  "eventType": "TRAFFIC",
  "affectedEdges": ["WH_PUNE-WH_MUMBAI", "DC_THANE-MARKET_DADAR"],
  "sourceNodeId": "FARM_PUNE",
  "destinationNodeId": "MARKET_DADAR",
  "multiplier": 2.0
}
```

Preset scenarios:
```json
{ "preset": "MONSOON",       "sourceNodeId": "...", "destinationNodeId": "..." }
{ "preset": "HIGHWAY_BLOCK", "sourceNodeId": "...", "destinationNodeId": "..." }
{ "preset": "PEAK_TRAFFIC",  "sourceNodeId": "...", "destinationNodeId": "..." }
```

### GET `/simulate/reset`
Reset all events. Restore original graph.

### GET `/metrics`
Get cumulative impact dashboard data.

### GET `/graph`
Get full adjacency list, nodes, edges, vehicles.

### POST `/graph/init`
Re-initialize default Maharashtra graph.

### GET `/health`
Service health check.

---

## 🚀 How to Run

### Prerequisites
- Java 17+
- Maven 3.8+
- Node.js 18+

### Backend (Spring Boot)

```bash
cd backend
mvn spring-boot:run
```

Server starts at: `http://localhost:8080`

Graph auto-initializes on startup with 8 nodes + 14 edges.

Test it:
```bash
curl -X GET http://localhost:8080/health
curl -X GET http://localhost:8080/graph
curl -X POST http://localhost:8080/route/optimize \
  -H "Content-Type: application/json" \
  -d '{"sourceNodeId":"FARM_PUNE","destinationNodeId":"MARKET_DADAR","weightCost":0.4,"weightTime":0.3,"weightCarbon":0.3}'
```

### Frontend (React + Vite)

```bash
cd frontend
npm install
npm run dev
```

App starts at: `http://localhost:3000`

> ⚠️ Start the backend first. The frontend shows a warning if the backend is unreachable.

---

## 📦 Sample Data

### Nodes (Maharashtra Supply Chain)

| ID              | Name                    | Type                |
|-----------------|-------------------------|---------------------|
| FARM_PUNE       | Pune Farm Hub           | FARM                |
| FARM_NASHIK     | Nashik Grape Farm       | FARM                |
| COLD_PUNE       | Pune Cold Storage       | COLD_STORAGE        |
| WH_PUNE         | Pune Warehouse          | WAREHOUSE           |
| WH_MUMBAI       | Mumbai Warehouse        | WAREHOUSE           |
| DC_THANE        | Thane Dist. Center      | DISTRIBUTION_CENTER |
| MARKET_DADAR    | Dadar Market            | RETAIL              |
| MARKET_VASHI    | Vashi Market            | RETAIL              |

### Sample Edges

| Route                    | Cost (₹) | Time (h) | Carbon (kg) | Road Type   |
|--------------------------|----------|----------|-------------|-------------|
| FARM_PUNE → WH_PUNE      | 650      | 0.9      | 7.2         | STATE_ROAD  |
| WH_PUNE → WH_MUMBAI      | 1400     | 2.5      | 19.8        | HIGHWAY     |
| WH_MUMBAI → DC_THANE     | 500      | 0.8      | 5.4         | STATE_ROAD  |
| DC_THANE → MARKET_DADAR  | 450      | 0.7      | 4.5         | STATE_ROAD  |
| FARM_PUNE → WH_MUMBAI    | 1100     | 3.5      | 14.4        | VILLAGE_ROAD|

### Vehicles

| ID    | Name                  | Emission (kg/km) | Electric? |
|-------|-----------------------|------------------|-----------|
| VH001 | Heavy Diesel Truck    | 0.25             | No        |
| VH002 | Medium Truck          | 0.18             | No        |
| VH003 | Refrigerated Van      | 0.22             | No        |
| VH004 | Electric Cargo Van    | 0.05             | **Yes**   |
| VH005 | Motorcycle (Express)  | 0.08             | No        |

---

## 🌍 SDG Impact

| SDG    | Goal                  | How We Contribute                              |
|--------|-----------------------|------------------------------------------------|
| SDG 2  | Zero Hunger           | Reduce food loss via faster, optimized routes  |
| SDG 12 | Responsible Use       | Minimize transport waste and fuel consumption  |
| SDG 13 | Climate Action        | Reduce CO₂ via carbon-aware routing            |

**Key Facts:**
- 🌾 30% of agricultural produce lost to poor logistics
- 🚚 Road freight = 45% of logistics carbon emissions in India
- 🌡 Smart routing can cut supply chain CO₂ by 25–35%

---

## 🔍 Error Handling

| Scenario               | Response                                      |
|------------------------|-----------------------------------------------|
| Node not found         | 400 + "Node 'X' not found in graph"          |
| No path exists         | 200 + `routeFound: false` + message           |
| Disconnected graph     | 200 + `routeFound: false` + explanation       |
| Invalid weight sum     | 400 + "Weights must sum to 1.0"              |
| Source = Destination   | 400 + "Source and destination cannot be same" |
| All edges blocked      | 200 + `routeFound: false` + "No path found"  |

---

*Algorithm: Dijkstra's O((V+E) log V) · Graph: Adjacency List O(V+E) · PQ: O(log V)*
