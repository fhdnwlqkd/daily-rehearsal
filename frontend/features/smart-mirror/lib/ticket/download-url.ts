export function buildTicketDownloadUrl(
  origin: string,
  sessionId: string,
): string {
  const baseUrl = new URL(origin);
  if (baseUrl.protocol !== "http:" && baseUrl.protocol !== "https:") {
    throw new Error("Ticket download URL requires an http(s) origin");
  }

  return new URL(
    `/download/${encodeURIComponent(sessionId)}`,
    baseUrl.origin,
  ).toString();
}
