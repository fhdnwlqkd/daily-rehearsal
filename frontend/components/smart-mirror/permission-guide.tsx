"use client"

import { AlertCircle, Camera, Mic, Settings } from "lucide-react"
import { motion } from "framer-motion"
import { Button } from "@/components/ui/button"

interface PermissionGuideProps {
  cameraError: boolean
  audioError: boolean
  onRetry: () => void
}

export function PermissionGuide({ cameraError, audioError, onRetry }: PermissionGuideProps) {
  return (
    <div className="absolute inset-0 z-[100] flex items-center justify-center bg-black/80 backdrop-blur-md">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="max-w-md space-y-8 p-8 text-center text-white"
      >
        <div className="flex justify-center space-x-4">
          <div className={`rounded-full p-4 ${cameraError ? "bg-red-500/20" : "bg-green-500/20"}`}>
            <Camera className={`h-8 w-8 ${cameraError ? "text-red-500" : "text-green-500"}`} />
          </div>
          <div className={`rounded-full p-4 ${audioError ? "bg-red-500/20" : "bg-green-500/20"}`}>
            <Mic className={`h-8 w-8 ${audioError ? "text-red-500" : "text-green-500"}`} />
          </div>
        </div>

        <div className="space-y-4">
          <h2 className="text-2xl font-light tracking-tight">권한 설정이 필요합니다</h2>
          <p className="text-white/60 font-extralight leading-relaxed">
            스마트 미러 기능을 사용하기 위해 카메라와 마이크 권한이 필요합니다. 
            브라우저 주소창 좌측의 설정 아이콘을 클릭하여 권한을 허용해 주세요.
          </p>
        </div>

        <div className="rounded-2xl bg-white/5 p-6 text-left space-y-4 border border-white/10">
          <div className="flex items-start gap-3">
            <div className="mt-1 rounded-full bg-blue-500/20 p-1">
              <Settings className="h-4 w-4 text-blue-400" />
            </div>
            <div>
              <p className="text-sm font-medium text-white/90">Chrome 설정 방법</p>
              <p className="mt-1 text-xs text-white/50 leading-normal">
                주소창 왼쪽 <span className="text-blue-400">자물쇠 아이콘</span> 클릭 → 
                <span className="text-blue-400"> 사이트 설정</span> → 
                <span className="text-blue-400"> 카메라/마이크</span> '허용'으로 변경
              </p>
            </div>
          </div>
        </div>

        <div className="flex flex-col gap-3">
          <Button 
            onClick={onRetry}
            className="w-full bg-white text-black hover:bg-white/90 rounded-full py-6"
          >
            다시 시도하기
          </Button>
          <p className="text-[10px] text-white/30 flex items-center justify-center gap-1">
            <AlertCircle className="h-3 w-3" />
            권한을 허용하지 않으면 정상적인 서비스 이용이 불가능합니다.
          </p>
        </div>
      </motion.div>
    </div>
  )
}
