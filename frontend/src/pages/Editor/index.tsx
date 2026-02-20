import { useParams } from "react-router-dom";
import { useState, useEffect, useRef, useCallback } from "react";
import { EditorContainer, TextareaWrapper, StyledTextarea } from "./styles";
import InsertionPoint from "../../components/InsertionPoint";
import { notaService } from "../../services/notaService";
import debounce from "lodash.debounce";
import { useMemo } from "react";
import type { DebouncedFunc } from "lodash";

const INACTIVITY_TIMEOUT = 2000;
const AUTO_SAVE_DELAY = 1000;

export default function Editor() {
  const { key } = useParams<{ key: string }>();
  const [text, setText] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const [cursorPosition, setCursorPosition] = useState({ top: 20, left: 20 });
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

useEffect(() => {
  if (!key) return;
  const fetchUpdates = async () => {
    if (!isTyping) {
      try {
        const nota = await notaService.getBySlug(key);
        if (nota?.content !== undefined && nota.content !== text) {
          setText(nota.content);
        }
      } catch (err) {
        console.error("Erro no polling de atualização:", err);
      }
    }
  };

  const interval = setInterval(fetchUpdates, 2000);

  return () => clearInterval(interval);
}, [key, isTyping, text]);

  const saveToBackend = useMemo(
    () =>
      debounce((slug: string, content: string) => {
        notaService.upsert(slug, content)
          .catch((err) => console.error("Falha ao salvar no banco:", err));
      }, AUTO_SAVE_DELAY),
    []
  );

  useEffect(() => {
    return () => {
      saveToBackend.cancel();
    };
  }, [saveToBackend]);

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const newText = e.target.value;
    setText(newText);
    setIsTyping(true);

    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }

    typingTimeoutRef.current = setTimeout(() => {
      setIsTyping(false);
    }, INACTIVITY_TIMEOUT);

    if (key) {
      saveToBackend(key, newText);
    }
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
    document.title = titleText ? `${titleText} | escreveaqui` : "escreveaqui";
  }, [key]);

  return (
    <EditorContainer>
      <TextareaWrapper>
        <InsertionPoint
        top={cursorPosition.top}
        left={cursorPosition.left}
        isTyping={isTyping}
        hasText={(text?.length ?? 0) > 0}
        />
        <StyledTextarea
        ref={textareaRef}
        value={text || ""}
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