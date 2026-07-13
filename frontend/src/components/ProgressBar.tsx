export function ProgressBar({ value, height = 4, width }: { value: number; height?: number; width?: number }) {
  const pct = Math.max(0, Math.min(1, value)) * 100;
  return (
    <div
      style={{
        height,
        width: width ?? "100%",
        background: "var(--color-row-divider)",
        borderRadius: height / 2,
        overflow: "hidden",
      }}
    >
      <div style={{ height: "100%", width: `${pct}%`, background: "var(--color-gold)" }} />
    </div>
  );
}
