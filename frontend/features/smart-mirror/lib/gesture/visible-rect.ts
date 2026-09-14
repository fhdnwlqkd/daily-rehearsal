export interface SourceRect {
  /** 카메라 프레임 픽셀 좌표 — drawImage의 sx, sy, sw, sh에 그대로 넣는다 */
  sx: number;
  sy: number;
  sw: number;
  sh: number;
}

export interface VisibleSourceRectArgs {
  /** 카메라 트랙의 실제 해상도 */
  videoWidth: number;
  videoHeight: number;
  /** 화면에 이 영상이 그려지는 박스 크기(뷰포트) */
  displayWidth: number;
  displayHeight: number;
  /**
   * 보이는 영역을 넓히거나(+) 좁히는(−) 비율. 양수는 화면 가장자리에 걸친
   * 손이 잘려 인식이 끊기는 걸 막고, 음수는 더 좁혀서 옆사람을 밀어낸다.
   */
  margin?: number;
}

/**
 * `object-cover`로 표시되는 영상에서 **실제로 화면에 보이는 부분**의
 * 원본 좌표를 역산한다.
 *
 * 전시장 카메라는 세로 모니터(1080×1920)보다 훨씬 넓게 찍는다. 화면은
 * object-cover라 가운데 세로 띠만 보이는데, MediaPipe에는 프레임 전체가
 * 들어가서 화면 밖에 서 있는 관람객의 손까지 후보가 된다 — 인식이 엉뚱한
 * 사람에게 붙는 원인이다. 인식 입력을 이 사각형으로 잘라내면 "화면에
 * 보이는 사람만 인식한다"가 성립한다.
 *
 * 부수 효과로 제스처 임계값(정규화 좌표 기준)의 의미도 정상화된다. 넓은
 * 프레임에서는 사람이 작게 찍혀 같은 팔 동작의 정규화 이동량이 줄어드는데,
 * 잘라내면 "사람이 프레임을 채운" 상태 — 임계 상수를 튜닝했던 그 조건 — 로
 * 돌아온다.
 */
export function computeVisibleSourceRect({
  videoWidth,
  videoHeight,
  displayWidth,
  displayHeight,
  margin = 0,
}: VisibleSourceRectArgs): SourceRect {
  // 아직 메타데이터가 없거나(0) 화면 크기를 모르면 자르지 않는다 —
  // 잘못된 크롭보다 전체 프레임이 낫다.
  if (
    videoWidth <= 0 ||
    videoHeight <= 0 ||
    displayWidth <= 0 ||
    displayHeight <= 0
  ) {
    return { sx: 0, sy: 0, sw: videoWidth, sh: videoHeight };
  }

  // object-cover: 짧은 축을 채우도록 확대하고 긴 축은 잘라낸다.
  const scale = Math.max(
    displayWidth / videoWidth,
    displayHeight / videoHeight,
  );
  const visibleWidth = displayWidth / scale;
  const visibleHeight = displayHeight / scale;

  // 음수 margin으로 좁힐 수 있게 하되, 0 이하로 뒤집히지는 않게 막는다.
  const grow = Math.max(0.1, 1 + margin);
  const sw = Math.min(videoWidth, visibleWidth * grow);
  const sh = Math.min(videoHeight, visibleHeight * grow);

  return {
    sx: (videoWidth - sw) / 2,
    sy: (videoHeight - sh) / 2,
    sw,
    sh,
  };
}
