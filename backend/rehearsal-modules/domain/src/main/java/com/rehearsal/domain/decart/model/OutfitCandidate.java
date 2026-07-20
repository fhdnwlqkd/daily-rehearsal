package com.rehearsal.domain.decart.model;

import com.rehearsal.domain.core.annotation.Description;

@Description("옷 후보 목록 조회 응답에 쓰이는 outfit 후보 항목")
public record OutfitCandidate(String outfitId, String label, String thumbnailUrl, boolean defaultOutfit) {}
