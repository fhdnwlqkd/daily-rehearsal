package com.rehearsal.domain.ticket.model;

import com.rehearsal.domain.core.annotation.Description;

@Description("사용자가 실전에서 이어갈 행동 계획을 담는 변화 카드")
public record ChangeCard(String todayAction, String tomorrowAttitude, String ifThenPlan) {

  public ChangeCard {
    todayAction = requireText(todayAction, "todayAction");
    tomorrowAttitude = requireText(tomorrowAttitude, "tomorrowAttitude");
    ifThenPlan = requireText(ifThenPlan, "ifThenPlan");
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value.strip();
  }
}
