package com.rehearsal.domain.ticket.model;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.ConversationHistory;
import com.rehearsal.domain.rehearsal.model.TurnEvaluation;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;

@Description("발급 완료된 티켓 내용 및 QR payload")
public record TicketPayload(
    @Description("AI 또는 fallback으로 생성된 티켓 제목") String title,
    @Description("AI 또는 fallback으로 생성된 티켓 문구") String message,
    @Description("title/message가 AI 실패로 정적 fallback을 사용했는지 여부") boolean fallback,
    SituationType situationType,
    String selectedOutfitId,
    List<ConversationHistory> conversationHistory,
    List<TurnEvaluation> turnEvaluations,
    @Description("실제 업로드된 영상 URL. 업로드 미완료 시 null일 수 있다") String videoUrl,
    @Description("videoUploadStatus == COMPLETED 여부") boolean videoAvailable,
    @Description("영상이 준비되지 않았으면 fallback URL로 대체된 다운로드 링크") String downloadUrl,
    @Description("QR 코드로 인코딩할 payload. 현재는 downloadUrl과 동일한 값") String qrPayload) {}
