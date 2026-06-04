"use client"

import { motion } from "framer-motion"
import type { ReactNode } from "react"
import {
  ArrowLeft,
  ArrowRight,
  Check,
  CloudRain,
  MapPin,
  Mic,
  QrCode,
  Sparkles,
  UserRound,
  Volume2,
} from "lucide-react"
import { AudioWave } from "./audio-wave"
import { GlassPanel } from "./glass-panel"
import { ScanningEffect } from "./scanning-effect"

type ExperiencePhaseId =
  | "briefing"
  | "context"
  | "transformation"
  | "gesture-fit"
  | "rehearsal"
  | "change-card"

interface ExperiencePhase {
  id: ExperiencePhaseId
  label: string
  timeRange: string
}

export const experiencePhases = [
  { id: "briefing", label: "거울 속의 나", timeRange: "0-15s" },
  { id: "context", label: "데이터 스캐닝", timeRange: "15-25s" },
  { id: "transformation", label: "시공간의 전환", timeRange: "25-35s" },
  { id: "gesture-fit", label: "제스처 피팅", timeRange: "35-45s" },
  { id: "rehearsal", label: "결정적 순간", timeRange: "45-55s" },
  { id: "change-card", label: "변화 카드", timeRange: "55-60s" },
] as const satisfies readonly ExperiencePhase[]

const mockExperience = {
  transcript:
    "내일 소개팅이 있는데 좀 어색할 것 같고, 너무 꾸민 느낌은 싫어요. 성수 쪽 음식점에서 만나고 늦으면 안 될 것 같아요.",
  contextReply:
    "자연스럽고 자신감 있어 보이고 싶어요. 처음 만났을 때 어색한 침묵이 생기는 게 제일 걱정돼요. 상대는 차분하지만 낯가림이 있을 것 같아요.",
  followUp:
    "내일의 장면이 거의 완성됐어요. 어떤 모습으로 보이고 싶은지, 가장 걱정되는 순간은 언제인지, 상대는 어떤 분위기일 것 같은지 알려주세요.",
  tags: ["소개팅", "어색함", "과하지 않은 단정함", "성수 음식점", "지각 우려"],
  missing: ["페르소나", "걱정 순간", "상대 분위기"],
  followUpQuestions: ["어떤 모습으로 보이고 싶나요?", "가장 걱정되는 순간은 언제인가요?", "상대는 어떤 분위기일 것 같나요?"],
  outfits: [
    { name: "단정한 캐주얼", tone: "과하지 않은 첫인상", active: false },
    { name: "부드러운 뉴트럴", tone: "편안한 대화 분위기", active: true },
    { name: "차분한 데이트룩", tone: "조용하지만 선명한 인상", active: false },
  ],
  persona: "자신감 있는 첫인상",
  style: "단정한 캐주얼",
  scene: "성수 음식점에서 첫 만남",
  routeRisk: "비 옴 +12분",
  placeMood: "공간 소음도 중간",
  gestureHint: "오른손을 넘기면 다음 스타일, 손바닥을 멈추면 선택",
  aiPrompt: "처음 뵙네요. 오는 길 괜찮으셨어요?",
  userReply: "네, 조금 일찍 나와서 여유 있게 도착했어요. 여기 분위기가 좋네요.",
  changeAction: "20분 일찍 출발하기",
  changeAttitude: "천천히 말하고 먼저 웃기",
  ifThen: "어색한 침묵이 생기면 장소에 대한 질문으로 다시 시작하기",
}

interface P1ExperienceStageProps {
  phase: ExperiencePhase
  phaseIndex: number
  totalPhases: number
}

export function P1ExperienceStage({ phase, phaseIndex, totalPhases }: P1ExperienceStageProps) {
  return (
    <div className="relative h-full w-full overflow-hidden text-white">
      <MirrorToneOverlay phase={phase.id} />
      <StageHeader phase={phase} phaseIndex={phaseIndex} totalPhases={totalPhases} />
      {phase.id !== "briefing" && <TransitionCue phase={phase.id} />}

      {phase.id === "briefing" && <BriefingStage />}
      {phase.id === "context" && <ContextStage />}
      {phase.id === "transformation" && <TransformationStage />}
      {phase.id === "gesture-fit" && <GestureFitStage />}
      {phase.id === "rehearsal" && <RehearsalStage />}
      {phase.id === "change-card" && <ChangeCardStage />}

      <TapHint isLast={phaseIndex === totalPhases - 1} />
    </div>
  )
}

