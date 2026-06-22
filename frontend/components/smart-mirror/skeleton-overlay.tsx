"use client";

import { motion } from "framer-motion";

export function SkeletonOverlay() {
  const jointPositions = {
    head: { x: 200, y: 40 },
    neck: { x: 200, y: 80 },
    leftShoulder: { x: 140, y: 100 },
    rightShoulder: { x: 260, y: 95 },
    leftElbow: { x: 100, y: 170 },
    rightElbow: { x: 300, y: 165 },
    leftWrist: { x: 70, y: 240 },
    rightWrist: { x: 330, y: 235 },
    spine: { x: 200, y: 180 },
    hip: { x: 200, y: 260 },
    leftHip: { x: 170, y: 260 },
    rightHip: { x: 230, y: 260 },
    leftKnee: { x: 160, y: 360 },
    rightKnee: { x: 240, y: 360 },
    leftAnkle: { x: 155, y: 460 },
    rightAnkle: { x: 245, y: 460 },
  };

  const bones = [
    ["head", "neck"],
    ["neck", "leftShoulder"],
    ["neck", "rightShoulder"],
    ["leftShoulder", "leftElbow"],
    ["rightShoulder", "rightElbow"],
    ["leftElbow", "leftWrist"],
    ["rightElbow", "rightWrist"],
    ["neck", "spine"],
    ["spine", "hip"],
    ["hip", "leftHip"],
    ["hip", "rightHip"],
    ["leftHip", "leftKnee"],
    ["rightHip", "rightKnee"],
    ["leftKnee", "leftAnkle"],
    ["rightKnee", "rightAnkle"],
  ];

  return (
    <motion.svg
      width="400"
      height="500"
      viewBox="0 0 400 500"
      className="opacity-60"
      initial={{ opacity: 0 }}
      animate={{ opacity: 0.6 }}
      transition={{ duration: 1 }}
    >
      {/* Bones */}
      {bones.map(([start, end], index) => {
        const startPos = jointPositions[start as keyof typeof jointPositions];
        const endPos = jointPositions[end as keyof typeof jointPositions];
        return (
          <motion.line
            key={`bone-${index}`}
            x1={startPos.x}
            y1={startPos.y}
            x2={endPos.x}
            y2={endPos.y}
            stroke="white"
            strokeWidth="1"
            strokeOpacity="0.4"
            initial={{ pathLength: 0 }}
            animate={{ pathLength: 1 }}
            transition={{ duration: 1, delay: index * 0.05 }}
          />
        );
      })}

      {/* Joints */}
      {Object.entries(jointPositions).map(([name, pos], index) => (
        <motion.circle
          key={name}
          cx={pos.x}
          cy={pos.y}
          r={name === "head" ? 20 : 4}
          fill="none"
          stroke="white"
          strokeWidth="1"
          strokeOpacity="0.5"
          initial={{ scale: 0, opacity: 0 }}
          animate={{ scale: 1, opacity: 0.5 }}
          transition={{ duration: 0.3, delay: 0.5 + index * 0.03 }}
        />
      ))}

      {/* Asymmetry indicator on shoulders */}
      <motion.path
        d="M 140 100 Q 200 85 260 95"
        fill="none"
        stroke="rgba(239, 68, 68, 0.6)"
        strokeWidth="2"
        strokeDasharray="4 4"
        initial={{ pathLength: 0 }}
        animate={{ pathLength: 1 }}
        transition={{ duration: 1.5, delay: 1 }}
      />
    </motion.svg>
  );
}
