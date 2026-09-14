# 제스처 인식 영역(크롭) 검증법

"화면 밖에 서 있는 사람은 MediaPipe에 들어가지 않는다"를 확인하는 방법.
전시장 카메라가 세로 모니터보다 훨씬 넓게 찍어서 옆사람 손이 인식 후보로
들어오던 문제(2026-09-14 현장)를 고친 뒤의 회귀 검증용이다.

## 왜 썸네일만 보면 되는가

인식 호출은 `use-gesture-controller.ts`의 `recognizeForVideo(prepareInput(), now)`
**한 곳뿐**이고, `prepareInput()`은 크롭된 캔버스를 돌려준다. 즉 MediaPipe가
보는 픽셀은 그 캔버스가 전부다. 디버그 오버레이(`D` 키)의 우측 하단
**"MediaPipe 입력"** 썸네일이 바로 그 캔버스를 그대로 띄운 것이므로,

- 썸네일에 안 보이는 사람 → 인식될 방법이 없다
- 썸네일에 보이는 사람 → 아직 인식 후보다

썸네일이 아예 안 뜨면 크롭이 동작하지 않은 것이다(HUD의 `인식역` 줄도
`전체 — 크롭 안 됨`으로 바뀐다).

## 1) 현장 검증 (사람 두 명, 30초)

1. `D`로 디버그 오버레이를 켠다.
2. 관람객 위치에 한 명이 선다. 썸네일에 그 사람만 보이는지 확인.
3. 다른 한 명이 화면에는 안 나오지만 카메라 화각 안인 위치(좌우 바깥)에서
   손을 흔든다. → 썸네일에 안 나타나고 HUD `hand`가 `없음`을 유지하면 통과.
4. 그 사람이 화면 안으로 들어오면 손이 잡히는지도 확인(반대 방향 검증).

옆사람이 여전히 잡히면 `constants.ts`의 `GESTURE_CROP_MARGIN`을 음수로
내린다(`-0.2` = 보이는 영역보다 20% 더 좁게). 점선 경계와 썸네일이 즉시 반응한다.

## 2) 카메라 없이 재현 (개발 PC, DevTools 콘솔)

광각 카메라를 흉내 낸 가짜 스트림을 주입해 크롭 기하를 검증한다. 좌/우에
화면 밖 표식, 가운데에 표식을 그려두고 썸네일에 가운데만 남는지 본다.

1. 세로 뷰포트로 만든다(예: 540×960 — 1080×1920과 같은 9:16).
2. 카메라 권한을 거부해 "권한 설정이 필요합니다" 화면을 띄운다.
3. 콘솔에 아래를 붙여넣는다.

```js
const c = document.createElement("canvas");
c.width = 1920;
c.height = 1080;
const g = c.getContext("2d");
let t = 0;
(function paint() {
  g.fillStyle = "#111";
  g.fillRect(0, 0, 1920, 1080);
  g.fillStyle = "#e11d48";
  g.fillRect(0, 0, 500, 1080); // 좌: 화면 밖
  g.fillStyle = "#22c55e";
  g.fillRect(656, 0, 608, 1080); // 중앙: 화면에 보이는 띠
  g.fillStyle = "#3b82f6";
  g.fillRect(1420, 0, 500, 1080); // 우: 화면 밖
  g.fillStyle = "#fff";
  g.font = "bold 64px sans-serif";
  g.fillText("LEFT-OUT", 40, 540);
  g.fillText("CENTER", 760, 540);
  g.fillText("RIGHT-OUT", 1450, 540);
  g.fillText(String(t++), 760, 700);
  requestAnimationFrame(paint);
})();
const fake = c.captureStream(30);
Object.defineProperty(navigator.mediaDevices, "getUserMedia", {
  configurable: true,
  value: async () => fake,
});
```

4. "다시 시도하기"를 누르고 `D`로 오버레이를 켠다.
5. 썸네일에 **가운데 표식만** 보이면 통과. 빨강/파랑이 보이면 크롭이 화면과
   어긋난 것이다.

픽셀로 못 박으려면(썸네일 위치는 dpr 2 · 540×960 기준):

```js
const g2 = document.querySelector("canvas").getContext("2d");
const d = g2.getImageData(782, 1450, 264, 436).data;
let red = 0,
  blue = 0;
for (let i = 0; i < d.length; i += 4) {
  if (d[i + 3] < 10) continue;
  if (d[i] > 150 && d[i + 1] < 90 && d[i + 2] < 110) red++;
  if (d[i + 2] > 180 && d[i] < 110) blue++;
}
console.log({ red, blue }); // 둘 다 0이어야 한다
```

실측(2026-09-14): 카메라 1920×1080 · 뷰포트 540×960에서 인식역 656×1080
(프레임 폭의 34%), 썸네일 내 빨강 0 · 파랑 0.

## 검증 못 하는 부분

- **실제 손의 좌표 정합**은 카메라가 있어야 한다. 옷 입히기 단계에서 손을
  들어 스켈레톤이 실제 손 위에 얹히는지 눈으로 봐야 한다.
- 옷 입히기~시뮬레이션 구간의 화면은 카메라가 아니라 **Decart 출력**이다.
  크롭은 카메라 해상도 기준으로 계산하므로, Decart가 입력을 자기 비율로
  센터 크롭한다면 실제 보이는 영역이 계산보다 좁을 수 있다. 어긋나 보이면
  `GESTURE_CROP_MARGIN`을 음수로 조정한다.