function MirrorToneOverlay({ phase }: { phase: ExperiencePhaseId }) {
  const overlays: Record<ExperiencePhaseId, string> = {
    briefing: "from-black/35 via-transparent to-black/55",
    context: "from-black/55 via-black/10 to-black/75",
    transformation: "from-black/45 via-black/10 to-black/75",
    "gesture-fit": "from-black/45 via-black/10 to-black/75",
    rehearsal: "from-black/45 via-black/20 to-black/80",
    "change-card": "from-black/70 via-black/55 to-black/90",
  }

  return (
    <>
      <div className={`absolute inset-0 bg-gradient-to-b ${overlays[phase]}`} />
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,transparent_0%,rgba(0,0,0,0.18)_54%,rgba(0,0,0,0.7)_100%)]" />
    </>
  )
}

function StageHeader({
  phase,
  phaseIndex,
  totalPhases,
}: {
  phase: ExperiencePhase
  phaseIndex: number
  totalPhases: number
}) {
  return (
    <div className="absolute left-8 right-8 top-6 z-20 flex items-center justify-between">
      <div className="flex items-center gap-3 text-white/70">
        <span className="text-xs font-light tracking-[0.32em]">DAILY REHEARSAL</span>
        <span className="h-px w-10 bg-white/20" />
        <span className="text-xs font-light tracking-[0.18em]">{phase.timeRange}</span>
      </div>
      <div className="flex items-center gap-3">
        <span className="text-xs font-light tracking-[0.16em] text-white/60">{phase.label}</span>
        <div className="flex gap-1.5">
          {Array.from({ length: totalPhases }).map((_, index) => (
            <div
              key={index}
              className={`h-1.5 w-7 rounded-full ${index <= phaseIndex ? "bg-white/70" : "bg-white/15"}`}
            />
          ))}
        </div>
      </div>
    </div>
  )
}

function TransitionCue({ phase }: { phase: ExperiencePhaseId }) {
  const copy: Record<ExperiencePhaseId, string> = {
    briefing: "",
    context: "맥락을 정리합니다",
    transformation: "내일의 모습을 입혀봅니다",
    "gesture-fit": "제스처로 모습을 고릅니다",
    rehearsal: "결정적 순간을 들어봅니다",
    "change-card": "내일의 변화 카드를 발급합니다",
  }

  return (
    <motion.div
      className="pointer-events-none absolute inset-0 z-40 flex items-center justify-center bg-black/35 backdrop-blur-[2px]"
      initial={{ opacity: 0 }}
      animate={{ opacity: [0, 1, 1, 0] }}
      transition={{ duration: 2.4, times: [0, 0.16, 0.72, 1], ease: "easeInOut" }}
    >
      <motion.div
        className="border-y border-white/20 px-10 py-8 text-center"
        initial={{ y: 18, scale: 0.98 }}
        animate={{ y: [18, 0, 0, -10], scale: [0.98, 1, 1, 0.99] }}
        transition={{ duration: 2.4, times: [0, 0.16, 0.72, 1], ease: "easeInOut" }}
      >
        <p className="mb-4 text-xs font-light tracking-[0.34em] text-white/55">NEXT SEQUENCE</p>
        <p className="text-5xl font-extralight tracking-wide text-white md:text-7xl">{copy[phase]}</p>
      </motion.div>
    </motion.div>
  )
}

