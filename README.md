# Daily Rehearsal

> 내일의 상황에 맞는 나를 미리 입어보고, 듣고, 말해보는 Daily Simulator

## 1. 기획 배경

현대인들은 이미 매일 작은 방식으로 내일을 대비합니다. 알람을 맞추고, 날씨를 확인하고, 일정에 맞는 옷을 고르고, 필요한 준비물을 챙깁니다. 하지만 이런 대비는 대부분 단편적인 정보 확인에 머무릅니다.

내일 중요한 발표가 있어도 우리는 발표장의 공기, 예상 질문, 그 순간의 몸 상태, 말이 막혔을 때의 감각까지 미리 겪어보지는 못합니다. 소개팅이 있어도 그 자리에서 어떤 인상을 줄지, 어떤 태도로 대화를 시작할지, 어떤 모습의 내가 더 자연스러울지 실제로 시뮬레이션하지는 못합니다.

미래의 AI 비서는 단순히 일정을 알려주는 도구를 넘어설 수 있습니다. 나의 일정, 컨디션, 지난 경험, 날씨, 예약 상황, 이동 동선처럼 내일에 영향을 주는 정보를 종합해 하루를 미리 시뮬레이션하고, 불안과 긴장을 낮추는 행동 변화를 제안할 수 있습니다.

Daily Rehearsal은 이 미래의 AI 비서를 전시장 안에서 체험 가능한 형태로 구현합니다.

## 2. 보험을 넘어서는 보험

기존의 보험은 일이 벌어진 뒤 손실을 보상하는 방식에 가깝습니다. 하지만 미래의 보험은 사고 이후의 보상만이 아니라, 사용자가 더 나은 선택을 하도록 돕고 위험을 줄이는 방향으로 확장될 수 있습니다.

Daily Rehearsal은 이 지점을 “보험을 넘어서는 보험”으로 해석합니다.

이 프로젝트에서 보험은 물리적 사고나 금전적 손실만을 다루지 않습니다. 사용자가 내일 겪을 수 있는 긴장, 실수, 불안, 어색함, 준비 부족 같은 일상의 리스크를 미리 경험하게 하고, 그 경험을 바탕으로 행동을 바꾸도록 돕는 예방적 경험입니다.

즉 Daily Rehearsal은 다음 질문에서 출발합니다.

> 보험이 손실을 보상하는 것을 넘어, 내일의 나를 더 나은 상태로 바꾸는 경험이 될 수 있을까?

## 3. 핵심 키워드: 변화

Daily Rehearsal의 핵심 키워드는 “변화”입니다.

이 변화는 거창한 인생 변화가 아닙니다. 내일 발표장에서 등을 조금 더 펴고 앉는 것, 첫 문장을 더 침착하게 시작하는 것, 어색한 만남에서 먼저 눈을 맞추는 것, 중요한 하루에 맞는 옷을 미리 입어보는 것처럼 작지만 실제적인 변화입니다.

프로젝트의 목적은 사용자를 분석하는 데서 끝나지 않습니다. 사용자가 내일을 미리 겪어보고, 그 예측을 바탕으로 작은 행동 변화를 선택하게 만드는 것입니다.

Daily Rehearsal이 만드는 변화는 다음과 같습니다.

- 단편적인 정보 확인에서 몰입형 하루 시뮬레이션으로
- 불안한 상상에서 직접 참여하는 리허설로
- 사후 보상 중심의 보험에서 사전 행동 변화 중심의 보험 경험으로
- 말하기 코칭에서 몸, 옷, 목소리, 태도를 포함한 생활 시뮬레이션으로

## 4. 프로젝트 정의

Daily Rehearsal은 사용자가 내일 마주할 수 있는 상황과 되고 싶은 태도를 선택하면, AI가 그에 맞는 장면, 상대의 한마디, 복장, 첫 반응을 구성해주는 Daily Simulator입니다.

이 프로젝트는 발표나 면접을 잘하게 만드는 단순 코칭 서비스가 아닙니다. 사용자가 “내일의 나”를 미리 시뮬레이션해보고, 그 장면에 어울리는 모습과 말투를 짧게 경험하게 만드는 리허설 경험입니다.

핵심 컨셉은 “내일의 모드로 갈아입기”입니다.

## 5. 프로젝트 구분

Daily Rehearsal은 두 계열로 나누어 준비합니다.

| 구분 | 우선순위 | 역할 | 핵심 경험 |
| --- | --- | --- | --- |
| 오프라인 전시 | P1 | 전시장용 Daily Simulator | 내일을 짧게 브리핑하고 AI가 채운 맥락으로 즉시 시뮬레이션 |
| 모바일 온라인 전시 | P2 | 개인화된 리허설 빌더 | 객관식 선택으로 더 세밀한 내일의 장면을 조립 |

현재 기준 범위는 P1 오프라인 전시입니다. P1에서 사용자가 무엇을 선택하고 어떻게 체험하는지는 [P1_scenario.md](./P1_scenario.md)에 분리해 정리합니다.

## 6. P1 방향 요약

P1은 전시장 방문자가 긴 설명 없이도 바로 이해할 수 있는 짧은 몰입형 체험입니다.

