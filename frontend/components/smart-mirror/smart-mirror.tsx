"use client"

import { useState, useEffect, useCallback } from "react"
import { motion, AnimatePresence } from "framer-motion"
import { WebcamBackground } from "./webcam-background"
import { StateSelector } from "./state-selector"
import { StandbyState } from "./states/standby-state"
import { ListeningState } from "./states/listening-state"
import { LoadingState } from "./states/loading-state"
import { ResultState } from "./states/result-state"
import { PermissionGuide } from "./permission-guide"

export type MirrorState = 1 | 2 | 3 | 4

export function SmartMirror() {
  const [currentState, setCurrentState] = useState<MirrorState>(1)
  const [permissions, setPermissions] = useState<{
    camera: boolean | null
    audio: boolean | null
  }>({ camera: null, audio: null })

  const checkPermissions = useCallback(async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: true,
        audio: true,
      })
      // If successful, stop the tracks and set permissions to true
      stream.getTracks().forEach((track) => track.stop())
      setPermissions({ camera: true, audio: true })
    } catch (error: any) {
      console.error("Permission check failed:", error)
      const isCameraError = error.name === "NotAllowedError" || error.name === "NotFoundError"
      // Since getUserMedia for both fails if either is denied, we handle it conservatively
      setPermissions({
        camera: error.message?.includes("video") ? false : null,
        audio: error.message?.includes("audio") ? false : null,
      })
      
      // Fallback: If we can't distinguish, assume both are needed/failed for now
      if (error.name === "NotAllowedError") {
        setPermissions({ camera: false, audio: false })
      }
    }
  }, [])

  useEffect(() => {
    checkPermissions()
  }, [checkPermissions])

  const showPermissionGuide = permissions.camera === false || permissions.audio === false

  return (
    <div className="relative h-screen w-screen overflow-hidden bg-black">
      {/* Permission Guide Overlay */}
      <AnimatePresence>
        {showPermissionGuide && (
          <PermissionGuide
            cameraError={permissions.camera === false}
            audioError={permissions.audio === false}
            onRetry={checkPermissions}
          />
        )}
      </AnimatePresence>

      {/* Webcam Background Layer (z-0) */}
      <WebcamBackground />

      {/* State Content Layer */}
      <AnimatePresence mode="wait">
        {currentState === 1 && (
          <motion.div
            key="standby"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.6, ease: "easeInOut" }}
            className="absolute inset-0 z-10"
          >
            <StandbyState />
          </motion.div>
        )}
        {currentState === 2 && (
          <motion.div
            key="listening"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.6, ease: "easeInOut" }}
            className="absolute inset-0 z-10"
          >
            <ListeningState />
          </motion.div>
        )}
        {currentState === 3 && (
          <motion.div
            key="loading"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.6, ease: "easeInOut" }}
            className="absolute inset-0 z-10"
          >
            <LoadingState />
          </motion.div>
        )}
        {currentState === 4 && (
          <motion.div
            key="result"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.6, ease: "easeInOut" }}
            className="absolute inset-0 z-10"
          >
            <ResultState />
          </motion.div>
        )}
      </AnimatePresence>

      {/* State Selector (Top Right) */}
      <StateSelector currentState={currentState} onStateChange={setCurrentState} />
    </div>
  )
}