function BriefingStage() {
  return (
    <div className="relative flex h-full flex-col items-center justify-center px-8">
      <EdgeWave />
      <motion.div
        className="absolute top-[17%] flex flex-col items-center gap-4 text-center"
        initial={{ opacity: 0, y: -14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.7 }}
      >
        <div className="flex items-center gap-2 rounded-full border border-white/10 bg-black/20 px-4 py-2 text-white/70 backdrop-blur-xl">
          <Mic className="h-4 w-4" strokeWidth={1.5} />
          <span className="text-xs font-light tracking-[0.2em]">LISTENING</span>
        </div>
        <h1 className="text-4xl font-extralight tracking-wide md:text-6xl">내일 하루를 짧게 말해주세요</h1>
        <p className="max-w-2xl text-base font-light text-white/65 md:text-lg">
          일정, 걱정, 되고 싶은 모습까지 한 번에 말하면 거울이 내일의 장면을 구성합니다.
        </p>
      </motion.div>

      <motion.div
        className="absolute inset-x-0 bottom-12 flex justify-center px-6"
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2, duration: 0.7 }}
      >
        <SttPanel text={mockExperience.transcript} />
      </motion.div>
    </div>
  )
}

function SttPanel({
  text,
  label = "REAL-TIME STT",
  compact = false,
}: {
  text: string
  label?: string
  compact?: boolean
}) {
  return (
    <GlassPanel className={`w-full ${compact ? "max-w-3xl px-6 py-4" : "max-w-4xl px-7 py-5"}`}>
      <div className="flex items-start gap-5">
        <AudioWave />
        <div className="flex-1">
          <p className="mb-2 text-xs font-light tracking-[0.22em] text-white/50">{label}</p>
          <RevealText text={text} compact={compact} />
        </div>
      </div>
    </GlassPanel>
  )
}

function ContextStage() {
  return (
    <div className="relative h-full px-8">
      <div className="absolute inset-0 flex items-center justify-center">
        <ScanningEffect />
      </div>
      <motion.div
        className="absolute left-10 top-32 z-20 flex max-w-xs flex-col gap-3"
        initial="hidden"
        animate="visible"
        variants={{
          hidden: {},
          visible: { transition: { staggerChildren: 0.16 } },
        }}
      >
        <FloatingLabel>파악한 맥락</FloatingLabel>
        <div className="flex flex-wrap gap-2">
          {mockExperience.tags.map((tag) => (
            <Tag key={tag} label={tag} />
          ))}
        </div>
      </motion.div>
      <motion.div
        className="absolute right-10 top-32 z-20 flex max-w-xs flex-col gap-3"
        initial="hidden"
        animate="visible"
        variants={{
          hidden: {},
          visible: { transition: { staggerChildren: 0.18, delayChildren: 0.3 } },
        }}
      >
        <FloatingLabel>더 필요한 맥락</FloatingLabel>
        <div className="flex flex-col gap-2">
          {mockExperience.missing.map((label) => (
            <MissingTag key={label} label={label} />
          ))}
        </div>
      </motion.div>
      <div className="relative z-10 flex h-full flex-col items-center justify-center px-8 pb-28 pt-32">
        <motion.div
          className="flex max-w-4xl flex-col items-center gap-5 text-center"
          initial={{ opacity: 0, y: -14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7 }}
        >
          <div className="flex items-center gap-2 rounded-full border border-white/15 bg-white/8 px-4 py-2 text-white/75 backdrop-blur-xl">
            <Mic className="h-4 w-4" strokeWidth={1.5} />
            <span className="text-xs font-light tracking-[0.22em]">ANSWER OUT LOUD</span>
          </div>
          <h1 className="text-4xl font-extralight tracking-wide md:text-6xl">조금만 더 알려주세요</h1>
          <p className="max-w-2xl text-base font-light text-white/65 md:text-lg">
            내일의 장면을 완성하려면 세 가지가 더 필요해요. 아래 질문에 이어서 말해주세요.
          </p>
          <div className="mt-4 grid w-full grid-cols-3 gap-3">
            {mockExperience.followUpQuestions.map((question, index) => (
              <motion.div
                key={question}
                className="rounded-2xl border border-white/10 bg-black/25 px-5 py-4 text-left backdrop-blur-xl"
                initial={{ opacity: 0, y: 14 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.18 + index * 0.1, duration: 0.45 }}
              >
                <p className="mb-2 text-xs font-light tracking-[0.2em] text-white/45">QUESTION {index + 1}</p>
                <p className="text-lg font-light leading-snug text-white/85">{question}</p>
              </motion.div>
            ))}
          </div>
        </motion.div>
      </div>
      <div className="absolute inset-x-0 bottom-12 z-20 flex justify-center px-6">
        <SttPanel text={mockExperience.contextReply} label="FOLLOW-UP STT" compact />
      </div>
    </div>
  )
}

