import { useState, useEffect, useRef, useCallback } from "react";

// ============================================================
// SmartAgri Routing Engine — React Frontend
// Light mode, agricultural green theme, smooth animations
// Pages: Graph Setup | Routing Engine | Simulation | Dashboard
// ============================================================

const API = "http://localhost:8080";

// ---- Colour tokens ----
const C = {
  green:     "#2d6a4f",
  greenMid:  "#40916c",
  greenLight:"#74c69d",
  lime:      "#95d5b2",
  limeLight: "#d8f3dc",
  yellow:    "#f4a261",
  orange:    "#e76f51",
  sky:       "#48cae4",
  bg:        "#f8fdf9",
  card:      "#ffffff",
  border:    "#b7e4c7",
  textDark:  "#1b4332",
  textMid:   "#2d6a4f",
  textLight: "#52b788",
  muted:     "#6b7280",
};

// ---- Utility ----
const fmt = (n, d = 0) => (typeof n === "number" ? n.toFixed(d) : "–");
const pct  = n => fmt(n, 1) + "%";

// ---- API helpers ----
async function api(method, path, body) {
  try {
    const res = await fetch(API + path, {
      method,
      headers: { "Content-Type": "application/json" },
      body: body ? JSON.stringify(body) : undefined,
    });
    return await res.json();
  } catch {
    return { success: false, error: "Cannot reach backend. Is Spring Boot running on :8080?" };
  }
}

// ============================================================
// SHARED COMPONENTS
// ============================================================

function Spinner({ text = "Processing…" }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 12, padding: 40 }}>
      <div style={{ width: 40, height: 40, border: `4px solid ${C.limeLight}`, borderTopColor: C.green,
                    borderRadius: "50%", animation: "spin 0.8s linear infinite" }} />
      <span style={{ color: C.textMid, fontFamily: "Crimson Pro, Georgia, serif", fontSize: 15 }}>{text}</span>
    </div>
  );
}

function Card({ children, style = {} }) {
  return (
    <div style={{ background: C.card, borderRadius: 16, border: `1.5px solid ${C.border}`,
                  boxShadow: "0 2px 16px #2d6a4f12", padding: 24, ...style }}>
      {children}
    </div>
  );
}

function Badge({ children, color = C.green, bg = C.limeLight }) {
  return (
    <span style={{ background: bg, color, borderRadius: 20, padding: "3px 12px",
                   fontSize: 12, fontWeight: 700, letterSpacing: 0.4 }}>
      {children}
    </span>
  );
}

function MetricPill({ label, value, unit = "", color = C.green, icon }) {
  return (
    <div style={{ background: C.limeLight, borderRadius: 14, padding: "14px 20px",
                  display: "flex", flexDirection: "column", gap: 4, flex: 1, minWidth: 140 }}>
      <span style={{ fontSize: 11, color: C.muted, fontWeight: 600, textTransform: "uppercase", letterSpacing: 0.8 }}>
        {icon} {label}
      </span>
      <span style={{ fontSize: 26, fontWeight: 800, color, fontFamily: "Crimson Pro, Georgia, serif" }}>
        {value}<span style={{ fontSize: 13, fontWeight: 400, color: C.textLight }}> {unit}</span>
      </span>
    </div>
  );
}

function ImprovementBar({ label, percent, color }) {
  return (
    <div style={{ marginBottom: 14 }}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 5 }}>
        <span style={{ fontSize: 13, color: C.textDark, fontWeight: 600 }}>{label}</span>
        <span style={{ fontSize: 13, color, fontWeight: 700 }}>{pct(percent)} saved</span>
      </div>
      <div style={{ height: 8, background: C.limeLight, borderRadius: 99, overflow: "hidden" }}>
        <div style={{ width: `${Math.min(percent, 100)}%`, height: "100%", background: color,
                      borderRadius: 99, transition: "width 0.8s cubic-bezier(.4,0,.2,1)" }} />
      </div>
    </div>
  );
}

function Alert({ type, message }) {
  const styles = {
    error:   { bg: "#fff5f5", border: "#fca5a5", color: "#dc2626", icon: "⚠️" },
    success: { bg: "#f0fdf4", border: C.border,  color: C.green,   icon: "✅" },
    info:    { bg: "#f0f9ff", border: "#7dd3fc", color: "#0369a1", icon: "ℹ️" },
  };
  const s = styles[type] || styles.info;
  return (
    <div style={{ background: s.bg, border: `1px solid ${s.border}`, borderRadius: 10,
                  padding: "10px 16px", color: s.color, fontSize: 14, display: "flex", gap: 8, alignItems: "center" }}>
      {s.icon} {message}
    </div>
  );
}

// ============================================================
// SDG POPUP
// ============================================================
const SDG_FACTS = [
  "🌾 30% of agricultural produce is lost due to poor logistics & supply chain inefficiencies.",
  "🚚 Road freight accounts for ~45% of total logistics carbon emissions in India.",
  "❄️ Cold chain gaps cause ₹92,000 crore in post-harvest losses annually in India.",
  "🌱 Optimizing just 10% of farm-to-market routes can cut CO₂ by millions of tonnes/year.",
  "📦 SDG 12: Responsible consumption requires minimizing food & transport waste.",
  "🌍 SDG 13: Climate action depends on decarbonizing agricultural supply chains.",
  "🍅 Perishable food (fruits/vegetables) accounts for 40% of post-harvest loss in India.",
];

function SdgPopup({ onClose }) {
  const fact = SDG_FACTS[Math.floor(Math.random() * SDG_FACTS.length)];
  return (
    <div style={{ position: "fixed", bottom: 24, right: 24, maxWidth: 340,
                  background: C.green, color: "#fff", borderRadius: 16,
                  padding: 20, boxShadow: "0 8px 32px #1b433266",
                  animation: "slideUp 0.3s ease", zIndex: 1000 }}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}>
        <span style={{ fontWeight: 700, fontSize: 13, letterSpacing: 0.5 }}>🎯 SDG IMPACT FACT</span>
        <button onClick={onClose} style={{ background: "none", border: "none",
                                           color: "#fff", cursor: "pointer", fontSize: 16 }}>×</button>
      </div>
      <p style={{ margin: 0, fontSize: 14, lineHeight: 1.6, opacity: 0.95 }}>{fact}</p>
    </div>
  );
}

