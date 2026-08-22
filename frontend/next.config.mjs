/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "standalone",
  typescript: {
    ignoreBuildErrors: true,
  },
  images: {
    unoptimized: true,
  },
  async headers() {
    return [
      {
        source: "/:path*",
        headers: [
          {
            // 외부 전시/운영 페이지가 Daily Rehearsal을 iframe으로 호출할 수 있다.
            key: "Content-Security-Policy",
            value: "frame-ancestors *",
          },
          {
            // iframe 내부의 카메라·마이크 체험과 모바일 저장 공유를 허용한다.
            // 실제 권한 사용에는 부모 iframe의 allow 속성도 함께 필요하다.
            key: "Permissions-Policy",
            value:
              "camera=*, microphone=*, autoplay=*, fullscreen=*, web-share=*, display-capture=*",
          },
        ],
      },
    ];
  },
};

export default nextConfig;