function TransformationStage() {
  return (
    <div className="relative h-full px-8">
      <GlitchFlash />
      <StyleProjectionOverlay />
      <div className="absolute inset-x-0 top-[16%] z-20 flex flex-col items-center gap-4 text-center">
        <div className="flex items-center gap-2 rounded-full border border-white/15 bg-white/8 px-4 py-2 text-white/75 backdrop-blur-xl">
          <Sparkles className="h-4 w-4" strokeWidth={1.5} />
          <span className="text-xs font-light tracking-[0.22em]">VTON PREVIEW</span>
        </div>
        <h1 className="text-4xl font-extralight tracking-wide md:text-6xl">내일 입어볼 모습을 골라보세요</h1>
        <p className="max-w-2xl text-base font-light text-white/65 md:text-lg">
          거울 속 현재 모습 위에 내일의 페르소나 스타일을 겹쳐봅니다.
        </p>
      </div>
      <div className="absolute left-8 top-1/2 z-20 flex -translate-y-1/2 flex-col gap-3">
        <RiskWidget icon={<CloudRain className="h-4 w-4" />} label="이동 변수" value={mockExperience.routeRisk} compact />
        <RiskWidget icon={<MapPin className="h-4 w-4" />} label="장소" value="성수 음식점" compact />
      </div>
      <div className="absolute right-8 top-1/2 z-20 flex -translate-y-1/2 flex-col gap-3">
        <RiskWidget icon={<UserRound className="h-4 w-4" />} label="페르소나" value={mockExperience.persona} compact />
        <RiskWidget icon={<MapPin className="h-4 w-4" />} label="공간" value={mockExperience.placeMood} compact />
      </div>
      <div className="absolute inset-x-0 bottom-14 z-20 flex justify-center px-8">
        <OutfitCarouselMock />
      </div>
    </div>
  )
}

function GestureFitStage() {
  return (
    <div className="relative flex h-full items-center justify-center px-8 pt-20">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,rgba(255,255,255,0.08),transparent_44%)]" />
      <motion.div
        className="absolute inset-x-0 top-[15%] z-20 flex flex-col items-center gap-4 text-center"
        initial={{ opacity: 0, y: -14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.65 }}
      >
        <div className="flex items-center gap-2 rounded-full border border-white/15 bg-white/8 px-4 py-2 text-white/75 backdrop-blur-xl">
          <Sparkles className="h-4 w-4" strokeWidth={1.5} />
          <span className="text-xs font-light tracking-[0.22em]">GESTURE FITTING</span>
        </div>
        <h1 className="text-4xl font-extralight tracking-wide md:text-6xl">손짓으로 내일의 모습을 골라보세요</h1>
        <p className="max-w-2xl text-base font-light text-white/65 md:text-lg">
          거울이 사용자를 인식하는 동안 Decart preview를 준비하고, 제스처로 입어볼 옷을 바꿉니다.
        </p>
      </motion.div>
      <motion.div
        className="relative mt-28 h-[440px] w-[360px]"
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.6 }}
      >
        <div className="absolute inset-x-12 top-10 h-24 rounded-full border border-white/35" />
        <div className="absolute inset-x-4 top-36 h-52 rounded-[42%] border border-white/25" />
        <div className="absolute left-1/2 top-0 h-full w-px -translate-x-1/2 bg-gradient-to-b from-transparent via-white/25 to-transparent" />
        <motion.div
          className="absolute left-1/2 top-1/2 h-28 w-28 -translate-x-1/2 -translate-y-1/2 rounded-full border border-white/25"
          animate={{ scale: [0.82, 1.18, 0.82], opacity: [0.25, 0.75, 0.25] }}
          transition={{ duration: 2.1, repeat: Infinity, ease: "easeInOut" }}
        />
        <motion.div
          className="absolute -left-10 top-1/2 flex -translate-y-1/2 items-center gap-2 text-white/55"
          animate={{ x: [-8, 0, -8], opacity: [0.45, 0.85, 0.45] }}
          transition={{ duration: 1.8, repeat: Infinity, ease: "easeInOut" }}
        >
          <ArrowLeft className="h-5 w-5" strokeWidth={1.4} />
          <span className="text-xs font-light tracking-[0.18em]">PREV</span>
        </motion.div>
        <motion.div
          className="absolute -right-10 top-1/2 flex -translate-y-1/2 items-center gap-2 text-white/55"
          animate={{ x: [8, 0, 8], opacity: [0.45, 0.85, 0.45] }}
          transition={{ duration: 1.8, repeat: Infinity, ease: "easeInOut" }}
        >
          <span className="text-xs font-light tracking-[0.18em]">NEXT</span>
          <ArrowRight className="h-5 w-5" strokeWidth={1.4} />
        </motion.div>
        <motion.div
          className="absolute inset-0 rounded-[2rem] border border-white/18"
          animate={{ boxShadow: ["0 0 0 rgba(255,255,255,0)", "0 0 60px rgba(255,255,255,0.16)", "0 0 0 rgba(255,255,255,0)"] }}
          transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
        />
        <div className="absolute inset-x-0 bottom-14 px-8">
          <ProgressCharge />
        </div>
      </motion.div>
      <div className="absolute inset-x-0 bottom-12 flex justify-center px-6">
        <GlassPanel pulsing pulseColor="rgba(255, 255, 255, 0.32)" className="max-w-3xl text-center">
          <p className="mb-2 text-xs font-light tracking-[0.26em] text-white/55">DECART PREVIEW 준비 중</p>
          <p className="text-3xl font-extralight tracking-wide">손짓으로 옷을 넘겨보세요</p>
          <p className="mt-3 text-sm font-light text-white/55">{mockExperience.gestureHint}</p>
        </GlassPanel>
      </div>
    </div>
  )
}

