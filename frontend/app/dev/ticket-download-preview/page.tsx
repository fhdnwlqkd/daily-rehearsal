import { notFound } from "next/navigation";

import { TicketDownloadPreview } from "@/features/smart-mirror";

const previewSituations = new Set(["date", "interview", "first-day"]);

export default async function TicketDownloadPreviewPage({
  searchParams,
}: {
  searchParams: Promise<{ situation?: string }>;
}) {
  if (process.env.NODE_ENV !== "development") {
    notFound();
  }

  const { situation } = await searchParams;
  const previewSituation = previewSituations.has(situation ?? "")
    ? (situation as "date" | "interview" | "first-day")
    : "date";

  return <TicketDownloadPreview situation={previewSituation} />;
}
