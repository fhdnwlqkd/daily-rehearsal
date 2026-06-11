INSERT INTO context_slot_schema (
    schema_key,
    name,
    max_follow_up_attempt,
    active,
    created_at,
    updated_at
)
SELECT
    'p1_offline_default',
    'P1 Offline Default Context Slot Schema',
    1,
    TRUE,
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (
    SELECT 1
    FROM context_slot_schema
    WHERE schema_key = 'p1_offline_default'
);

INSERT INTO context_slot (
    slot_key,
    label,
    slot_type,
    extraction_hint,
    follow_up_hint,
    default_literal_value,
    created_at,
    updated_at
)
SELECT
    'situation_type',
    '상황 유형',
    'SINGLE_SELECT',
    '사용자의 내일 일정이나 이벤트를 발표, 소개팅/첫 만남, 면접, 중요한 대화, 모임/네트워킹, 일상 정돈 중 하나로 분류한다. 직접 판단할 수 없으면 null로 둔다.',
    '내일 어떤 상황을 미리 겪어보고 싶은지 짧게 알려주세요.',
    NULL,
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM context_slot WHERE slot_key = 'situation_type');

INSERT INTO context_slot (
    slot_key,
    label,
    slot_type,
    extraction_hint,
    follow_up_hint,
    default_literal_value,
    created_at,
    updated_at
)
SELECT
    'desired_persona',
    '되고 싶은 모습',
    'SINGLE_SELECT',
    '내일 사용자가 보여주고 싶은 태도나 인상을 추출한다. 예: 차분함, 신뢰감, 자연스러움, 준비된 인상, 먼저 다가가는 태도.',
    '내일 어떤 모습으로 보이고 싶은지 한 문장으로 알려주세요.',
    NULL,
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM context_slot WHERE slot_key = 'desired_persona');

INSERT INTO context_slot (
    slot_key,
    label,
    slot_type,
    extraction_hint,
    follow_up_hint,
    default_literal_value,
    created_at,
    updated_at
)
SELECT
    'critical_moment',
    '결정적 순간',
    'TEXT',
    '내일 사용자가 가장 흔들릴 수 있거나 리허설하고 싶은 한 순간을 추출한다. 예: 첫 인사, 예상 질문, 어색한 침묵, 자기소개 직후.',
    '가장 걱정되거나 미리 연습하고 싶은 순간은 언제인가요?',
    '첫 반응을 말해야 하는 순간',
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM context_slot WHERE slot_key = 'critical_moment');

INSERT INTO context_slot (
    slot_key,
    label,
    slot_type,
    extraction_hint,
    follow_up_hint,
    default_literal_value,
    created_at,
    updated_at
)
SELECT
    'anxiety_point',
    '걱정 포인트',
    'TEXT',
    '사용자가 내일 상황에서 걱정하거나 불편해하는 지점을 추출한다. 감정, 실수 우려, 어색함, 질문 부담, 지각 우려 등을 포함한다.',
    '내일 가장 신경 쓰이는 점이 있다면 짧게 말해주세요.',
    '처음 시작이 어색할 수 있음',
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM context_slot WHERE slot_key = 'anxiety_point');

INSERT INTO context_slot (
    slot_key,
    label,
    slot_type,
    extraction_hint,
    follow_up_hint,
    default_literal_value,
    created_at,
    updated_at
)
SELECT
    'place_context',
    '장소 맥락',
    'TEXT',
    '사용자가 내일 가게 될 장소, 공간 분위기, 지역, 회의실/식당/교실/사무실 같은 물리적 맥락을 추출한다.',
    '어디에서 일어나는 상황인지 알려주세요.',
    '조용하지만 약간 낯선 실내 공간',
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM context_slot WHERE slot_key = 'place_context');