function RehearsalStage() {
  return (
    <div className="relative h-full px-8">
      <div className="absolute inset-0 bg-black/25 backdrop-blur-[1px]" />
      <div className="relative z-10 flex h-full items-center justify-center">
        <div className="grid w-full max-w-6xl grid-cols-[0.9fr_1.1fr] items-center gap-10">
          <motion.div
            className="rounded-[2rem] border border-white/10 bg-black/30 p-8 backdrop-blur-2xl"
            initial={{ opacity: 0, x: -24 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.6 }}
          >
            <div className="mb-6 flex items-center gap-4">
              <div className="flex h-16 w-16 items-center justify-center rounded-full border border-white/15 bg-white/10">
                <Volume2 className="h-7 w-7 text-white/70" strokeWidth={1.3} />
              </div>
              <div>
                <p className="text-xs font-light tracking-[0.22em] text-white/45">AI COUNTERPART</p>
                <p className="mt-1 text-lg font-light text-white/80">상대의 첫 한마디</p>
              </div>
            </div>
            <p className="text-4xl font-extralight leading-snug tracking-wide">“{mockExperience.aiPrompt}”</p>
            <div className="mt-8">
              <AudioWave />
            </div>
          </motion.div>

          <motion.div
            className="rounded-[2rem] border border-white/10 bg-black/35 p-8 backdrop-blur-2xl"
            initial={{ opacity: 0, x: 24 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.6, delay: 0.15 }}
          >
            <div className="mb-6 flex items-center justify-between">
              <div className="flex items-center gap-3 text-white/75">
                <motion.span
                  className="h-3 w-3 rounded-full bg-white"
                  animate={{ opacity: [0.4, 1, 0.4] }}
                  transition={{ duration: 1.2, repeat: Infinity }}
                />
                <span className="text-xs font-light tracking-[0.22em]">REC</span>
              </div>
              <div className="flex items-center gap-2 text-white/45">
                <Mic className="h-4 w-4" strokeWidth={1.5} />
                <span className="text-xs font-light tracking-[0.18em]">YOUR TURN</span>
              </div>
            </div>
            <p className="text-lg font-light leading-relaxed text-white/60">추천 응답</p>
            <p className="mt-3 text-3xl font-extralight leading-snug tracking-wide">“{mockExperience.userReply}”</p>
            <div className="mt-8 grid grid-cols-3 gap-3">
              <MiniScore label="길이" value="적절" />
              <MiniScore label="톤" value="차분" />
              <MiniScore label="시작" value="자연" />
            </div>
          </motion.div>
        </div>
      </div>
    </div>
  )
}

