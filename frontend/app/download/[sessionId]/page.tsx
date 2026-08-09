import { MobileDownloadPage } from "@/features/smart-mirror/components/ticket/mobile-download-page";

interface DownloadPageProps {
  params: Promise<{ sessionId: string }>;
}

export default async function DownloadPage({ params }: DownloadPageProps) {
  const { sessionId } = await params;
  return <MobileDownloadPage sessionId={sessionId} />;
}
