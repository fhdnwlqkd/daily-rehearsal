import type {
  TicketJobResponse,
  VideoUploadResponse,
  VideoUploadStatus,
} from "../../types";

/** 티켓 발급 뒤 NONE이면 업로드 요청 자체가 없었던 것이므로 불필요하게 기다리지 않는다. */
export function isVideoTerminal(status: VideoUploadStatus) {
  return status === "NONE" || status === "COMPLETED" || status === "FAILED";
}

/** 영상 상태 조회와 티켓 생성 시점의 완료 플래그 중 하나라도 완료면 다운로드를 연다. */
export function isVideoDownloadAvailable(
  video: VideoUploadResponse | null,
  ticket: TicketJobResponse,
) {
  return video?.status === "COMPLETED" || ticket.videoAvailable === true;
}