function ChangeCardStage() {
  return (
    <div className="relative flex h-full items-center justify-center px-8">
      <div className="absolute inset-0 bg-black/55 backdrop-blur-md" />
      <motion.div
        className="relative z-10 grid w-full max-w-5xl grid-cols-[1.3fr_0.7fr] overflow-hidden rounded-[2rem] border border-white/15 bg-neutral-100 text-neutral-950 shadow-[0_30px_120px_rgba(0,0,0,0.45)]"
        initial={{ opacity: 0, y: 40, rotateX: 8 }}
        animate={{ opacity: 1, y: 0, rotateX: 0 }}
        transition={{ duration: 0.7, ease: "easeOut" }}
      >
        <div className="p-10">
          <div className="mb-10 flex items-center justify-between border-b border-neutral-300 pb-5">
            <div>
              <p className="text-xs font-medium tracking-[0.28em] text-neutral-500">CHANGE CARD</p>
              <h2 className="mt-2 text-4xl font-light tracking-tight">내일의 변화 카드</h2>
            </div>
            <span className="rounded-full border border-neutral-300 px-4 py-2 text-sm text-neutral-600">P1-DAILY</span>
          </div>
          <ChangeRow label="오늘 바꿀 행동" value={mockExperience.changeAction} />
          <ChangeRow label="내일 유지할 태도" value={mockExperience.changeAttitude} />
          <ChangeRow label="If-Then" value={mockExperience.ifThen} />
          <div className="mt-10 flex items-center gap-3 text-neutral-500">
            <Check className="h-5 w-5" strokeWidth={1.6} />
            <span className="text-sm">내일의 리스크를 줄이는 행동 변화가 저장되었습니다.</span>
          </div>
        </div>
        <div className="flex flex-col items-center justify-center border-l border-dashed border-neutral-300 bg-neutral-950 p-8 text-white">
          <MockQr />
          <div className="mt-6 flex items-center gap-2 text-white/65">
            <QrCode className="h-4 w-4" strokeWidth={1.4} />
            <span className="text-xs font-light tracking-[0.2em]">SCAN TO SAVE</span>
          </div>
          <p className="mt-3 text-center text-sm font-light leading-relaxed text-white/55">
            개인 폰으로 스캔해 오늘의 변화 카드를 가져갑니다.
          </p>
        </div>
      </motion.div>
    </div>
  )
}

function EdgeWave() {
  return (
    <div className="pointer-events-none absolute inset-0">
      <motion.div
        className="absolute inset-4 rounded-[2rem] border border-white/20"
        animate={{ boxShadow: ["0 0 20px rgba(255,255,255,0.06)", "0 0 60px rgba(255,255,255,0.16)", "0 0 20px rgba(255,255,255,0.06)"] }}
        transition={{ duration: 2.4, repeat: Infinity, ease: "easeInOut" }}
      />
      <motion.div
        className="absolute bottom-0 left-0 right-0 h-1 bg-gradient-to-r from-transparent via-white/55 to-transparent"
        animate={{ opacity: [0.25, 0.9, 0.25], scaleX: [0.65, 1, 0.65] }}
        transition={{ duration: 1.6, repeat: Infinity, ease: "easeInOut" }}
      />
    </div>
  )
}

function RevealText({ text, compact = false }: { text: string; compact?: boolean }) {
  const words = text.split(" ")

  return (
    <p className={`${compact ? "text-lg" : "text-2xl"} font-extralight leading-relaxed tracking-wide text-white/90`}>
      {words.map((word, index) => (
        <motion.span
          key={`${word}-${index}`}
          className="mr-2 inline-block"
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: index * 0.045, duration: 0.25 }}
        >
          {word}
        </motion.span>
      ))}
    </p>
  )
}

function FloatingLabel({ children }: { children: ReactNode }) {
  return <p className="text-xs font-light tracking-[0.22em] text-white/45">{children}</p>
}

function Tag({ label }: { label: string }) {
  return (
    <motion.div
      className="rounded-full border border-white/20 bg-white/8 px-4 py-2 text-sm font-light text-white/85 backdrop-blur-xl"
      variants={{
        hidden: { opacity: 0, y: 14, scale: 0.92 },
        visible: { opacity: 1, y: 0, scale: 1 },
      }}
    >
      {label}
    </motion.div>
  )
}

