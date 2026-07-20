# HTTP Validation

## Local environment

Copy `http-client.env.example.json` to `http-client.private.env.json` in the `rehearsal-modules` directory.
Select either `local-fake` or `manual-gemini` as the IntelliJ HTTP Client environment before running a request.

Do not commit `http-client.private.env.json`. It may contain a production host or an `X-API-KEY` value.

## Deterministic local validation

Start the API with the `local` profile and fake AI provider. Run
`v1/01-situation-selection-and-context-collection.http` from top to bottom.

The `local-fake` transcript intentionally omits `desired_persona`. The expected flow is:

1. briefing submission returns `EXTRACTING`;
2. context polling reaches `FOLLOW_UP_REQUIRED`;
3. follow-up submission returns `MERGING`;
4. context polling reaches `COMPLETED`.

## Manual Gemini validation

Start the API process with `GEMINI_API_KEY` and `REHEARSAL_AI_DEFAULT_PROVIDER=gemini` in its environment. Use the `manual-gemini` environment to submit a natural transcript.

Gemini output is non-deterministic. Verify that polling eventually reaches either `COMPLETED` or `FOLLOW_UP_REQUIRED`, and inspect the returned context quality. Do not assert exact generated values in automated tests.

Submit each action POST once. Re-run only its polling GET request until it reaches a terminal state. Polling reads RDB rows and must not trigger another AI call.
