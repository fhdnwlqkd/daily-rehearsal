import type {
  TicketJobResponse,
  VideoUploadResponse,
  VideoUploadStatus,
} from "../../types";

/** NONE은 업로드 요청이 아직 도착하지 않은 상태일 수 있어 모바일에서 계속 확인한다. */
export function isVideoTerminal(status: VideoUploadStatus) {
  return status === "COMPLETED" || status === "FAILED";
}

/** 영상 상태 조회와 티켓 생성 시점의 완료 플래그 중 하나라도 완료면 다운로드를 연다. */
export function isVideoDownloadAvailable(
  video: VideoUploadResponse | null,
  ticket: TicketJobResponse,
) {
  return video?.status === "COMPLETED" || ticket.videoAvailable === true;
}
