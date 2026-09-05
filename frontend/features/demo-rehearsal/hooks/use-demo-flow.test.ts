import { describe, expect, it } from "vitest";
import { demoOutfits } from "../data/outfits";
import {
  advanceDemoFlow,
  confirmDemoOutfit,
  initialDemoFlowSnapshot,
  timedDemoTransition,
} from "../lib/demo-flow";

describe("demo flow", () => {
  it("고정 브리핑 두 답변을 처리한 뒤 옷 선택으로 이동한다", () => {
    let state = advanceDemoFlow(initialDemoFlowSnapshot);
    expect(state.briefingStep).toBe("INITIAL_TRANSCRIPT");
    state = timedDemoTransition(state)?.next ?? state;
    expect(state.briefingStep).toBe("ANALYZING");
    state = timedDemoTransition(state)?.next ?? state;
    expect(state.briefingStep).toBe("FOLLOW_UP_QUESTION");
    state = advanceDemoFlow(state);
    state = timedDemoTransition(state)?.next ?? state;
    state = timedDemoTransition(state)?.next ?? state;
    expect(state.phase).toBe("outfit");
  });

  it("첫 턴 코칭과 둘째 턴 성공 뒤 티켓으로 이동한다", () => {
    let state = confirmDemoOutfit(initialDemoFlowSnapshot, demoOutfits[0]);
    state = timedDemoTransition(state)?.next ?? state;
    expect(state.simulationStep).toBe("ANSWERING");

    state = advanceDemoFlow(state);
    state = timedDemoTransition(state)?.next ?? state;
    state = timedDemoTransition(state)?.next ?? state;
    expect(state.simulationStep).toBe("FEEDBACK");
    expect(state.simulationTurnIndex).toBe(0);

    state = advanceDemoFlow(state);
    state = timedDemoTransition(state)?.next ?? state;
    state = advanceDemoFlow(state);
    state = timedDemoTransition(state)?.next ?? state;
    state = timedDemoTransition(state)?.next ?? state;
    state = advanceDemoFlow(state);

    expect(state.phase).toBe("ticket");
  });
});
