import { notFound } from "next/navigation";

import { TicketPreview } from "@/features/smart-mirror";

const previewSituations = new Set(["date", "interview", "first-day"]);

export default async function TicketPreviewPage({
  searchParams,
}: {
  searchParams: Promise<{ situation?: string }>;
}) {
  // 실제 전시 배포에는 테스트 데이터가 포함된 화면을 노출하지 않는다.
  if (process.env.NODE_ENV !== "development") {
    notFound();
  }

  const { situation } = await searchParams;
  const previewSituation = previewSituations.has(situation ?? "")
    ? (situation as "date" | "interview" | "first-day")
    : "date";

  return <TicketPreview situation={previewSituation} />;
}
