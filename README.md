# 🪞 STC (StyleCaster): Daily Risk Simulator

> **"완벽한 하루를 입다. AI 기반 상황 인식형 인터랙티브 스마트 거울"**



## 📌 프로젝트 소개 (Project Overview)
**STC (StyleCaster)**는 다가올 불확실한 일상(날씨, 복잡한 일정 등)을 선제적으로 예측하고 대비할 수 있도록 돕는 **라이프케어 스마트 거울**입니다. 

단순히 옷을 입혀보는 가상 피팅(Virtual Fitting)을 넘어, 사용자가 거울 앞에 서는 순간 1) 체형 불균형을 분석하여 교정하고, 2) 오늘의 일정과 날씨를 음성으로 파악한 뒤, 3) 생성형 AI를 통해 가장 완벽한 핏과 스타일링을 제안하여 시각적인 자신감을 부여합니다.

## ✨ 핵심 기능 (Key Features)

*   **🧘‍♂️ 실시간 자세 분석 및 피드백 (Wellness Focus)**
    *   웹캠과 비전 인식 기술을 활용해 사용자의 관절 위치를 추적합니다.
    *   어깨 비대칭, 거북목 등 불균형한 자세를 감지하고 UI를 통해 실시간 교정 피드백을 제공합니다.
*   **🎙️ 음성 기반 맥락 인식 (Context-Awareness)**
    *   사용자의 음성을 통해 오늘의 주요 일정과 TPO(시간, 장소, 상황)를 파악합니다.
    *   외부 API를 연동하여 기상 데이터를 실시간으로 동기화합니다.
*   **👔 AI 가상 피팅 시뮬레이션 (VTON & Styling)**
    *   수집된 맥락(날씨, 일정)을 바탕으로 최적의 스타일링 프롬프트를 생성합니다.
    *   사용자의 이미지에 생성된 스타일을 합성하여 거울 화면에 실시간으로 오버레이 합니다.
*   **🖐️ 제스처 인터랙션 (Touchless Control)**
    *   화면을 직접 터치할 필요 없이, 허공에서의 스와이프 제스처를 인식하여 결과를 제어합니다.

## 🛠️ 기술 스택 (Tech Stack)

### Frontend (User Interaction & Vision)
*   **Framework/Library:** React.js, Tailwind CSS
*   **Animation:** Framer Motion (부드러운 상태 전환 및 UI 렌더링)
*   **Vision/AI:** MediaPipe (실시간 Pose 및 Hand Tracking)

### Backend (Orchestration & Data Pipeline)
*   **Framework:** Java / Spring Boot 
*   **Data & Concurrency:** Redis (세션 캐싱 및 멱등성 보장)
*   **AI Integration:** OpenAI API (음성/텍스트 분석), VTON API (가상 피팅 이미지 생성)

## 🏗️ 시스템 아키텍처 (Architecture)

1.  **Standby State:** MediaPipe가 클라이언트 브라우저에서 사용자의 자세를 인식하고 캡처합니다. (서버 부하 최소화)
2.  **Listening State:** 사용자의 음성 데이터와 스냅샷 이미지가 Spring Boot 서버로 전송됩니다.
3.  **Loading State:** 백엔드 서버가 외부 AI API를 오케스트레이션하여 맥락을 분석하고 합성 이미지를 생성합니다. 동시에 Redis를 활용해 요청의 중복을 막고 진행 상태를 관리합니다.
4.  **Result State:** 완성된 렌더링 이미지와 안내 텍스트가 프론트엔드로 응답되어 화면에 출력됩니다.

## 👥 팀원 및 역할 (Team Roles)
*   **PM & AI Prompting:** 기획 총괄, 외부 AI 모델(VTON) 파이프라인 구축 및 프롬프트 엔지니어링
*   **Frontend Developer:** 웹캠/마이크 연동, MediaPipe 비전 처리, 4단계 UI/UX 상태 관리 및 애니메이션 구현
*   **Backend Developer:** Spring Boot 서버 구축, AI API 게이트웨이 역할 수행, 트래픽 제어 및 비동기 파이프라인 설계

## 🚀 시작하기 (Getting Started)
*(추후 로컬 환경 실행 방법 및 환경 변수 설정 방법 작성 예정)*