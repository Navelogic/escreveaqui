import { useParams } from "react-router-dom";
import { useState, useEffect, useRef } from "react";
import { EditorContainer, TextareaWrapper, StyledTextarea } from "./styles";
import InsertionPoint from "../../components/InsertionPoint";

const INACTIVITY_TIMEOUT = 2000;


export default function Editor() {
  const { key } = useParams();
  const [text, setText] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const [cursorPosition, setCursorPosition] = useState({ top: 20, left: 20 });
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setText(e.target.value);
    setIsTyping(true);

    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }

    typingTimeoutRef.current = setTimeout(() => {
      setIsTyping(false);
    }, INACTIVITY_TIMEOUT);
  };

  const updateCursorPosition = () => {
    if (!textareaRef.current) return;

    const textarea = textareaRef.current;
    const selectionStart = textarea.selectionStart;

    const textBeforeCursor = text.substring(0, selectionStart);
    const lines = textBeforeCursor.split('\n');
    const currentLine = lines.length - 1;
    const currentCol = lines[lines.length - 1].length;

    const lineHeight = 24;
    const charWidth = 10.8;

    setCursorPosition({
      top: 20 + (currentLine * lineHeight),
      left: 20 + (currentCol * charWidth)
    });
  };

  useEffect(() => {
    updateCursorPosition();
  }, [text]);

  useEffect(() => {
    const titleText = (key || "").trim().substring(0, 10);
    if (titleText) {
      document.title = `${titleText} | escreveaqui`;
    } else {
      document.title = "escreveaqui";
    }
  }, [key]);

  useEffect(() => {
    return () => {
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }
    };
  }, []);

  return (
    <EditorContainer>
      <TextareaWrapper>
        <InsertionPoint
          top={cursorPosition.top}
          left={cursorPosition.left}
          isTyping={isTyping}
          hasText={text.length > 0}
        />
        <StyledTextarea
          ref={textareaRef}
          value={text}
          onChange={handleChange}
          onKeyUp={updateCursorPosition}
          onClick={updateCursorPosition}
          placeholder={`Escrevendo em: ${key}`}
          autoFocus
        />
      </TextareaWrapper>
    </EditorContainer>
  );
}