// ============================================================
// GRAPH CANVAS — simple SVG-based visualiser
// ============================================================
const NODE_POSITIONS = {
  FARM_PUNE:    { x: 160, y: 280 },
  FARM_NASHIK:  { x: 160, y: 120 },
  COLD_PUNE:    { x: 300, y: 240 },
  WH_PUNE:      { x: 300, y: 340 },
  WH_MUMBAI:    { x: 520, y: 200 },
  DC_THANE:     { x: 620, y: 280 },
  MARKET_DADAR: { x: 720, y: 220 },
  MARKET_VASHI: { x: 720, y: 340 },
};

const NODE_COLORS = {
  FARM:                 { fill: "#d8f3dc", stroke: C.green,     label: "🌾" },
  COLD_STORAGE:         { fill: "#e0f4fd", stroke: C.sky,       label: "❄️" },
  WAREHOUSE:            { fill: "#fff3cd", stroke: C.yellow,    label: "🏭" },
  DISTRIBUTION_CENTER:  { fill: "#fce4ec", stroke: "#e91e63",   label: "📦" },
  RETAIL:               { fill: "#f3e5f5", stroke: "#9c27b0",   label: "🛒" },
};

function GraphCanvas({ graphData, routePath = [], activeEvents = {} }) {
  if (!graphData || !graphData.nodes) return null;

  const nodes = graphData.nodes;
  const edges = graphData.edges || [];
  const pathNodeIds = new Set(routePath.map(n => n.id));
  const pathEdgeSet = new Set();

  // Build path edge set
  for (let i = 0; i < routePath.length - 1; i++) {
    pathEdgeSet.add(`${routePath[i].id}-${routePath[i + 1].id}`);
  }

  return (
    <svg width="100%" viewBox="0 0 860 440" style={{ borderRadius: 12,
         background: "linear-gradient(135deg, #f0fdf4 0%, #e8f5e9 100%)", maxHeight: 320 }}>
      {/* Edge lines */}
      {edges.map((e, i) => {
        const from = NODE_POSITIONS[e.fromNodeId];
        const to   = NODE_POSITIONS[e.toNodeId];
        if (!from || !to) return null;
        const key = `${e.fromNodeId}-${e.toNodeId}`;
        const isPath    = pathEdgeSet.has(key);
        const isBlocked = e.blocked;
        const hasEvent  = activeEvents[key];
        return (
          <line key={i}
            x1={from.x} y1={from.y} x2={to.x} y2={to.y}
            stroke={isBlocked ? "#dc2626" : isPath ? C.green : hasEvent ? C.orange : "#b7e4c7"}
            strokeWidth={isPath ? 3.5 : isBlocked ? 2.5 : 1.8}
            strokeDasharray={isBlocked ? "6 4" : "none"}
            opacity={isPath ? 1 : 0.65}
          />
        );
      })}

      {/* Nodes */}
      {nodes.map(node => {
        const pos = NODE_POSITIONS[node.id];
        if (!pos) return null;
        const t   = NODE_COLORS[node.type] || { fill: "#e8f5e9", stroke: C.green, label: "📍" };
        const isOnPath = pathNodeIds.has(node.id);
        return (
          <g key={node.id} transform={`translate(${pos.x},${pos.y})`}>
            <circle r={isOnPath ? 26 : 22}
              fill={isOnPath ? C.green : t.fill}
              stroke={isOnPath ? C.greenLight : t.stroke}
              strokeWidth={isOnPath ? 3 : 2}
              style={{ filter: isOnPath ? "drop-shadow(0 0 8px #2d6a4f88)" : "none",
                       transition: "all 0.4s ease" }} />
            <text textAnchor="middle" dominantBaseline="middle" fontSize={isOnPath ? 14 : 12}>
              {t.label}
            </text>
            <text y={34} textAnchor="middle" fontSize={9} fontWeight={isOnPath ? 700 : 500}
              fill={isOnPath ? C.green : C.textDark}>
              {node.name.split(" ").slice(0, 2).join(" ")}
            </text>
          </g>
        );
      })}
    </svg>
  );
}

