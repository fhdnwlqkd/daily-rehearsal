interface StageCountdownProps {
  label: string;
  remainingSeconds: number;
}

export function StageCountdown({
  label,
  remainingSeconds,
}: StageCountdownProps) {
  const urgent = remainingSeconds <= 10;

  return (
    <div
      className={`pointer-events-none absolute top-16 right-8 z-30 min-w-28 rounded-2xl border px-4 py-3 text-right backdrop-blur-xl transition-colors ${
        urgent
          ? "border-amber-300/50 bg-amber-950/55 text-amber-100"
          : "border-white/15 bg-black/45 text-white"
      }`}
      aria-label={`${label} ${remainingSeconds}초`}
    >
      <p className="text-[10px] font-light tracking-[0.22em] opacity-65">
        {label}
      </p>
      <p className="mt-1 text-2xl font-light tabular-nums">
        {remainingSeconds}초
      </p>
    </div>
  );
}
