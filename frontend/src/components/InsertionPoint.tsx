import { useState, useEffect } from "react";
import styled, { keyframes, css } from "styled-components";

const BR_COLORS = ["var(--color-green-flag)", "var(--color-yellow-flag)", "var(--color-blue-flag)"]; // Verde, Amarelo, Azul oficial

const blink = keyframes`
  0%, 49% {
    opacity: 1;
  }
  50%, 100% {
    opacity: 0;
  }
`;

const Cursor = styled.span<{ $color: string; $isTyping: boolean }>`
  position: absolute;
  width: 3px;
  height: 24px;
  background-color: ${props => props.$color};
  pointer-events: none;
  z-index: 0;
  transition: background-color 0.3s ease;
  
  ${props => !props.$isTyping && css`
    animation: ${blink} 1s step-end infinite;
  `}
`;

interface InsertionPointProps {
  top: number;
  left: number;
  isTyping: boolean;
  hasText: boolean;
}

export default function InsertionPoint({ top, left, isTyping, hasText }: InsertionPointProps) {
  const [colorIndex, setColorIndex] = useState(0);
  const [frozenColor, setFrozenColor] = useState<string | null>(null);

  useEffect(() => {
    if (hasText && isTyping) {
      if (frozenColor === null) {
        setFrozenColor(BR_COLORS[colorIndex]);
      }
      return;
    }

    setFrozenColor(null);

    const interval = setInterval(() => {
      setColorIndex((prev) => (prev + 1) % BR_COLORS.length);
    }, 600);

    return () => clearInterval(interval);
  }, [hasText, isTyping, colorIndex, frozenColor]);

  const currentColor = frozenColor || BR_COLORS[colorIndex];

  return (
    <Cursor
      $color={currentColor}
      $isTyping={isTyping}
      style={{
        top: `${top}px`,
        left: `${left}px`
      }}
    />
  );
}
