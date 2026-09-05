import { cookies } from "next/headers";
import { DemoRehearsal } from "@/features/demo-rehearsal";
import { DemoPasswordGate } from "@/features/demo-rehearsal/components/demo-password-gate";
import {
  DEMO_AUTH_COOKIE,
  isDemoAuthConfigured,
  verifyDemoSessionToken,
} from "@/features/demo-rehearsal/server/demo-auth";

export default async function DemoPage() {
  const cookieStore = await cookies();
  const authenticated = verifyDemoSessionToken(
    cookieStore.get(DEMO_AUTH_COOKIE)?.value,
  );

  return authenticated ? (
    <DemoRehearsal />
  ) : (
    <DemoPasswordGate configured={isDemoAuthConfigured()} />
  );
}
