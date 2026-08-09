/**
 * 브라우저는 백엔드를 직접 호출하지 않고 같은 출처의 프록시
 * (app/api/backend/[...path]/route.ts)를 거친다. 실서버 주소와 인증 키는
 * 프록시(서버)만 알고, 클라이언트 번들에는 어떤 시크릿도 들어가지 않는다.
 */
const PROXY_PREFIX = "/api/backend";

/**
 * 백엔드의 공통 응답 규약(ApiResponse.java와 1:1 대응).
 * 백엔드가 @JsonInclude(NON_NULL)이라 null 필드는 키 자체가 생략된다 —
 * 그래서 null 유니언이 아니라 optional(?)로 선언한다.
 * apiFetch가 벗겨서 data만 반환하므로 바깥에서 이 타입을 다룰 일은 없다.
 */
type ApiResponse<T> = {
  success: boolean;
  data?: T;
  error?: {
    code: string;
    name: string;
    message: string;
    /** 필드 검증 에러(C001) 시에만 존재. */
    details?: unknown[];
  };
};

/**
 * 실패 3종(네트워크 단절 / HTTP 4xx·5xx / success: false)을
 * 하나로 정규화한 에러. 호출부는 catch 한 번으로 처리하고,
 * 필요하면 code로 분기한다(예: SESSION_NOT_FOUND → 첫 화면 복귀).
 */
export class ApiError extends Error {
  constructor(
    message: string,
    public readonly code: string,
    public readonly status: number,
    /** 필드 검증 에러(C001) 시 백엔드가 내려주는 상세 목록. */
    public readonly details?: unknown[],
  ) {
    super(message);
    this.name = "ApiError";
  }
}

/**
 * 백엔드 JSON API 전용 fetch 래퍼.
 * baseURL 결합 + JSON 헤더 + ApiResponse 껍데기 벗기기 + 에러 정규화를 담당한다.
 *
 * JSON 요청 전용이다 — SSE 스트리밍과 multipart 업로드는
 * 응답/헤더 처리가 달라서 이 래퍼를 거치지 않고 fetch를 직접 쓴다.
 */
export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  // HeadersInit은 배열·Headers 객체일 수도 있어서 spread 대신 Headers로 병합한다.
  const headers = new Headers(options.headers);
  if (!headers.has("Content-Type") && !(options.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }
  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json");
  }

  const response = await fetch(`${PROXY_PREFIX}${path}`, {
    ...options,
    headers,
    // 세션 상태가 계속 변하는 앱이라 응답 캐싱은 하지 않는다.
    cache: "no-store",
  });

  if (!response.ok) {
    const body = (await response
      .json()
      .catch(() => null)) as ApiResponse<unknown> | null;
    throw new ApiError(
      body?.error?.message ?? `Request failed with ${response.status}`,
      body?.error?.code ?? "HTTP_ERROR",
      response.status,
      body?.error?.details,
    );
  }

  const body = (await response.json()) as ApiResponse<T>;
  if (!body.success) {
    throw new ApiError(
      body.error?.message ?? "Request failed",
      body.error?.code ?? "UNKNOWN",
      response.status,
      body.error?.details,
    );
  }

  // success인데 data가 없는 응답(ApiResponse.empty())도 정상이다.
  // 그런 엔드포인트는 apiFetch<void>로 호출한다.
  return body.data as T;
}