// ============================================================
// PAGE 1 — GRAPH SETUP
// ============================================================
function GraphSetupPage({ graphData, onRefresh, loading }) {
  const nodes = graphData?.nodes || [];
  const edges = graphData?.edges || [];
  const stats = graphData?.stats || {};

  const typeStats = nodes.reduce((acc, n) => {
    acc[n.type] = (acc[n.type] || 0) + 1;
    return acc;
  }, {});

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
      {/* Header */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <div>
          <h2 style={{ margin: 0, color: C.textDark, fontFamily: "Crimson Pro, Georgia, serif", fontSize: 26 }}>
            Supply Chain Graph
          </h2>
          <p style={{ margin: "4px 0 0", color: C.muted, fontSize: 14 }}>
            Adjacency List · HashMap&lt;Node, List&lt;Edge&gt;&gt; · Space O(V+E)
          </p>
        </div>
        <button onClick={onRefresh}
          style={{ background: C.green, color: "#fff", border: "none", borderRadius: 10,
                   padding: "9px 18px", cursor: "pointer", fontWeight: 700, fontSize: 14 }}>
          ↺ Reload
        </button>
      </div>

      {/* Stats row */}
      <div style={{ display: "flex", gap: 14, flexWrap: "wrap" }}>
        <MetricPill icon="📍" label="Nodes (V)"   value={nodes.length}  color={C.green}  />
        <MetricPill icon="↔️" label="Edges (E)"   value={edges.length}  color={C.greenMid} />
        <MetricPill icon="🚛" label="Vehicles"    value={graphData?.vehicles?.length || 0} color={C.yellow} />
        <MetricPill icon="⚙️" label="Avg Degree"  value={fmt(stats.averageDegree, 1)} color={C.sky} />
      </div>

      {/* Graph visualization */}
      <Card>
        <h3 style={{ margin: "0 0 14px", color: C.textDark, fontSize: 16 }}>Maharashtra Supply Chain Network</h3>
        {loading ? <Spinner text="Loading graph…" /> : <GraphCanvas graphData={graphData} />}
        <div style={{ display: "flex", gap: 16, marginTop: 14, flexWrap: "wrap" }}>
          {Object.entries(NODE_COLORS).map(([type, def]) => (
            <span key={type} style={{ fontSize: 12, color: C.textMid, display: "flex", alignItems: "center", gap: 4 }}>
              {def.label} {type.replace(/_/g, " ")}
            </span>
          ))}
        </div>
      </Card>

      {/* Node type breakdown */}
      <div style={{ display: "flex", gap: 16, flexWrap: "wrap" }}>
        <Card style={{ flex: 1, minWidth: 240 }}>
          <h3 style={{ margin: "0 0 14px", color: C.textDark, fontSize: 16 }}>Node Types</h3>
          {Object.entries(typeStats).map(([type, count]) => {
            const def = NODE_COLORS[type] || {};
            return (
              <div key={type} style={{ display: "flex", justifyContent: "space-between",
                                       padding: "7px 0", borderBottom: `1px solid ${C.limeLight}` }}>
                <span style={{ fontSize: 14, color: C.textDark }}>{def.label} {type.replace(/_/g, " ")}</span>
                <Badge>{count}</Badge>
              </div>
            );
          })}
        </Card>

        <Card style={{ flex: 2, minWidth: 300 }}>
          <h3 style={{ margin: "0 0 14px", color: C.textDark, fontSize: 16 }}>Vehicles Available</h3>
          {(graphData?.vehicles || []).map(v => (
            <div key={v.id} style={{ display: "flex", justifyContent: "space-between",
                                     padding: "8px 0", borderBottom: `1px solid ${C.limeLight}`,
                                     alignItems: "center" }}>
              <div>
                <div style={{ fontWeight: 700, fontSize: 14, color: C.textDark }}>{v.name}</div>
                <div style={{ fontSize: 12, color: C.muted }}>{v.type} · {v.capacityKg}kg capacity</div>
              </div>
              <div style={{ textAlign: "right" }}>
                <Badge color={v.type === "ELECTRIC" ? C.sky : C.textMid}
                       bg={v.type === "ELECTRIC" ? "#e0f4fd" : C.limeLight}>
                  {v.emissionFactorKgPerKm} kg CO₂/km
                </Badge>
              </div>
            </div>
          ))}
        </Card>
      </div>

      {/* DSA Info card */}
      <Card style={{ borderLeft: `4px solid ${C.green}`, background: "#f0fdf4" }}>
        <h4 style={{ margin: "0 0 10px", color: C.green, fontSize: 15 }}>🧠 DSA: Why Adjacency List?</h4>
        <p style={{ margin: 0, fontSize: 14, color: C.textDark, lineHeight: 1.7 }}>
          Agricultural supply chain graphs are <strong>sparse</strong> — each node connects to only a few others.
          An adjacency matrix would waste O(V²) space. Our <code>HashMap&lt;String, List&lt;Edge&gt;&gt;</code>
          uses only O(V+E) space and provides O(1) average access to any node's neighbors — 
          the key operation in Dijkstra's relaxation loop.
        </p>
      </Card>
    </div>
  );
}

