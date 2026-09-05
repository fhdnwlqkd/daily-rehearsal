import { apiFetch } from "@/lib/api";

interface DemoDecartTokenResponse {
  clientToken: string;
}

export function issueDemoDecartToken() {
  return apiFetch<DemoDecartTokenResponse>("/api/v1/demo/decart-token", {
    method: "POST",
  });
}