function MissingTag({ label }: { label: string }) {
  return (
    <motion.div
      className="rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm font-light text-white/55 backdrop-blur-xl"
      variants={{
        hidden: { opacity: 0, y: 14, scale: 0.92 },
        visible: { opacity: 1, y: 0, scale: 1 },
      }}
    >
      {label} 필요
    </motion.div>
  )
}

function GlitchFlash() {
  return (
    <motion.div
      className="pointer-events-none absolute inset-0 z-30 bg-white"
      initial={{ opacity: 0.75 }}
      animate={{ opacity: 0 }}
      transition={{ duration: 0.35, ease: "easeOut" }}
    />
  )
}

function StyleProjectionOverlay() {
  return (
    <div className="pointer-events-none absolute inset-0">
      <motion.div
        className="absolute left-1/2 top-[52%] h-[48vh] w-[min(32vw,340px)] min-w-[250px] -translate-x-1/2 -translate-y-1/2 rounded-[40%_40%_18%_18%] border border-white/25 bg-gradient-to-b from-white/16 via-white/8 to-white/5 shadow-[0_0_90px_rgba(255,255,255,0.12)] backdrop-blur-[1px]"
        initial={{ opacity: 0, scale: 0.9, filter: "blur(12px)" }}
        animate={{ opacity: 1, scale: 1, filter: "blur(0px)" }}
        transition={{ duration: 0.75, ease: "easeOut" }}
      />
      <motion.div
        className="absolute left-1/2 top-[42%] h-[18vh] w-[min(18vw,190px)] min-w-[140px] -translate-x-1/2 -translate-y-1/2 rounded-[45%] border border-white/25"
        animate={{ borderColor: ["rgba(255,255,255,0.18)", "rgba(255,255,255,0.52)", "rgba(255,255,255,0.18)"] }}
        transition={{ duration: 2.2, repeat: Infinity, ease: "easeInOut" }}
      />
      <motion.div
        className="absolute left-1/2 top-[62%] h-px w-[min(48vw,520px)] -translate-x-1/2 bg-gradient-to-r from-transparent via-white/45 to-transparent"
        animate={{ opacity: [0.25, 0.8, 0.25], scaleX: [0.7, 1, 0.7] }}
        transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
      />
    </div>
  )
}

function OutfitCarouselMock() {
  return (
    <div className="w-full max-w-5xl">
      <div className="mb-4 flex items-center justify-center gap-5 text-white/55">
        <ArrowLeft className="h-5 w-5" strokeWidth={1.4} />
        <span className="text-xs font-light tracking-[0.24em]">SWIPE TO TRY TOMORROW'S LOOK</span>
        <ArrowRight className="h-5 w-5" strokeWidth={1.4} />
      </div>
      <div className="grid grid-cols-3 gap-4">
        {mockExperience.outfits.map((outfit, index) => (
          <motion.div
            key={outfit.name}
            className={`rounded-3xl border px-6 py-5 backdrop-blur-2xl ${
              outfit.active
                ? "border-white/45 bg-white/14 shadow-[0_0_50px_rgba(255,255,255,0.12)]"
                : "border-white/10 bg-black/25"
            }`}
            initial={{ opacity: 0, y: 20 }}
            animate={{
              opacity: outfit.active ? 1 : 0.72,
              y: outfit.active ? [0, -4, 0] : 0,
              scale: outfit.active ? 1.04 : 0.96,
            }}
            transition={{
              delay: index * 0.08,
              duration: outfit.active ? 2 : 0.5,
              repeat: outfit.active ? Infinity : 0,
              ease: "easeInOut",
            }}
          >
            <p className="mb-2 text-xs font-light tracking-[0.22em] text-white/45">LOOK {index + 1}</p>
            <p className="text-2xl font-extralight tracking-wide text-white">{outfit.name}</p>
            <p className="mt-2 text-sm font-light text-white/55">{outfit.tone}</p>
          </motion.div>
        ))}
      </div>
    </div>
  )
}