방문자는 상황 카드를 먼저 고르지 않습니다. “내일 하루를 짧게 말해주세요”라는 안내에 맞춰 일정, 걱정, 되고 싶은 모습을 자유롭게 말합니다. 프론트는 음성을 transcript로 확정하고, rehearsal-api는 DB의 active context slot으로 `situation_type`, `critical_moment`, 페르소나, 복장 방향 같은 맥락을 한 번에 채웁니다.

맥락이 부족하면 AI가 빈 슬롯을 하나씩 캐묻지 않고, 필요한 질문을 한 화면에 묶어 짧게 재질문합니다. 이후 Decart WebRTC preview와 제스처 기반 outfit switching으로 “내일의 나”를 보여주고, 결정적 순간을 한 번 리허설한 뒤 변화 카드로 닫습니다.

P1의 핵심 경험은 다음 문장으로 정리합니다.

> “내일의 상황에 맞는 나를 입어보고, 그 장면에서 첫마디를 먼저 말해본다.”

## 7. 기술의 역할

Daily Rehearsal에서 기술은 각각 따로 보이는 기능이 아니라, 하나의 시뮬레이션 경험을 만들기 위해 사용됩니다.

| 기술 | 경험 안에서의 역할 |
| --- | --- |
| LLM | transcript에서 맥락 slot을 채우고, 결정적 순간 장면과 피드백/변화 카드를 생성 |
| TTS | 브라우저 음성 합성으로 AI 상대의 한마디를 들려주어 장면감을 강화 |
| STT | 프론트에서 사용자의 발화를 transcript로 확정하고 맥락/피드백에 활용 |
| VTON | 프론트가 Decart WebRTC에 직접 연결해 선택한 페르소나의 preview를 생성 |
| Camera/Vision | 전시장 거울 경험, Decart camera stream, 제스처 기반 outfit switching을 담당 |

이 조합의 목적은 “말을 잘했는지 평가”하는 것이 아니라, 사용자가 내일의 장면 속 자기 모습을 한 번 경험하도록 만드는 것입니다.

백엔드의 LLM provider는 AI 작업 단위로 선택합니다. 예를 들어 `slot-extraction`은 Gemini를 쓰고, 이후 `simulation-dialogue`나 `feedback-generation`은 OpenAI를 쓰도록 분리할 수 있습니다. 전역 `multi` 모드나 fallback 순차 실행은 두지 않고, `rehearsal.ai.tasks.<task>` 설정으로 provider와 model을 지정합니다. 로컬 개발에서는 API key 없이 흐름을 확인할 수 있는 `fake` provider를 사용할 수 있습니다. 자세한 설정 방식은 [Rehearsal Modules README](./backend/rehearsal-modules/README.md#ai-provider-routing)에 정리합니다.

API key와 password 같은 secret은 git에 커밋하지 않고 환경변수로 주입합니다. 로컬에서는 backend 모듈의 `.env.example`을 복사해 개인별 `.env.local`을 만들고, AWS 배포에서는 Secrets Manager 값을 환경변수로 연결합니다. 자세한 기준은 [Secret Management](./backend/rehearsal-modules/README.md#secret-management)에 정리합니다.

## 8. P2와의 관계

P2 모바일은 같은 Daily Simulator를 더 개인화된 방식으로 확장하는 계열입니다.

P1이 전시장용 “빠른 시뮬레이션”이라면, P2는 사용자가 더 많은 객관식 질문에 답하면서 자기 상황에 가까운 리허설을 만드는 “개인용 시뮬레이션 빌더”입니다.

```text
Daily Rehearsal
├─ P1 오프라인 전시: 상황 + 맥락 + 페르소나 기반 Daily Simulator
└─ P2 모바일 전시: 객관식 기반 개인화 Daily Simulator
```

## 9. 현재 결정 사항

- 프로젝트명은 Daily Rehearsal로 고정합니다.
- P1은 발표 코치가 아니라 Daily Simulator로 정의합니다.
- P1은 “보험을 넘어서는 보험”을 사전 시뮬레이션과 행동 변화의 경험으로 해석합니다.
- P1은 사용자의 음성 브리핑을 기반으로 active context slot을 채워 시뮬레이션합니다.
- P1의 STT/TTS는 프론트에서 처리하고, 백엔드는 transcript text와 aiLine text를 다룹니다.
- P1의 VTON preview는 프론트가 Decart WebRTC에 직접 연결하고, 백엔드는 client token/spec/reference image URL을 내려줍니다.
- 백엔드 LLM provider는 작업별 route로 선택하며, 현재 연결된 작업은 `slot-extraction`입니다. 로컬에서는 `fake` provider로 API key 없이 개발할 수 있습니다.
- API key와 password는 환경변수로 주입하고, 실제 secret 값은 git에 저장하지 않습니다.
- P1은 중요한 일이 없는 사람도 체험할 수 있어야 합니다.
- P1은 개인 일정 연동 없이도 자기 상황처럼 느껴지는 브리핑 기반 구조를 사용합니다.
- P2 모바일은 후속 확장으로 분리합니다.

## 10. 다음 단계

1. P1 active context slot과 기본값을 확정합니다.
2. `situation_type`, `critical_moment`, 페르소나, 복장 방향 slot의 option을 정리합니다.
3. 페르소나/복장 방향별 Decart prompt와 reference image 후보를 정의합니다.
4. gesture-fit 단계의 outfit switching gesture와 fallback 조작을 확정합니다.
5. briefing, context, gesture-fit, rehearsal, change-card 화면 카피를 작성합니다.