INSERT INTO context_slot (
    slot_key,
    label,
    slot_type,
    extraction_hint,
    follow_up_hint,
    default_literal_value,
    created_at,
    updated_at
)
SELECT
    'opponent_context',
    '상대/청중 분위기',
    'TEXT',
    '사용자가 만나거나 마주할 상대, 청중, 면접관, 팀원의 분위기를 추출한다. 알 수 없으면 null로 둔다.',
    '상대나 청중은 어떤 분위기일 것 같나요?',
    '상대는 아직 분위기를 살피는 중',
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM context_slot WHERE slot_key = 'opponent_context');

INSERT INTO context_slot (
    slot_key,
    label,
    slot_type,
    extraction_hint,
    follow_up_hint,
    default_literal_value,
    created_at,
    updated_at
)
SELECT
    'outfit_direction',
    '복장 방향',
    'SINGLE_SELECT',
    '사용자가 원하는 복장이나 스타일 방향을 추출한다. 과하지 않은 단정함, 캐주얼, 비즈니스, 격식 있음, 부드러운 뉴트럴 등을 구분한다.',
    '내일 어떤 옷차림이나 분위기로 보이고 싶나요?',
    NULL,
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM context_slot WHERE slot_key = 'outfit_direction');

INSERT INTO context_slot (
    slot_key,
    label,
    slot_type,
    extraction_hint,
    follow_up_hint,
    default_literal_value,
    created_at,
    updated_at
)
SELECT
    'route_risk',
    '이동 변수',
    'TEXT',
    '지각 우려, 날씨, 이동 시간 압박, 낯선 장소, 주차/환승 같은 내일의 이동 리스크를 추출한다.',
    '이동이나 도착 시간에서 걱정되는 점이 있나요?',
    '시간 여유가 줄어들 수 있음',
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM context_slot WHERE slot_key = 'route_risk');

INSERT INTO context_slot (
    slot_key,
    label,
    slot_type,
    extraction_hint,
    follow_up_hint,
    default_literal_value,
    created_at,
    updated_at
)
SELECT
    'change_action',
    '바꿀 행동',
    'TEXT',
    '내일 사용자가 실제로 바꾸고 싶은 행동이나 조정할 습관을 추출한다. 예: 일찍 출발하기, 첫 문장 짧게 말하기, 먼저 웃기.',
    '내일 하나만 바꾼다면 어떤 행동을 바꾸고 싶나요?',
    '10분 일찍 준비하고 첫 문장을 짧게 시작하기',
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM context_slot WHERE slot_key = 'change_action');

INSERT INTO context_slot_option (context_slot_id, option_key, label, created_at, updated_at)
SELECT slot.id, option_values.option_key, option_values.label, NOW(6), NOW(6)
FROM context_slot slot
JOIN (
    SELECT 'presentation' AS option_key, '발표' AS label
    UNION ALL SELECT 'date', '소개팅/첫 만남'
    UNION ALL SELECT 'interview', '면접'
    UNION ALL SELECT 'important_conversation', '중요한 대화'
    UNION ALL SELECT 'social_event', '모임/네트워킹'
    UNION ALL SELECT 'daily_reset', '일상 정돈'
) option_values
WHERE slot.slot_key = 'situation_type'
  AND NOT EXISTS (
      SELECT 1
      FROM context_slot_option existing_option
      WHERE existing_option.context_slot_id = slot.id
        AND existing_option.option_key = option_values.option_key
  );

INSERT INTO context_slot_option (context_slot_id, option_key, label, created_at, updated_at)
SELECT slot.id, option_values.option_key, option_values.label, NOW(6), NOW(6)
FROM context_slot slot
JOIN (
    SELECT 'calm_confident' AS option_key, '차분하고 신뢰감 있는 모습' AS label
    UNION ALL SELECT 'warm_natural', '따뜻하고 자연스러운 모습'
    UNION ALL SELECT 'sharp_prepared', '준비된 인상'
    UNION ALL SELECT 'open_friendly', '먼저 다가가는 모습'
    UNION ALL SELECT 'grounded', '흔들리지 않는 모습'
) option_values
WHERE slot.slot_key = 'desired_persona'
  AND NOT EXISTS (
      SELECT 1
      FROM context_slot_option existing_option
      WHERE existing_option.context_slot_id = slot.id
        AND existing_option.option_key = option_values.option_key
  );

