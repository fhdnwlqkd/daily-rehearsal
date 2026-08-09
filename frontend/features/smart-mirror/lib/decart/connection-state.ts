import type { ConnectionState } from "@decartai/sdk";

import type { DecartConnectionStatus } from "../../types";

/**
 * SDK의 소문자 ConnectionState를 우리 대문자 상태로 접는다.
 * reconnecting을 CONNECTING으로 접는 이유: UI 입장에서 "지금 변환 프리뷰가
 * 안 나온다 → 준비 문구를 보여준다"는 행동이 최초 연결과 동일하다.
 */
export function mapDecartConnectionState(
  state: ConnectionState,
): DecartConnectionStatus {
  switch (state) {
    case "connected":
    case "generating":
      return "CONNECTED";
    case "connecting":
    case "reconnecting":
      return "CONNECTING";
    case "disconnected":
      return "CLOSED";
    default:
      return state satisfies never;
  }
}
