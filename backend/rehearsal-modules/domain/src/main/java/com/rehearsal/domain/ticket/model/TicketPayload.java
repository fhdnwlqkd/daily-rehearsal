package com.rehearsal.domain.ticket.model;

import com.rehearsal.domain.core.annotation.Description;

@Description("발급 완료된 티켓 내용 및 QR payload")
public record TicketPayload(
    TicketSnapshot snapshot,
    ChangeCard changeCard,
    boolean fallback,
    @Description("실제 업로드된 영상 URL. 업로드 미완료 시 null일 수 있다") String videoUrl,
    @Description("videoUploadStatus == COMPLETED 여부") boolean videoAvailable,
    @Description("영상이 준비되지 않았으면 fallback URL로 대체된 다운로드 링크") String downloadUrl,
    @Description("QR 코드로 인코딩할 payload. 현재는 downloadUrl과 동일한 값") String qrPayload) {}