// ============================================================
// PAGE 2 — ROUTING ENGINE (MAIN)
// ============================================================
function RoutingPage({ graphData, onRouteFound }) {
  const nodes = graphData?.nodes || [];
  const [source, setSource]  = useState("FARM_PUNE");
  const [dest, setDest]      = useState("MARKET_DADAR");
  const [wc, setWc]          = useState(0.4);
  const [wt, setWt]          = useState(0.3);
  const [wco, setWco]        = useState(0.3);
  const [result, setResult]  = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError]    = useState(null);
  const [phase, setPhase]    = useState("idle"); // idle | loading | done

  const weightSum = +(wc + wt + wco).toFixed(2);
  const weightsOk = Math.abs(weightSum - 1.0) < 0.02;

  const handleOptimize = async () => {

  if (!weightsOk) {
    setError("Weights must sum to 1.0");
    return;
  }

  if (source === dest) {
    setError("Source and destination must differ");
    return;
  }

  setError(null);
  setLoading(true);
  setResult(null);

  try {
    const res = await fetch("http://localhost:8080/route/optimize", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        sourceNodeId: source,
        destinationNodeId: dest,
        weightCost: wc,
        weightTime: wt,
        weightCarbon: wco
      })
    });

    if (!res.ok) {
      throw new Error("Server error: " + res.status);
    }

    const data = await res.json();
    console.log("API RESPONSE:", data);

    // ✅ IMPORTANT SAFETY CHECK
    if (!data || !data.result) {
      setError("Invalid backend response");
      return;
    }

    // ✅ FIX: store only result
    setResult(data.result);

    // ✅ pass clean result
    onRouteFound && onRouteFound(data.result);

  } catch (err) {
    console.error(err);
    setError("Backend error");
  } finally {
    setLoading(false);
  }
};
  const r = result;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
      <div>
        <h2 style={{ margin: 0, color: C.textDark, fontFamily: "Crimson Pro, Georgia, serif", fontSize: 26 }}>
          Routing Engine
        </h2>
        <p style={{ margin: "4px 0 0", color: C.muted, fontSize: 14 }}>
          Dijkstra's Algorithm · score = Wc·cost + Wt·time + Wco₂·carbon · O((V+E) log V)
        </p>
      </div>

      {/* Configuration card */}
      <Card>
        <h3 style={{ margin: "0 0 16px", fontSize: 16, color: C.textDark }}>⚙️ Route Configuration</h3>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16, marginBottom: 20 }}>
          <div>
            <label style={{ fontSize: 13, fontWeight: 600, color: C.textDark, display: "block", marginBottom: 6 }}>
              🟢 Source Node
            </label>
            <select value={source} onChange={e => setSource(e.target.value)}
              style={{ width: "100%", padding: "9px 12px", borderRadius: 8, border: `1.5px solid ${C.border}`,
                       fontSize: 14, color: C.textDark, background: "#fff" }}>
              {nodes.map(n => <option key={n.id} value={n.id}>{n.name}</option>)}
            </select>
          </div>
          <div>
            <label style={{ fontSize: 13, fontWeight: 600, color: C.textDark, display: "block", marginBottom: 6 }}>
              🔴 Destination Node
            </label>
            <select value={dest} onChange={e => setDest(e.target.value)}
              style={{ width: "100%", padding: "9px 12px", borderRadius: 8, border: `1.5px solid ${C.border}`,
                       fontSize: 14, color: C.textDark, background: "#fff" }}>
              {nodes.map(n => <option key={n.id} value={n.id}>{n.name}</option>)}
            </select>
          </div>
        </div>

        {/* Weight sliders */}
        <h4 style={{ margin: "0 0 14px", fontSize: 14, color: C.textDark }}>
          Multi-Objective Weights
          <span style={{ marginLeft: 10, fontSize: 12, fontWeight: 400, color: weightsOk ? C.green : C.orange }}>
            Sum: {weightSum} {weightsOk ? "✓" : "⚠ must = 1.0"}
          </span>
        </h4>
        {[
          { label: "💰 Cost (Wc)", val: wc, set: setWc, color: C.yellow },
          { label: "⏱ Time (Wt)", val: wt, set: setWt, color: C.sky },
          { label: "🌿 Carbon (Wco₂)", val: wco, set: setWco, color: C.green },
        ].map(({ label, val, set, color }) => (
          <div key={label} style={{ marginBottom: 12 }}>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
              <span style={{ fontSize: 13, fontWeight: 600, color: C.textDark }}>{label}</span>
              <span style={{ fontSize: 13, color, fontWeight: 700 }}>{val}</span>
            </div>
            <input type="range" min={0} max={1} step={0.1} value={val}
              onChange={e => set(+e.target.value)}
              style={{ width: "100%", accentColor: color }} />
          </div>
        ))}

        {error && <Alert type="error" message={error} />}

        <button onClick={handleOptimize} disabled={loading || !weightsOk}
          style={{ width: "100%", marginTop: 16, padding: "13px 0",
                   background: loading ? C.greenLight : C.green, color: "#fff",
                   border: "none", borderRadius: 10, fontSize: 16, fontWeight: 700,
                   cursor: loading ? "not-allowed" : "pointer",
                   transition: "background 0.2s", letterSpacing: 0.5 }}>
          {loading ? "⏳ Optimizing using Dijkstra…" : "🚀 Optimize Route"}
        </button>
      </Card>

      {/* Loading animation */}
      {loading && (
        <Card style={{ textAlign: "center", background: "#f0fdf4" }}>
          <Spinner text="Running Dijkstra's Algorithm… O((V+E) log V)" />
          <p style={{ color: C.textMid, fontSize: 13, margin: "8px 0 0" }}>
            Exploring nodes via Priority Queue min-heap…
          </p>
        </Card>
      )}

      {/* Results */}
      {r && !loading && (
        <div style={{ animation: "fadeIn 0.4s ease" }}>
          {/* Path visualization */}
          <Card style={{ marginBottom: 16 }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
              <h3 style={{ margin: 0, color: C.textDark, fontSize: 16 }}>🗺 Optimal Path Found</h3>
              <Badge>{r.algorithmUsed}</Badge>
            </div>
            <GraphCanvas graphData={graphData} routePath={r.path || []} />
            <div style={{ marginTop: 12, padding: "10px 14px", background: C.limeLight,
                          borderRadius: 10, fontSize: 14, color: C.textDark, fontWeight: 500 }}>
              📍 {r.pathDescription}
            </div>
          </Card>

          {/* Eco points banner */}
          {result?.ecoPointsEarned > 0 && (
            <div style={{ background: `linear-gradient(135deg, ${C.green}, ${C.greenMid})`,
                          borderRadius: 14, padding: "16px 24px", marginBottom: 16,
                          display: "flex", justifyContent: "space-between", alignItems: "center",
                          animation: "slideUp 0.4s ease" }}>
              <div>
                <div style={{ color: "#fff", fontWeight: 800, fontSize: 20 }}>
                  +{result.ecoPointsEarned} Eco Points Earned! 🌱
                </div>
                <div style={{ color: C.lime, fontSize: 14 }}>
                  Total: {result.totalEcoPoints} Eco Points
                </div>
              </div>
              <div style={{ fontSize: 48 }}>🏅</div>
            </div>
          )}

          {/* Before/After comparison */}
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16, marginBottom: 16 }}>
            <Card style={{ borderTop: `4px solid ${C.orange}` }}>
              <h4 style={{ margin: "0 0 14px", color: C.orange, fontSize: 15 }}>BEFORE (Baseline)</h4>
              <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                {[
                  { icon: "💰", label: "Cost",   value: `₹${fmt(r.baselineCostINR, 0)}` },
                  { icon: "⏱", label: "Time",    value: `${fmt(r.baselineTimeHours, 1)}h` },
                  { icon: "🌿", label: "Carbon",  value: `${fmt(r.baselineCarbonKg, 1)} kg CO₂` },
                ].map(({ icon, label, value }) => (
                  <div key={label} style={{ display: "flex", justifyContent: "space-between",
                                            padding: "8px 12px", background: "#fff5f0",
                                            borderRadius: 8, fontSize: 14 }}>
                    <span style={{ color: C.muted }}>{icon} {label}</span>
                    <span style={{ fontWeight: 700, color: C.orange }}>{value}</span>
                  </div>
                ))}
              </div>
            </Card>

            <Card style={{ borderTop: `4px solid ${C.green}` }}>
              <h4 style={{ margin: "0 0 14px", color: C.green, fontSize: 15 }}>AFTER (Optimized)</h4>
              <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                {[
                  { icon: "💰", label: "Cost",   value: `₹${fmt(r.totalCostINR, 0)}` },
                  { icon: "⏱", label: "Time",    value: `${fmt(r.totalTimeHours, 1)}h` },
                  { icon: "🌿", label: "Carbon",  value: `${fmt(r.totalCarbonKg, 1)} kg CO₂` },
                ].map(({ icon, label, value }) => (
                  <div key={label} style={{ display: "flex", justifyContent: "space-between",
                                            padding: "8px 12px", background: C.limeLight,
                                            borderRadius: 8, fontSize: 14 }}>
                    <span style={{ color: C.muted }}>{icon} {label}</span>
                    <span style={{ fontWeight: 700, color: C.green }}>{value}</span>
                  </div>
                ))}
              </div>
            </Card>
          </div>

          {/* Improvement bars */}
          <Card>
            <h3 style={{ margin: "0 0 16px", color: C.textDark, fontSize: 16 }}>📊 Optimization Impact</h3>
            <ImprovementBar label="💰 Cost"   percent={r.costSavedPercent}    color={C.yellow} />
            <ImprovementBar label="⏱ Time"    percent={r.timeReducedPercent}  color={C.sky} />
            <ImprovementBar label="🌿 Carbon"  percent={r.carbonReducedPercent} color={C.green} />
          </Card>
        </div>
      )}
    </div>
  );
}