INSERT INTO context_slot_option (context_slot_id, option_key, label, created_at, updated_at)
SELECT slot.id, option_values.option_key, option_values.label, NOW(6), NOW(6)
FROM context_slot slot
JOIN (
    SELECT 'casual' AS option_key, '캐주얼' AS label
    UNION ALL SELECT 'neat_casual', '과하지 않은 단정함'
    UNION ALL SELECT 'smart_casual', '스마트 캐주얼'
    UNION ALL SELECT 'business', '비즈니스'
    UNION ALL SELECT 'formal', '격식 있음'
    UNION ALL SELECT 'soft_neutral', '부드러운 뉴트럴'
) option_values
WHERE slot.slot_key = 'outfit_direction'
  AND NOT EXISTS (
      SELECT 1
      FROM context_slot_option existing_option
      WHERE existing_option.context_slot_id = slot.id
        AND existing_option.option_key = option_values.option_key
  );

UPDATE context_slot slot
JOIN context_slot_option option_value
    ON option_value.context_slot_id = slot.id
   AND option_value.option_key = 'daily_reset'
SET slot.default_context_slot_option_id = option_value.id,
    slot.updated_at = NOW(6)
WHERE slot.slot_key = 'situation_type'
  AND slot.default_context_slot_option_id IS NULL;

UPDATE context_slot slot
JOIN context_slot_option option_value
    ON option_value.context_slot_id = slot.id
   AND option_value.option_key = 'calm_confident'
SET slot.default_context_slot_option_id = option_value.id,
    slot.updated_at = NOW(6)
WHERE slot.slot_key = 'desired_persona'
  AND slot.default_context_slot_option_id IS NULL;

UPDATE context_slot slot
JOIN context_slot_option option_value
    ON option_value.context_slot_id = slot.id
   AND option_value.option_key = 'smart_casual'
SET slot.default_context_slot_option_id = option_value.id,
    slot.updated_at = NOW(6)
WHERE slot.slot_key = 'outfit_direction'
  AND slot.default_context_slot_option_id IS NULL;

INSERT INTO context_slot_schema_item (
    context_slot_schema_id,
    context_slot_id,
    required_level,
    priority,
    active,
    created_at,
    updated_at
)
SELECT
    schema_value.id,
    slot.id,
    item_values.required_level,
    item_values.priority,
    TRUE,
    NOW(6),
    NOW(6)
FROM context_slot_schema schema_value
JOIN (
    SELECT 'situation_type' AS slot_key, 'REQUIRED' AS required_level, 10 AS priority
    UNION ALL SELECT 'desired_persona', 'REQUIRED', 20
    UNION ALL SELECT 'critical_moment', 'REQUIRED', 30
    UNION ALL SELECT 'anxiety_point', 'SOFT_REQUIRED', 40
    UNION ALL SELECT 'place_context', 'OPTIONAL', 50
    UNION ALL SELECT 'opponent_context', 'OPTIONAL', 60
    UNION ALL SELECT 'outfit_direction', 'OPTIONAL', 70
    UNION ALL SELECT 'route_risk', 'OPTIONAL', 80
    UNION ALL SELECT 'change_action', 'OPTIONAL', 90
) item_values
JOIN context_slot slot
    ON slot.slot_key = item_values.slot_key
WHERE schema_value.schema_key = 'p1_offline_default'
  AND NOT EXISTS (
      SELECT 1
      FROM context_slot_schema_item existing_item
      WHERE existing_item.context_slot_schema_id = schema_value.id
        AND existing_item.context_slot_id = slot.id
  );
