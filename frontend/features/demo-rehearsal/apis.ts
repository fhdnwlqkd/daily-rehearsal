interface DemoDecartTokenResponse {
  clientToken: string;
}

interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: { message?: string };
}

export async function issueDemoDecartToken() {
  const response = await fetch("/api/demo/decart-token", {
    method: "POST",
    cache: "no-store",
  });
  const body = (await response.json().catch(() => null)) as
    | ApiResponse<DemoDecartTokenResponse>
    | { message?: string }
    | null;

  if (
    !response.ok ||
    !body ||
    !("success" in body) ||
    !body.success ||
    !body.data
  ) {
    const message =
      body && "message" in body
        ? body.message
        : body && "error" in body
          ? body.error?.message
          : undefined;
    throw new Error(message ?? "Demo Decart token request failed");
  }

  return body.data;
}