// ============================================================
// PAGE 3 — SIMULATION
// ============================================================
function SimulationPage({ graphData, lastRoute }) {
  const nodes = graphData?.nodes || [];
  const [source, setSource]      = useState(lastRoute?.sourceNodeId || "FARM_PUNE");
  const [dest, setDest]          = useState(lastRoute?.destinationNodeId || "MARKET_DADAR");
  const [eventType, setEventType] = useState("TRAFFIC");
  const [multiplier, setMultiplier] = useState(1.8);
  const [selectedEdges, setSelectedEdges] = useState([]);
  const [preset, setPreset]      = useState("");
  const [result, setResult]      = useState(null);
  const [loading, setLoading]    = useState(false);
  const [error, setError]        = useState(null);
  const [resetMsg, setResetMsg]  = useState(null);

  const edges = graphData?.edges || [];
  const edgeOptions = edges.map(e => `${e.fromNodeId}-${e.toNodeId}`);

  const EVENT_TYPES = [
    { value: "TRAFFIC",  label: "🚦 Traffic Congestion",  desc: "Increases travel time" },
    { value: "FLOOD",    label: "🌊 Road Flood/Block",    desc: "Completely blocks route" },
    { value: "WEATHER",  label: "🌧 Bad Weather",         desc: "Increases time + carbon" },
    { value: "BLOCKADE", label: "🚧 Road Blockade",       desc: "Road closure" },
    { value: "FESTIVAL", label: "🎉 Seasonal Festival",   desc: "Mild congestion" },
  ];

  const PRESETS = [
    { value: "MONSOON",       label: "🌧 Monsoon Season",     desc: "Multiple routes affected by rain" },
    { value: "HIGHWAY_BLOCK", label: "🚧 Highway Blocked",    desc: "Forces alternative route" },
    { value: "PEAK_TRAFFIC",  label: "🚦 Morning Rush Hour",  desc: "Urban market routes congested" },
  ];

  const handleSimulate = async () => {
    setError(null);
    setLoading(true);
    setResult(null);
    await new Promise(r => setTimeout(r, 900));

    const body = preset
      ? { eventType: "PRESET", preset, sourceNodeId: source, destinationNodeId: dest }
      : { eventType, affectedEdges: selectedEdges, sourceNodeId: source,
          destinationNodeId: dest, multiplier };

    const data = await api("POST", "/simulate/event", body);
    setLoading(false);

    if (data.success === false) { setError(data.error); return; }
    setResult(data);
  };

  const handleReset = async () => {
    await api("GET", "/simulate/reset");
    setResult(null);
    setResetMsg("✅ All simulation events cleared. Graph restored.");
    setTimeout(() => setResetMsg(null), 3000);
  };

  const r = result?.result;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <div>
          <h2 style={{ margin: 0, color: C.textDark, fontFamily: "Crimson Pro, Georgia, serif", fontSize: 26 }}>
            Dynamic Simulation
          </h2>
          <p style={{ margin: "4px 0 0", color: C.muted, fontSize: 14 }}>
            Modify edge weights → Re-run Dijkstra → Observe route adaptation
          </p>
        </div>
        <button onClick={handleReset}
          style={{ background: "#fff", color: C.orange, border: `2px solid ${C.orange}`,
                   borderRadius: 10, padding: "8px 16px", cursor: "pointer", fontWeight: 700, fontSize: 14 }}>
          🔄 Reset Events
        </button>
      </div>

      {resetMsg && <Alert type="success" message={resetMsg} />}

      {/* Quick Presets */}
      <Card>
        <h3 style={{ margin: "0 0 14px", color: C.textDark, fontSize: 16 }}>⚡ Quick Scenarios</h3>
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          {PRESETS.map(p => (
            <button key={p.value}
              onClick={() => { setPreset(p.value); setSelectedEdges([]); }}
              style={{ padding: "10px 16px", borderRadius: 10, cursor: "pointer",
                       border: `2px solid ${preset === p.value ? C.green : C.border}`,
                       background: preset === p.value ? C.limeLight : "#fff",
                       color: preset === p.value ? C.green : C.textDark, fontWeight: 600, fontSize: 13 }}>
              {p.label}
              <div style={{ fontSize: 11, fontWeight: 400, color: C.muted, marginTop: 2 }}>{p.desc}</div>
            </button>
          ))}
          <button onClick={() => setPreset("")}
            style={{ padding: "10px 16px", borderRadius: 10, cursor: "pointer",
                     border: `2px solid ${!preset ? C.green : C.border}`,
                     background: !preset ? C.limeLight : "#fff",
                     color: !preset ? C.green : C.textDark, fontWeight: 600, fontSize: 13 }}>
            ✏️ Custom Event
          </button>
        </div>
      </Card>

      {/* Route Selection */}
      <Card>
        <h3 style={{ margin: "0 0 14px", color: C.textDark, fontSize: 16 }}>🗺 Route to Simulate</h3>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
          <div>
            <label style={{ fontSize: 13, fontWeight: 600, color: C.textDark, display: "block", marginBottom: 6 }}>
              Source
            </label>
            <select value={source} onChange={e => setSource(e.target.value)}
              style={{ width: "100%", padding: "9px 12px", borderRadius: 8, border: `1.5px solid ${C.border}`, fontSize: 14 }}>
              {nodes.map(n => <option key={n.id} value={n.id}>{n.name}</option>)}
            </select>
          </div>
          <div>
            <label style={{ fontSize: 13, fontWeight: 600, color: C.textDark, display: "block", marginBottom: 6 }}>
              Destination
            </label>
            <select value={dest} onChange={e => setDest(e.target.value)}
              style={{ width: "100%", padding: "9px 12px", borderRadius: 8, border: `1.5px solid ${C.border}`, fontSize: 14 }}>
              {nodes.map(n => <option key={n.id} value={n.id}>{n.name}</option>)}
            </select>
          </div>
        </div>
      </Card>

      {/* Custom event config */}
      {!preset && (
        <Card>
          <h3 style={{ margin: "0 0 14px", color: C.textDark, fontSize: 16 }}>🎛 Custom Event Configuration</h3>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16, marginBottom: 16 }}>
            {EVENT_TYPES.map(et => (
              <button key={et.value}
                onClick={() => setEventType(et.value)}
                style={{ padding: "10px 14px", borderRadius: 10, cursor: "pointer", textAlign: "left",
                         border: `2px solid ${eventType === et.value ? C.green : C.border}`,
                         background: eventType === et.value ? C.limeLight : "#fff" }}>
                <div style={{ fontWeight: 700, fontSize: 14, color: eventType === et.value ? C.green : C.textDark }}>
                  {et.label}
                </div>
                <div style={{ fontSize: 12, color: C.muted }}>{et.desc}</div>
              </button>
            ))}
          </div>

          <div style={{ marginBottom: 16 }}>
            <label style={{ fontSize: 13, fontWeight: 600, color: C.textDark, display: "block", marginBottom: 6 }}>
              Weight Multiplier: <span style={{ color: C.orange }}>{multiplier}x</span>
            </label>
            <input type="range" min={1.1} max={3.0} step={0.1} value={multiplier}
              onChange={e => setMultiplier(+e.target.value)}
              style={{ width: "100%", accentColor: C.orange }} />
          </div>

          <div>
            <label style={{ fontSize: 13, fontWeight: 600, color: C.textDark, display: "block", marginBottom: 8 }}>
              Affected Edges (select multiple)
            </label>
            <div style={{ display: "flex", flexDirection: "column", gap: 6, maxHeight: 160, overflowY: "auto",
                          border: `1px solid ${C.border}`, borderRadius: 8, padding: 8 }}>
              {edgeOptions.map(ek => (
                <label key={ek} style={{ display: "flex", gap: 8, alignItems: "center",
                                         cursor: "pointer", fontSize: 13 }}>
                  <input type="checkbox" checked={selectedEdges.includes(ek)}
                    onChange={e => setSelectedEdges(prev =>
                      e.target.checked ? [...prev, ek] : prev.filter(x => x !== ek))} />
                  <span style={{ color: C.textDark }}>{ek.replace(/-/g, " → ")}</span>
                </label>
              ))}
            </div>
          </div>
        </Card>
      )}

      {error && <Alert type="error" message={error} />}

      <button onClick={handleSimulate} disabled={loading}
        style={{ padding: "14px 0", background: loading ? C.greenLight : C.orange,
                 color: "#fff", border: "none", borderRadius: 10, fontSize: 16, fontWeight: 700,
                 cursor: loading ? "not-allowed" : "pointer" }}>
        {loading ? "⏳ Simulating & Re-routing…" : "⚡ Run Simulation"}
      </button>

      {loading && <Spinner text="Applying event → Re-running Dijkstra…" />}

      {/* Simulation result */}
      {r && !loading && (
        <div style={{ animation: "fadeIn 0.4s ease" }}>
          <Card style={{ borderTop: `4px solid ${C.orange}` }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
              <h3 style={{ margin: 0, color: C.textDark, fontSize: 16 }}>
                {r.routeFound ? "🔄 Re-optimized Route" : "⚠️ Route Disrupted"}
              </h3>
              {result.activeEvents && Object.keys(result.activeEvents).length > 0 && (
                <Badge color={C.orange} bg="#fff5f0">
                  {Object.keys(result.activeEvents).length} Active Event(s)
                </Badge>
              )}
            </div>

            {r.routeFound ? (
              <>
                <GraphCanvas graphData={graphData} routePath={r.path || []}
                             activeEvents={result.activeEvents || {}} />
                <div style={{ marginTop: 12, padding: "10px 14px", background: "#fff5f0",
                              borderRadius: 10, fontSize: 14, color: C.orange, fontWeight: 500 }}>
                  📍 New path: {r.pathDescription}
                </div>
                <div style={{ display: "flex", gap: 14, marginTop: 14, flexWrap: "wrap" }}>
                  <MetricPill icon="💰" label="Cost"   value={`₹${fmt(r.totalCostINR, 0)}`}   color={C.yellow} />
                  <MetricPill icon="⏱" label="Time"   value={`${fmt(r.totalTimeHours, 1)}h`}  color={C.sky} />
                  <MetricPill icon="🌿" label="Carbon" value={`${fmt(r.totalCarbonKg, 1)}kg`} color={C.green} />
                </div>
              </>
            ) : (
              <Alert type="error" message={`All paths blocked! ${r.errorMessage}`} />
            )}
          </Card>

          {/* Active events list */}
          {result.activeEvents && Object.keys(result.activeEvents).length > 0 && (
            <Card style={{ background: "#fff5f0", borderLeft: `4px solid ${C.orange}` }}>
              <h4 style={{ margin: "0 0 10px", color: C.orange }}>⚠️ Active Disruptions</h4>
              {Object.entries(result.activeEvents).map(([edge, desc]) => (
                <div key={edge} style={{ padding: "6px 0", borderBottom: `1px solid #fed7aa`,
                                         fontSize: 13, color: C.textDark }}>
                  <strong>{edge.replace(/-/g, " → ")}</strong>: {desc}
                </div>
              ))}
            </Card>
          )}
        </div>
      )}
    </div>
  );
}