function RiskWidget({
  icon,
  label,
  value,
  compact = false,
}: {
  icon: ReactNode
  label: string
  value: string
  compact?: boolean
}) {
  return (
    <motion.div
      className={`${compact ? "w-56 px-4 py-3" : "w-64 px-5 py-4"} rounded-2xl border border-white/10 bg-black/30 backdrop-blur-2xl`}
      initial={{ opacity: 0, x: -18 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.5 }}
    >
      <div className="mb-2 flex items-center gap-2 text-white/50">
        {icon}
        <span className="text-xs font-light tracking-[0.18em]">{label}</span>
      </div>
      <p className={`${compact ? "text-lg" : "text-2xl"} font-extralight tracking-wide`}>{value}</p>
    </motion.div>
  )
}

function TomorrowSilhouette() {
  return (
    <svg className="absolute inset-x-0 bottom-20 mx-auto h-[58%] w-[78%] text-white/70" viewBox="0 0 260 360" fill="none">
      <motion.circle
        cx="130"
        cy="54"
        r="38"
        stroke="currentColor"
        strokeWidth="1"
        initial={{ pathLength: 0 }}
        animate={{ pathLength: 1 }}
        transition={{ duration: 0.8, delay: 0.2 }}
      />
      <motion.path
        d="M72 132C84 105 103 94 130 94C157 94 176 105 188 132L210 306H50L72 132Z"
        stroke="currentColor"
        strokeWidth="1"
        fill="rgba(255,255,255,0.04)"
        initial={{ pathLength: 0, opacity: 0 }}
        animate={{ pathLength: 1, opacity: 1 }}
        transition={{ duration: 1, delay: 0.35 }}
      />
      <motion.path
        d="M78 132H182M92 166H168M104 204H156"
        stroke="rgba(125,211,252,0.7)"
        strokeWidth="1"
        strokeDasharray="4 6"
        initial={{ pathLength: 0 }}
        animate={{ pathLength: 1 }}
        transition={{ duration: 1.1, delay: 0.55 }}
      />
    </svg>
  )
}

function ProgressCharge() {
  return (
    <div className="rounded-full border border-white/10 bg-black/40 p-2 backdrop-blur-xl">
      <motion.div
        className="h-3 rounded-full bg-gradient-to-r from-white/45 via-white/75 to-white"
        initial={{ width: "8%" }}
        animate={{ width: ["8%", "42%", "72%", "100%"] }}
        transition={{ duration: 3, repeat: Infinity, repeatDelay: 0.6, ease: "easeInOut" }}
      />
    </div>
  )
}

function MiniScore({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-center">
      <p className="text-xs font-light tracking-[0.18em] text-white/40">{label}</p>
      <p className="mt-1 text-lg font-light text-white/80">{value}</p>
    </div>
  )
}

function ChangeRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="border-b border-neutral-200 py-5">
      <p className="text-xs font-medium tracking-[0.22em] text-neutral-500">{label}</p>
      <p className="mt-2 text-3xl font-light tracking-tight text-neutral-950">{value}</p>
    </div>
  )
}

function MockQr() {
  const filled = new Set([0, 1, 2, 4, 6, 8, 10, 14, 16, 18, 20, 21, 22, 25, 27, 30, 32, 34, 36, 38, 40, 42, 45, 48, 50, 52, 54, 56, 58, 60, 62, 64, 65, 66, 70, 72, 74, 76, 78, 80])

  return (
    <div className="grid h-48 w-48 grid-cols-9 gap-1 rounded-2xl bg-white p-4">
      {Array.from({ length: 81 }).map((_, index) => (
        <div key={index} className={`rounded-[2px] ${filled.has(index) ? "bg-neutral-950" : "bg-transparent"}`} />
      ))}
    </div>
  )
}

function TapHint({ isLast }: { isLast: boolean }) {
  return (
    <motion.div
      className="absolute bottom-6 left-1/2 z-30 -translate-x-1/2 text-center text-xs font-light tracking-[0.2em] text-white/45"
      animate={{ opacity: [0.25, 0.75, 0.25] }}
      transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
    >
      {isLast ? "탭하거나 Enter를 눌러 다시 시작" : "탭하거나 Enter를 눌러 계속"}
    </motion.div>
  )
}
