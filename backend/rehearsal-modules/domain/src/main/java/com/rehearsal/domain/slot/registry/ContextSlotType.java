package com.rehearsal.domain.slot.registry;

import com.rehearsal.domain.slot.model.SlotType;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContextSlotType {
  SITUATION_DETAIL(
      "situation_detail",
      "구체적인 상황",
      SlotType.TEXT,
      """
      사용자가 실제로 연습하려는 상황의 구체적인 맥락을 짧고 재사용 가능한 명사구로 추출하세요.
      소개팅은 만남의 성격이나 단계, 면접은 지원 직무·면접 유형·단계, 첫 출근은 팀·역할·입사 형태를 우선합니다.
      원하는 인상, 바라는 결과, 걱정되는 순간, 강점, 의상은 이 값에 섞지 마세요.
      사용자가 단순히 소개팅·면접·첫 출근처럼 situation type만 반복했다면 구체 정보가 아니므로 null입니다.
      회사, 직무, 장소, 관계, 역할, 만남 방식을 사용자가 말하지 않았다면 추측하지 마세요.
      좋은 값: '지인 소개로 처음 만나는 소개팅', '백엔드 개발자 기술 면접', '새 결제 개발팀 첫 출근'.
      나쁜 값: '따뜻해 보이고 싶음', '면접이 걱정됨', '단정한 캐주얼'.
      """,
      "어떤 상황을 연습하는지 한마디로 알려주세요.",
      null,
      null,
      List.of()),
  DESIRED_PERSONA(
      "desired_persona",
      "남기고 싶은 인상",
      SlotType.SINGLE_SELECT,
      """
      사용자가 이 상황에서 상대에게 남기고 싶다고 명확히 말한 핵심 인상을 하나의 optionKey로 정규화하세요.
      현재 성격을 평가하지 말고, 보여주고 싶거나 기억되고 싶다고 말한 미래의 인상만 추출하세요.
      calm_confident=차분함·여유·침착함·자신감, warm_natural=따뜻함·편안함·친근함·자연스러움,
      sharp_prepared=또렷함·논리적임·정돈됨·준비됨, curious_engaged=호기심·적극성·관심,
      honest_grounded=솔직함·진정성·꾸밈없음, energetic_positive=밝음·활기·긍정성,
      thoughtful_considerate=사려 깊음·배려·세심함, professional_reliable=전문성·신뢰·책임감,
      collaborative_open=협업·열린 태도·함께하려는 자세에 대응합니다.
      여러 인상을 말하면 '가장', '특히', '무엇보다'처럼 우선순위가 명시된 값을 고르세요.
      우선순위가 없다면 사용자가 가장 명확하게 강조한 의미를 고르고, 충분히 대응하지 않으면 null입니다.
      말하기 방식은 response_style, 강점이나 능력은 user_strength로 보내고 이 값에 섞지 마세요.
      """,
      "상대에게 어떤 인상을 남기고 싶나요?",
      null,
      null,
      List.of(
          ContextSlotOptionType.CALM_CONFIDENT,
          ContextSlotOptionType.WARM_NATURAL,
          ContextSlotOptionType.SHARP_PREPARED,
          ContextSlotOptionType.CURIOUS_ENGAGED,
          ContextSlotOptionType.HONEST_GROUNDED,
          ContextSlotOptionType.ENERGETIC_POSITIVE,
          ContextSlotOptionType.THOUGHTFUL_CONSIDERATE,
          ContextSlotOptionType.PROFESSIONAL_RELIABLE,
          ContextSlotOptionType.COLLABORATIVE_OPEN)),
  DESIRED_OUTCOME(
      "desired_outcome",
      "바라는 결과",
      SlotType.TEXT,
      """
      사용자가 이 상황이 어떻게 끝나기를 원하는지 또는 무엇을 이루면 성공이라고 느끼는지 짧게 추출하세요.
      관찰 가능한 결과, 전달하고 싶은 핵심, 만들고 싶은 관계, 원하는 마무리를 우선합니다.
      좋은 값: '서로 편하게 대화를 마치는 것', '프로젝트 기여도를 분명히 전달하는 것',
      '팀원들과 편하게 질문할 수 있는 관계를 만드는 것'.
      '따뜻해 보이고 싶다' 같은 인상은 desired_persona이고, '긴장하지 않고 말하기' 같은 방식은 response_style입니다.
      '잘됐으면 좋겠다', '성공하고 싶다'처럼 구체적인 결과가 없으면 null을 반환하세요.
      사용자가 말하지 않은 합격, 호감, 다음 약속, 관계 형성을 임의로 목표로 만들지 마세요.
      """,
      "이 상황이 어떻게 끝나면 좋을까요?",
      null,
      null,
      List.of()),
  CONVERSATION_MATERIAL(
      "conversation_material",
      "활용할 대화 소재",
      SlotType.TEXT,
      """
      사용자가 대화나 답변에서 편하게 활용할 수 있다고 말한 주제, 경험 영역, 관심사 또는 업무 소재를 추출하세요.
      실제 사건 하나가 아니라 여러 질문으로 확장 가능한 소재 수준으로 짧게 정리합니다.
      소개팅 예: '전시와 산책', 면접 예: '결제 시스템 성능 개선과 장애 대응',
      첫 출근 예: '백엔드 개발 경험과 협업 방식'.
      구체적인 단일 사건·프로젝트 사례는 supporting_example에 넣고, 피하고 싶은 주제는 interaction_constraint에 넣으세요.
      단순히 상황에 어울릴 것 같다는 이유로 취미, 기술, 경험을 추측하지 마세요.
      사용자가 편하게 말할 수 있는 소재를 명확히 밝히지 않았다면 null입니다.
      """,
      "편하게 이야기하거나 활용할 수 있는 소재 하나를 알려주세요.",
      null,
      null,
      List.of()),
  CRITICAL_MOMENT(
      "critical_moment",
      "가장 어려운 순간",
      SlotType.TEXT,
      """
      사용자가 가장 어렵거나 걱정되거나 집중적으로 연습하고 싶다고 말한 구체적인 순간·질문·행동을 추출하세요.
      상황 전체가 아니라 실제 장면 단위의 짧은 표현으로 정리합니다.
      좋은 값: '첫인사 뒤 대화가 끊기는 순간', '프로젝트에서 본인의 기여도를 설명하는 순간',
      '팀원들 앞에서 처음 자기소개하는 순간'.
      '긴장돼요', '잘하고 싶어요', '소개팅이 걱정돼요'처럼 특정 순간이나 행동이 없는 표현은 null입니다.
      예상 질문의 실제 내용은 anticipated_question에도 함께 담을 수 있지만, 이 값에는 사용자가 어려워하는 장면을 담으세요.
      원하는 결과, 남기고 싶은 인상, 일반적인 상황 설명은 포함하지 마세요.
      """,
      "가장 걱정되거나 집중해서 연습하고 싶은 순간은 언제인가요?",
      null,
      null,
      List.of()),
  COUNTERPART_CONTEXT(
      "counterpart_context",
      "상대 정보",
      SlotType.TEXT,
      """
      사용자가 상호작용할 상대의 역할, 관계, 구성 또는 이미 알고 있는 특성을 명확히 말한 경우에만 추출하세요.
      좋은 값: '지인이 소개해준 처음 만나는 상대', '실무 개발자와 팀 리더', '같은 프로젝트를 담당할 팀원들'.
      상대의 성별, 나이, 성격, 외모, 호감, 직급, 전문성은 사용자가 말하지 않았다면 추측하지 마세요.
      '소개팅 상대', '면접관', '팀원'처럼 situation type만으로 자동 유추되는 일반 표현만 있으면 null이어도 됩니다.
      만남의 장소나 대면·화상 여부는 interaction_setting, 과거에 나눈 대화는 prior_interaction_context로 분리하세요.
      """,
      "누구와 대화하거나 함께하게 되는 상황인가요?",
      null,
      null,
      List.of()),
  INTERACTION_SETTING(
      "interaction_setting",
      "진행 환경",
      SlotType.TEXT,
      """
      사용자가 명확히 말한 상호작용 환경만 짧게 추출하세요.
      대면·화상, 일대일·다대일·팀 전체, 장소, 시간 제약, 공식적·편안한 자리 같은 정보가 대상입니다.
      좋은 값: '카페에서 일대일로 만남', '화상으로 진행되는 다대일 기술 면접',
      '팀 전체가 모인 자리에서 자기소개'.
      사용자가 말하지 않은 장소, 시간, 인원, 온라인 여부를 추측하지 마세요.
      situation_detail에 동일 정보가 있더라도 이 slot에는 환경 요소만 분리해 간결하게 담을 수 있습니다.
      """,
      "대면·화상, 인원이나 장소처럼 진행 환경을 알려주세요.",
      null,
      null,
      List.of()),
  PRIOR_INTERACTION_CONTEXT(
      "prior_interaction_context",
      "이전 상호작용",
      SlotType.TEXT,
      """
      현재 리허설 장면 전에 상대와 이미 있었던 상호작용, 공유된 정보 또는 선행 단계를 사용자가 말한 경우에만 추출하세요.
      좋은 값: '만나기 전에 메신저로 취미 이야기를 나눔', '1차 면접을 통과하고 기술 면접을 앞둠',
      '입사 전에 팀장과 담당 업무를 간단히 이야기함'.
      앞으로 일어나기를 바라는 일은 desired_outcome이며 여기에 넣지 마세요.
      사용자가 말하지 않은 이전 대화, 관계, 평가, 약속을 만들지 마세요.
      """,
      "이전에 상대와 나눈 대화나 먼저 진행된 단계가 있나요?",
      null,
      null,
      List.of()),
  USER_STRENGTH(
      "user_strength",
      "활용할 강점",
      SlotType.TEXT,
      """
      사용자가 이 상황에서 보여주거나 활용하고 싶다고 명확히 말한 자신의 강점, 태도, 능력을 짧게 추출하세요.
      좋은 값: '상대 이야기를 잘 듣는 편', '장애 원인을 끝까지 추적하는 문제 해결력',
      '모르는 것을 솔직하게 질문하는 태도'.
      '자신감 있어 보이고 싶다'는 desired_persona이고, 실제로 잘하거나 활용할 수 있다고 말한 능력이 user_strength입니다.
      supporting_example에 포함된 경험으로부터 강점을 새로 평가하거나 추론하지 말고, 사용자가 직접 표현한 내용만 담으세요.
      직함, 경력, 자격, 성격을 근거 없이 강점으로 바꾸지 마세요.
      """,
      "이 상황에서 보여주고 싶은 자신의 강점은 무엇인가요?",
      null,
      null,
      List.of()),
  SUPPORTING_EXAMPLE(
      "supporting_example",
      "활용할 구체 경험",
      SlotType.TEXT,
      """
      사용자가 대화나 답변에서 활용할 수 있다고 말한 구체적인 경험, 사건, 프로젝트 또는 에피소드를 짧게 추출하세요.
      상황·행동·결과가 모두 없어도 실제로 있었던 사건을 식별할 수 있으면 추출할 수 있습니다.
      좋은 값: '최근 친구와 다녀온 전시 경험', '결제 장애 원인을 분석해 복구 시간을 줄인 프로젝트',
      '이전 팀에서 신규 구성원의 적응을 도운 경험'.
      사건의 핵심 사실만 보존하고 결과, 수치, 역할, 성과를 새로 만들지 마세요.
      넓은 관심사나 기술 영역은 conversation_material, 사용자가 직접 말한 역량은 user_strength로 분리하세요.
      실제 경험이 언급되지 않았다면 null입니다.
      """,
      "답변에 활용할 구체적인 경험이나 사례 하나를 알려주세요.",
      null,
      null,
      List.of()),
  ANTICIPATED_QUESTION(
      "anticipated_question",
      "예상 질문",
      SlotType.TEXT,
      """
      사용자가 상대에게 받을 것으로 예상하거나 걱정한다고 명확히 말한 질문의 핵심 내용을 추출하세요.
      가능하면 의미가 보존되는 짧은 질문 형태로 정리합니다.
      좋은 값: '쉬는 날에는 보통 무엇을 하는지 묻는 질문', '프로젝트에서 본인의 기여도가 무엇인지 묻는 질문',
      '이전 직장에서 어떤 역할을 했는지 묻는 질문'.
      critical_moment와 함께 채울 수 있지만, 이 값은 질문 내용이고 critical_moment는 그 질문에 답하는 순간입니다.
      사용자가 예상 질문을 말하지 않았다면 상황에 맞춰 임의로 만들지 마세요.
      """,
      "받을 것 같거나 특히 걱정되는 질문이 있나요?",
      null,
      null,
      List.of()),
  RESPONSE_STYLE(
      "response_style",
      "원하는 말하기 방식",
      SlotType.SINGLE_SELECT,
      """
      사용자가 이 상황에서 사용하고 싶다고 명확히 말한 전달 방식을 하나의 optionKey로 정규화하세요.
      concise_direct=짧고 핵심부터, relaxed_conversational=부담 없는 대화체,
      structured_evidence=순서·근거·사례를 갖춘 답변, listen_and_respond=상대 말을 충분히 듣고 반응,
      question_and_expand=질문을 주고받으며 확장, empathetic_responsive=공감 표현을 먼저 하며 반응,
      assertive_clear=분명하고 주도적으로 표현, humble_honest=모르는 점을 인정하며 솔직하게 표현에 대응합니다.
      상대에게 남기고 싶은 인상은 desired_persona이고, 실제 발화 구성 방식만 이 slot에 넣으세요.
      여러 방식을 말하면 사용자가 가장 강조한 하나를 선택하고, 말하기 방식이 명확하지 않으면 null입니다.
      상황 타입만 보고 면접에는 structured_evidence 같은 값을 자동 지정하지 마세요.
      """,
      "어떤 방식으로 말하거나 답하고 싶나요?",
      null,
      null,
      List.of(
          ContextSlotOptionType.CONCISE_DIRECT,
          ContextSlotOptionType.RELAXED_CONVERSATIONAL,
          ContextSlotOptionType.STRUCTURED_EVIDENCE,
          ContextSlotOptionType.LISTEN_AND_RESPOND,
          ContextSlotOptionType.QUESTION_AND_EXPAND,
          ContextSlotOptionType.EMPATHETIC_RESPONSIVE,
          ContextSlotOptionType.ASSERTIVE_CLEAR,
          ContextSlotOptionType.HUMBLE_HONEST)),
  INTERACTION_CONSTRAINT(
      "interaction_constraint",
      "피하거나 조심할 점",
      SlotType.TEXT,
      """
      사용자가 피하고 싶거나 조심하고 싶다고 명확히 말한 주제, 행동, 표현 방식 또는 개인적 경계를 추출하세요.
      좋은 값: '연애 경험을 캐묻는 대화는 피하고 싶음', '모르는 기술을 아는 척하지 않기',
      '이전 직장과 비교하는 말을 길게 하지 않기', '답변을 너무 길게 하지 않기'.
      일반적인 예절, 법률, 시스템 안전 규칙을 자동으로 넣지 말고 사용자가 직접 표현한 제약만 담으세요.
      conversation_material과 충돌하면 '피하고 싶다', '하지 않겠다' 같은 명시적인 부정 의도를 우선하세요.
      단순한 걱정은 critical_moment이고, 실제로 피하거나 제한하려는 의사가 있을 때만 이 slot을 채우세요.
      """,
      "피하고 싶거나 특히 조심하고 싶은 주제나 행동이 있나요?",
      null,
      null,
      List.of()),
  FAMILIARITY_LEVEL(
      "familiarity_level",
      "상황 경험 정도",
      SlotType.SINGLE_SELECT,
      """
      사용자가 이와 비슷한 상황을 얼마나 경험했는지 명확히 말한 경우 하나의 optionKey로 정규화하세요.
      first_time=처음이라고 명시, limited_experience=거의 없거나 한 번 정도, some_experience=몇 번 또는 가끔 경험,
      very_familiar=자주 경험했거나 익숙하다고 명시한 경우입니다.
      현재 긴장하거나 어려워한다는 사실만으로 경험이 적다고 추측하지 마세요.
      경력 연수, 나이, 직무, 관계 상태만으로 경험 수준을 판단하지 마세요.
      명확한 빈도나 자기평가가 없으면 null을 반환하세요.
      """,
      "비슷한 상황을 얼마나 경험해봤나요?",
      null,
      null,
      List.of(
          ContextSlotOptionType.FIRST_TIME,
          ContextSlotOptionType.LIMITED_EXPERIENCE,
          ContextSlotOptionType.SOME_EXPERIENCE,
          ContextSlotOptionType.VERY_FAMILIAR)),
  OUTFIT_DIRECTION(
      "outfit_direction",
      "의상 방향성",
      SlotType.SINGLE_SELECT,
      """
      사용자가 원하는 의상 분위기를 하나의 optionKey로 정규화하세요.
      neat_casual=단정하지만 과하게 격식 차리지 않은 캐주얼,
      formal_clean=정장·포멀함·격식을 갖춘 깔끔한 옷,
      soft_friendly=부드럽고 편안하며 친근한 인상의 옷에 대응합니다.
      사용자가 의상이나 스타일을 명확히 말하지 않았다면 null입니다.
      situation type만 보고 적절한 의상을 추측하거나 desired_persona를 의상 방향으로 복사하지 마세요.
      실제 outfit 후보 설정과 연결된 안정적인 optionKey만 반환하세요.
      """,
      "어떤 분위기의 옷차림을 원하나요?",
      null,
      ContextSlotOptionType.NEAT_CASUAL,
      List.of(
          ContextSlotOptionType.NEAT_CASUAL,
          ContextSlotOptionType.FORMAL_CLEAN,
          ContextSlotOptionType.SOFT_FRIENDLY));

  private final String key;
  private final String label;
  private final SlotType slotType;
  private final String extractionHint;
  private final String followUpHint;
  private final String defaultLiteralValue;
  private final ContextSlotOptionType defaultOption;
  private final List<ContextSlotOptionType> options;
}