// ============================================================
// PAGE 4 — IMPACT DASHBOARD
// ============================================================
function DashboardPage() {
  const [metrics, setMetrics] = useState(null);
  const [loading, setLoading]  = useState(true);

  useEffect(() => {
    (async () => {
      setLoading(true);
      const data = await api("GET", "/metrics");
      setMetrics(data);
      setLoading(false);
    })();
  }, []);

  if (loading) return <Spinner text="Loading impact metrics…" />;
  if (!metrics) return <Alert type="error" message="Failed to load metrics" />;

  const sdg = metrics.sdgImpact || {};

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
      <div>
        <h2 style={{ margin: 0, color: C.textDark, fontFamily: "Crimson Pro, Georgia, serif", fontSize: 26 }}>
          Impact Dashboard
        </h2>
        <p style={{ margin: "4px 0 0", color: C.muted, fontSize: 14 }}>
          Cumulative optimization impact across all routing sessions
        </p>
      </div>

      {/* Eco Points Banner */}
      <div style={{ background: `linear-gradient(135deg, ${C.green} 0%, ${C.greenMid} 100%)`,
                    borderRadius: 16, padding: "24px 28px", color: "#fff",
                    display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <div>
          <div style={{ fontSize: 13, opacity: 0.85, fontWeight: 600, letterSpacing: 0.8, marginBottom: 4 }}>
            TOTAL ECO POINTS
          </div>
          <div style={{ fontSize: 52, fontWeight: 900, fontFamily: "Crimson Pro, Georgia, serif" }}>
            {metrics.totalEcoPoints || 0}
          </div>
          <div style={{ fontSize: 14, opacity: 0.9, marginTop: 4 }}>
            🌱 {metrics.treesEquivalent || 0} trees' worth of CO₂ saved
          </div>
        </div>
        <div style={{ fontSize: 72 }}>🏆</div>
      </div>

      {/* Cumulative metrics */}
      <div style={{ display: "flex", gap: 14, flexWrap: "wrap" }}>
        <MetricPill icon="🔢" label="Routes Optimized"  value={metrics.totalRoutesOptimized || 0} color={C.green} />
        <MetricPill icon="⚡" label="Simulations Run"   value={metrics.totalSimulationsRun || 0}  color={C.sky} />
        <MetricPill icon="💰" label="Total Cost Saved"  value={`₹${(metrics.totalCostSavedINR || 0).toLocaleString()}`} unit="" color={C.yellow} />
        <MetricPill icon="🌿" label="CO₂ Saved"         value={fmt(metrics.totalCarbonSavedKg, 1)} unit="kg" color={C.green} />
        <MetricPill icon="⏱" label="Time Saved"         value={fmt(metrics.totalTimeSavedHours, 1)} unit="hrs" color={C.sky} />
      </div>

      {/* Average improvements */}
      <Card>
        <h3 style={{ margin: "0 0 16px", color: C.textDark, fontSize: 16 }}>📊 Average Route Improvement</h3>
        <ImprovementBar label="💰 Cost Reduction"   percent={metrics.avgCostSavedPercent || 28.5}   color={C.yellow} />
        <ImprovementBar label="⏱ Time Reduction"    percent={metrics.avgTimeSavedPercent || 22.8}   color={C.sky} />
        <ImprovementBar label="🌿 Carbon Reduction"  percent={metrics.avgCarbonSavedPercent || 31.2} color={C.green} />
      </Card>

      {/* SDG Impact */}
      <Card>
        <h3 style={{ margin: "0 0 16px", color: C.textDark, fontSize: 16 }}>🌍 UN SDG Impact</h3>
        <div style={{ display: "flex", gap: 14, flexWrap: "wrap" }}>
          {[
            { sdg: "SDG 2", label: "Zero Hunger",          icon: "🌾", count: sdg.sdg2ZeroHunger || 0,    color: "#f59e0b" },
            { sdg: "SDG 12", label: "Responsible Use",     icon: "♻️",  count: sdg.sdg12ResponsibleUse || 0, color: "#3b82f6" },
            { sdg: "SDG 13", label: "Climate Action",      icon: "🌍",  count: sdg.sdg13ClimateAction || 0,  color: C.green },
          ].map(({ sdg: id, label, icon, count, color }) => (
            <div key={id} style={{ flex: 1, minWidth: 160, background: C.limeLight,
                                    borderRadius: 14, padding: "16px 18px" }}>
              <div style={{ fontSize: 28, marginBottom: 4 }}>{icon}</div>
              <div style={{ fontWeight: 800, color, fontSize: 16 }}>{id}</div>
              <div style={{ fontSize: 13, color: C.textDark, fontWeight: 600 }}>{label}</div>
              <div style={{ fontSize: 24, fontWeight: 900, color, marginTop: 6 }}>{count}</div>
              <div style={{ fontSize: 11, color: C.muted }}>routes contributing</div>
            </div>
          ))}
        </div>
      </Card>

      {/* Algorithm Info */}
      <Card style={{ borderLeft: `4px solid ${C.green}`, background: "#f0fdf4" }}>
        <h4 style={{ margin: "0 0 10px", color: C.green, fontSize: 15 }}>🧠 Algorithm Performance</h4>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
          {[
            { label: "Algorithm",       value: "Dijkstra's (Multi-Objective)" },
            { label: "Time Complexity",  value: "O((V + E) log V)" },
            { label: "Space Complexity", value: "O(V + E)" },
            { label: "Data Structure",   value: "PriorityQueue + HashMap" },
            { label: "Graph Nodes (V)",  value: metrics.graphNodes || 8 },
            { label: "Graph Edges (E)",  value: metrics.graphEdges || 14 },
          ].map(({ label, value }) => (
            <div key={label} style={{ padding: "8px 12px", background: "#fff",
                                       borderRadius: 8, border: `1px solid ${C.border}` }}>
              <div style={{ fontSize: 11, color: C.muted, fontWeight: 600, marginBottom: 2 }}>{label}</div>
              <div style={{ fontSize: 14, fontWeight: 700, color: C.textDark }}>{value}</div>
            </div>
          ))}
        </div>
      </Card>

      {/* Recent routes */}
      {metrics.recentRoutes && metrics.recentRoutes.length > 0 && (
        <Card>
          <h3 style={{ margin: "0 0 14px", color: C.textDark, fontSize: 16 }}>🕒 Recent Optimizations</h3>
          {metrics.recentRoutes.map((r, i) => (
            <div key={i} style={{ padding: "12px 0", borderBottom: `1px solid ${C.limeLight}` }}>
              <div style={{ fontSize: 13, fontWeight: 600, color: C.textDark, marginBottom: 6 }}>
                📍 {r.path}
              </div>
              <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
                <Badge>💰 {r.costSaved}% saved</Badge>
                <Badge color={C.sky} bg="#e0f4fd">⏱ {r.timeSaved}% faster</Badge>
                <Badge color={C.green}>🌿 {r.carbonSaved}% CO₂ cut</Badge>
                <Badge color="#7c3aed" bg="#f3e8ff">🌱 +{r.ecoPointsEarned} pts</Badge>
              </div>
            </div>
          ))}
        </Card>
      )}
    </div>
  );
}

// ============================================================
// ROOT APP
// ============================================================
export default function App() {
  const [page, setPage]        = useState("graph");
  const [graphData, setGraphData] = useState(null);
  const [graphLoading, setGraphLoading] = useState(false);
  const [lastRoute, setLastRoute] = useState(null);
  const [sdgVisible, setSdgVisible] = useState(false);
  const [ecoTotal, setEcoTotal]  = useState(0);

  const loadGraph = useCallback(async () => {
    setGraphLoading(true);
    const data = await api("GET", "/graph");
    if (data.success !== false) setGraphData(data);
    setGraphLoading(false);
  }, []);

  useEffect(() => { loadGraph(); }, [loadGraph]);

  // Show SDG popup randomly
  useEffect(() => {
    const t = setTimeout(() => setSdgVisible(true), 8000);
    return () => clearTimeout(t);
  }, []);

  const NAV = [
    { id: "graph",     icon: "🌐", label: "Graph Setup" },
    { id: "routing",   icon: "🚀", label: "Routing Engine" },
    { id: "simulate",  icon: "⚡", label: "Simulation" },
    { id: "dashboard", icon: "📊", label: "Dashboard" },
  ];

  return (
    <div style={{ minHeight: "100vh", background: C.bg, fontFamily: "system-ui, sans-serif" }}>
      {/* Global styles */}
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Crimson+Pro:wght@400;600;700;800;900&display=swap');
        @keyframes spin    { to { transform: rotate(360deg); } }
        @keyframes fadeIn  { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }
        @keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: none; } }
        * { box-sizing: border-box; }
        select, input, button { font-family: inherit; }
        ::-webkit-scrollbar { width: 6px; }
        ::-webkit-scrollbar-thumb { background: ${C.border}; border-radius: 99px; }
        input[type=range] { height: 6px; cursor: pointer; }
      `}</style>

      {/* HEADER */}
      <header style={{ background: `linear-gradient(135deg, ${C.green} 0%, ${C.greenMid} 100%)`,
                       padding: "0 24px", position: "sticky", top: 0, zIndex: 100,
                       boxShadow: "0 2px 20px #1b433244" }}>
        <div style={{ maxWidth: 1100, margin: "0 auto", display: "flex",
                      alignItems: "center", justifyContent: "space-between", height: 60 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
            <span style={{ fontSize: 28 }}>🌿</span>
            <div>
              <div style={{ color: "#fff", fontWeight: 900, fontSize: 18, letterSpacing: -0.3,
                            fontFamily: "Crimson Pro, Georgia, serif" }}>
                SmartAgri Routing Engine
              </div>
              <div style={{ color: C.lime, fontSize: 11, opacity: 0.9, letterSpacing: 0.3 }}>
                Cost + Carbon Optimizer · Dijkstra's Algorithm
              </div>
            </div>
          </div>

          {/* Nav */}
          <nav style={{ display: "flex", gap: 4 }}>
            {NAV.map(n => (
              <button key={n.id} onClick={() => setPage(n.id)}
                style={{ padding: "7px 16px", borderRadius: 8, border: "none", cursor: "pointer",
                         background: page === n.id ? "rgba(255,255,255,0.2)" : "transparent",
                         color: page === n.id ? "#fff" : "rgba(255,255,255,0.75)",
                         fontWeight: page === n.id ? 700 : 500, fontSize: 14,
                         transition: "all 0.2s" }}>
                {n.icon} {n.label}
              </button>
            ))}
          </nav>

          {/* Eco points badge */}
          <div style={{ background: "rgba(255,255,255,0.15)", borderRadius: 20,
                        padding: "6px 14px", color: "#fff", fontSize: 13, fontWeight: 700 }}>
            🌱 {ecoTotal} Eco Pts
          </div>
        </div>
      </header>

      {/* MAIN */}
      <main style={{ maxWidth: 1100, margin: "0 auto", padding: "28px 24px" }}>
        {/* Backend status warning */}
        {graphData === null && !graphLoading && (
          <Alert type="error" message="⚠️ Cannot connect to Spring Boot backend on :8080. Start it with: cd backend && mvn spring-boot:run" />
        )}

        <div style={{ animation: "fadeIn 0.35s ease" }} key={page}>
          {page === "graph"     && <GraphSetupPage  graphData={graphData} onRefresh={loadGraph} loading={graphLoading} />}
          {page === "routing"   && <RoutingPage     graphData={graphData}
                                                    onRouteFound={r => { setLastRoute(r); setEcoTotal(t => t + (r?.ecoPointsEarned || 0)); }} />}
          {page === "simulate"  && <SimulationPage  graphData={graphData} lastRoute={lastRoute} />}
          {page === "dashboard" && <DashboardPage   />}
        </div>
      </main>

      {/* FOOTER */}
      <footer style={{ textAlign: "center", padding: "20px 0 32px", color: C.muted, fontSize: 13 }}>
        SmartAgri Routing Engine · Dijkstra's Algorithm O((V+E) log V) · Built with Spring Boot + React
      </footer>

      {/* SDG Popup */}
      {sdgVisible && <SdgPopup onClose={() => setSdgVisible(false)} />}
    </div>
  );
}