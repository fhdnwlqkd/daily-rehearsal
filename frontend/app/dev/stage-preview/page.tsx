import { notFound } from "next/navigation";

import { StagePreview } from "@/features/smart-mirror";

const previewStages = new Set([
  "type-select",
  "briefing",
  "outfit",
  "simulation",
  "ticket",
]);

export default async function StagePreviewPage({
  searchParams,
}: {
  searchParams: Promise<{ stage?: string }>;
}) {
  // 실제 전시 배포에는 테스트 데이터가 포함된 화면을 노출하지 않는다.
  if (process.env.NODE_ENV !== "development") {
    notFound();
  }

  const { stage } = await searchParams;
  const previewStage = previewStages.has(stage ?? "")
    ? (stage as "type-select" | "briefing" | "outfit" | "simulation" | "ticket")
    : "type-select";

  return <StagePreview stage={previewStage} />;
}
