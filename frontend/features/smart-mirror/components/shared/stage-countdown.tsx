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
      className={`pointer-events-none absolute top-[clamp(40px,8vh,64px)] right-[clamp(14px,3vw,32px)] z-30 min-w-20 px-1 py-1 text-right drop-shadow-[0_2px_8px_rgba(0,0,0,0.85)] transition-colors ${
        urgent ? "text-amber-200" : "text-white"
      }`}
      aria-label={`${label} ${remainingSeconds}초`}
    >
      <p className="text-[10px] font-light tracking-[0.22em] whitespace-nowrap opacity-65">
        {label}
      </p>
      <p className="mt-1 text-[clamp(1rem,3vh,1.5rem)] font-light tabular-nums">
        {remainingSeconds}초
      </p>
    </div>
  );
}
