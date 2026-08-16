/** API 문구의 마침표·물음표·느낌표를 보존하며 문장 단위 표시 행으로 나눈다. */
export function splitSentences(text: string): string[] {
  const normalized = text.trim();
  if (!normalized) return [];
  return normalized.split(/(?<=[.!?。！？])\s+/u).filter(Boolean);
}
