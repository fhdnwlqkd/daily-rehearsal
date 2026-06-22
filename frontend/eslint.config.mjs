import js from "@eslint/js";
import tseslint from "typescript-eslint";
import reactHooks from "eslint-plugin-react-hooks";
import globals from "globals";

export default tseslint.config(
  {
    ignores: [
      ".next/**",
      "node_modules/**",
      "out/**",
      "dist/**",
      "next-env.d.ts",
      "tsconfig.tsbuildinfo",
    ],
  },
  js.configs.recommended,
  // 타입 인식 strict 룰셋 (no-floating-promises, no-unsafe-*, no-explicit-any 등)
  ...tseslint.configs.strictTypeChecked,
  {
    files: ["**/*.{ts,tsx}"],
    plugins: { "react-hooks": reactHooks },
    languageOptions: {
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
      globals: { ...globals.browser, ...globals.node },
    },
    rules: {
      // TS가 담당하므로 core 룰은 끔
      "no-undef": "off",
      "no-unused-vars": "off",
      // 훅 규칙: 조건부 훅 호출 차단(에러) + deps 누락(경고)
      "react-hooks/rules-of-hooks": "error",
      "react-hooks/exhaustive-deps": "warn",
      "@typescript-eslint/restrict-template-expressions": [
        "error",
        { allowNumber: true, allowBoolean: true },
      ],
      // 이벤트 핸들러의 () => setX() 같은 void 화살표 단축형은 허용
      "@typescript-eslint/no-confusing-void-expression": [
        "error",
        { ignoreArrowShorthand: true },
      ],
      // tsconfig에서 옮겨온 미사용 변수 검사 (_prefix는 의도적 무시)
      "@typescript-eslint/no-unused-vars": [
        "warn",
        {
          argsIgnorePattern: "^_",
          varsIgnorePattern: "^_",
          caughtErrorsIgnorePattern: "^_",
        },
      ],
    },
  },
  // shadcn 생성 보일러플레이트: 타입 인식 strict 룰 완화 (직접 작성/유지 대상 아님)
  {
    files: ["components/ui/**"],
    extends: [tseslint.configs.disableTypeChecked],
  },
  // 설정 파일 등 순수 JS: 타입 인식 룰 비활성화
  {
    files: ["**/*.{js,mjs,cjs}"],
    extends: [tseslint.configs.disableTypeChecked],
  },
);
