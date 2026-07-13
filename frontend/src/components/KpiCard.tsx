export function KpiCard({ label, value, size = 28 }: { label: string; value: string | number; size?: number }) {
  return (
    <div className="card" style={{ padding: "18px 20px" }}>
      <div style={{ fontSize: 12, fontWeight: 700, color: "var(--color-text-secondary)", marginBottom: 8 }}>
        {label}
      </div>
      <div style={{ fontSize: size, fontWeight: 800 }}>{value}</div>
    </div>
  );
}
