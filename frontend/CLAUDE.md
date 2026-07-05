# Frontend (Next.js 16 · React 19 · TS · Tailwind v4 · pnpm)

> 포맷/타입/린트는 strict tsconfig · eslint · prettier · pre-commit 게이트가 자동으로
> 강제한다. 이 문서엔 **자동으로 잡히지 않는 "구조 규칙"만** 적는다.

## 폴더 구조: 기능(feature) 기반

코드를 "종류"(전부 components, 전부 hooks)가 아니라 **"어느 기능에 속하느냐"**로 모은다.

- **여러 기능이 함께 쓰는 것** → `frontend/` 바로 아래(전역)
  - `components/ui/` — shadcn 기본 컴포넌트 (직접 수정하지 말 것)
  - `components/common/` — 우리가 만든 공용 컴포넌트
  - `hooks/` · `lib/` · `types/` · `constants/`
- **한 기능에서만 쓰는 것** → `features/<기능>/` 안에 모은다
  ```
  features/smart-mirror/
  ├── index.ts          # 이 기능의 "정문". 바깥은 여기로만 import 한다
  ├── smart-mirror.tsx  # 기능의 진입 컴포넌트
  ├── types.ts          # 이 기능의 타입 (늘어나면 types/ 폴더로)
  ├── data/             # mock / 실데이터
  ├── hooks/            # 이 기능 전용 훅 (예: use-camera)
  └── components/       # 이 기능의 컴포넌트 (내부 구성은 기능에 맞게)
      └── shared/       # 그 안에서 여러 곳이 함께 쓰는 부품
  ```
  > `index.ts` · `types.ts` · `data/` · `hooks/` · `components/`까지가 공통 뼈대다.
  > 그 아래 세부 폴더는 기능이 필요로 할 때 만든다 — 예를 들어 smart-mirror는
  > 화면이 단계별로 나뉘어서 `components/stages/`를 둔다(모든 기능에 필요한 건 아님).

## 규칙

**1. `app/` 폴더는 라우팅(주소)만 담당한다.**
`page.tsx` / `layout.tsx`는 기능 컴포넌트를 불러와 화면에 띄우기만 하고, 실제 로직은 `features/`에 둔다.
→ 주소 구조와 기능 코드가 섞이지 않아서 어디에 뭐가 있는지 찾기 쉽다.

**2. 기능 바깥에서는 그 기능의 `index.ts`로만 가져온다.**

```ts
import { SmartMirror } from "@/features/smart-mirror"; // ✅ 정문으로
import { SttPanel } from "@/features/smart-mirror/components/shared/stt-panel"; // ❌ 내부 직접
```

→ 기능의 "공개 API"가 `index.ts` 하나로 정해져서, 내부 구조를 마음껏 바꿔도 바깥이 안 깨진다.

**3. 새 코드는 일단 기능 폴더 안에 둔다. 두 번째 기능이 필요로 할 때 전역으로 옮긴다.**
"언젠가 공용으로 쓰겠지" 하고 미리 전역에 두지 않는다.
→ 실제로 한 번도 재사용 안 되는 "가짜 공용 코드"가 안 생긴다. (성급한 추상화 방지)

**4. `shared/`엔 "여러 곳이 함께 쓰는 부품"만. 한 곳에서만 쓰는 헬퍼는 그 파일 안에 둔다.**

```tsx
// change-card-stage.tsx — MockQr은 이 컴포넌트에서만 쓰니까 같은 파일 안에 숨긴다
export function ChangeCardStage() { return ( ... <MockQr /> ... ); }
function MockQr() { ... }   // export 안 함 = 이 파일 전용
```

→ `shared/`엔 진짜 공용만 남아서, "이거 공용이야?"를 폴더만 봐도 안다. 파일 수도 안 불어난다.

## API 호출 계층

컴포넌트 → 훅 → API 함수 → 공용 래퍼 순서로 내려간다. 층을 건너뛰지 않는다.

- **`lib/api.ts`** — 공용 fetch 래퍼(`apiFetch`). baseURL 결합, JSON 헤더,
  백엔드 공통 응답(`{ success, data, error }`) 벗기기, 에러 정규화(`ApiError`)를 담당한다.
  **JSON 요청 전용** — SSE 스트리밍·multipart 업로드는 래퍼를 거치지 않고
  기능의 `apis.ts` 안에서 fetch를 직접 쓴다.
- **`features/<기능>/apis.ts`** — 실제 API 호출 함수 (예: `getSituationTypes`).
  `apiFetch`를 사용하고, 경로·메서드·body·응답 타입만 선언한다.
- **`features/<기능>/types.ts`** — API 응답 타입 (예: `GetSituationTypesResponse`).
  백엔드 미구현 상태에서는 프론트가 먼저 정의하는 명세 역할을 한다.
- **`features/<기능>/data/mock-*.ts`** — 응답 타입을 그대로 만족하는 mock.
  타입 검사를 받으므로 명세와 mock이 어긋나면 컴파일이 잡는다.
- **`features/<기능>/hooks/use-get-*.ts`** — 컴포넌트가 쓰는 훅이자 **mock/실API 전환점**.
  mock 단계에서는 `data/`의 mock을 비동기로 돌려주고, API가 완성되면
  훅 안의 호출만 `apis.ts` 함수로 바꾼다. 컴포넌트는 수정하지 않는다.
  상태는 `ApiStatus`(`"LOADING" | "READY" | "ERROR"` — 대문자) 하나로 통일한다.

## 명령

- 개발 `pnpm dev` / 타입검사 `pnpm typecheck` / 린트 `pnpm lint` / 포맷 `pnpm format`
- 커밋하면 pre-commit 게이트(lint-staged + tsc)가 자동 실행되고, 통과 못 하면 커밋이 막힌다.
