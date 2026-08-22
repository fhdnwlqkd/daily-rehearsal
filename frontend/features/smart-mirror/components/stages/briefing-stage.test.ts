import { describe, expect, it } from "vitest";

import { splitSentences } from "../../lib/briefing/sentences";

describe("splitSentences", () => {
  it("keeps Korean briefing copy in semantic sentence rows", () => {
    expect(
      splitSentences(
        "모두 답하지 않아도 괜찮아요. 편한 것 한두 가지만 이야기해주세요. 더 필요한 내용은 짧게 여쭤볼게요.",
      ),
    ).toEqual([
      "모두 답하지 않아도 괜찮아요.",
      "편한 것 한두 가지만 이야기해주세요.",
      "더 필요한 내용은 짧게 여쭤볼게요.",
    ]);
  });

  it("returns no visual rows for blank copy", () => {
    expect(splitSentences("   ")).toEqual([]);
  });
});
