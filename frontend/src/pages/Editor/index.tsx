import { useParams } from "react-router-dom"
import { useState, useEffect, useRef, useMemo } from "react"
import { Textarea } from "@/components/ui/textarea"
import { notaService } from "@/services/notaService"
import debounce from "lodash.debounce"
import type { DebouncedFunc } from "lodash"

const BR_COLORS = ["#009c3b", "#ffdf00", "#002776"]
const INACTIVITY_TIMEOUT = 2000
const AUTO_SAVE_DELAY = 1000

export default function Editor() {
  const { key } = useParams<{ key: string }>()
  const [text, setText] = useState("")
  const [isTyping, setIsTyping] = useState(false)
  const [caretIndex, setCaretIndex] = useState(0)
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    const interval = setInterval(() => {
      setCaretIndex((prev) => (prev + 1) % BR_COLORS.length)
    }, 800)
    return () => clearInterval(interval)
  }, [])

  useEffect(() => {
    if (!key) return
    const fetchUpdates = async () => {
      if (!isTyping) {
        try {
          const nota = await notaService.getBySlug(key)
          if (nota?.content !== undefined && nota.content !== text) {
            setText(nota.content)
          }
        } catch (err) {
          console.error("Erro no polling de atualização:", err)
        }
      }
    }
    const interval = setInterval(fetchUpdates, 2000)
    return () => clearInterval(interval)
  }, [key, isTyping, text])

  const saveToBackend: DebouncedFunc<(slug: string, content: string) => void> = useMemo(
    () =>
      debounce((slug: string, content: string) => {
        notaService
          .upsert(slug, content)
          .catch((err) => console.error("Falha ao salvar no banco:", err))
      }, AUTO_SAVE_DELAY),
    []
  )

  useEffect(() => {
    return () => {
      saveToBackend.cancel()
    }
  }, [saveToBackend])

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const newText = e.target.value
    setText(newText)
    setIsTyping(true)

    if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current)
    typingTimeoutRef.current = setTimeout(() => {
      setIsTyping(false)
    }, INACTIVITY_TIMEOUT)

    if (key) saveToBackend(key, newText)
  }

  useEffect(() => {
    const titleText = (key ?? "").trim().substring(0, 10)
    document.title = titleText ? `${titleText} | escreveaqui` : "escreveaqui"
  }, [key])

  return (
    <div className="w-full h-screen bg-background">
      <Textarea
        value={text}
        onChange={handleChange}
        placeholder={`Escrevendo em: ${key}`}
        autoFocus
        className="w-full h-full resize-none border-none rounded-none font-mono text-[18px] leading-6 p-5 focus-visible:ring-0 focus-visible:ring-offset-0 placeholder:text-muted-foreground/40 [scrollbar-width:thin] [scrollbar-color:hsl(var(--border))_transparent]"
        style={{ caretColor: BR_COLORS[caretIndex] }}
      />
    </div>
  )
}
