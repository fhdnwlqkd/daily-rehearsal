package com.rehearsal.datasource.dbintegrated.session.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "rehearsal_result",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_rehearsal_result_session", columnNames = "session_id"))
public class RehearsalResultJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", nullable = false, unique = true)
  private RehearsalSessionJpaEntity session;

  @Column(name = "video_url", length = 500)
  private String videoUrl;

  @Column(name = "ticket_summary", columnDefinition = "TEXT")
  private String ticketSummary;

  @Column(name = "download_url", length = 500)
  private String downloadUrl;

  public static RehearsalResultJpaEntity create(
      RehearsalSessionJpaEntity session,
      String videoUrl,
      String ticketSummary,
      String downloadUrl) {
    RehearsalResultJpaEntity entity = new RehearsalResultJpaEntity();
    entity.session = session;
    entity.update(videoUrl, ticketSummary, downloadUrl);
    return entity;
  }

  public void update(String videoUrl, String ticketSummary, String downloadUrl) {
    this.videoUrl = videoUrl;
    this.ticketSummary = ticketSummary;
    this.downloadUrl = downloadUrl;
  }
}
