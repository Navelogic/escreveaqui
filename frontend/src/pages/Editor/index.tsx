import { useParams, useNavigate, Link } from "react-router-dom"
import { useState, useEffect, useRef, useMemo } from "react"
import { Textarea } from "@/components/ui/textarea"
import { notaService } from "@/services/notaService"
import { formatSlug } from "@/lib/utils"
import debounce from "lodash.debounce"
import type { DebouncedFunc } from "lodash"
import { Check, LoaderCircle, X } from "lucide-react"

const BR_COLORS = ["#009c3b", "#ffdf00", "#002776"]
const INACTIVITY_TIMEOUT = 2000
const AUTO_SAVE_DELAY = 1000

type SaveStatus = "idle" | "saving" | "saved" | "error"

export default function Editor() {
  const { key } = useParams<{ key: string }>()
  const navigate = useNavigate()
  const [text, setText] = useState("")
  const [isTyping, setIsTyping] = useState(false)
  const [caretIndex, setCaretIndex] = useState(0)
  const [saveStatus, setSaveStatus] = useState<SaveStatus>("idle")
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    if (!key) return
    const formattedKey = formatSlug(key)
    if (!formattedKey) {
      navigate("/", { replace: true })
      return
    }
    if (formattedKey !== key) {
      navigate("/" + formattedKey, { replace: true })
    }
  }, [key, navigate])

  useEffect(() => {
    const interval = setInterval(() => {
      setCaretIndex((prev) => (prev + 1) % BR_COLORS.length)
    }, 800)
    return () => clearInterval(interval)
  }, [])

  const isTypingRef = useRef(isTyping)
  useEffect(() => {
    isTypingRef.current = isTyping
  }, [isTyping])

  // 1. Busca inicial da nota (executa apenas uma vez ao montar ou mudar de slug)
  useEffect(() => {
    if (!key) return
    let isMounted = true

    notaService.getBySlug(key)
      .then((nota) => {
        if (isMounted && nota?.content !== undefined) {
          setText(nota.content)
        }
      })
      .catch((err) => {
        console.error("Erro na busca inicial da nota:", err)
      })

    return () => {
      isMounted = false
    }
  }, [key])

  // 2. Conexão SSE para atualizações remotas em tempo real (não resseta ao digitar)
  useEffect(() => {
    if (!key) return

    const baseUrl = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1/notes"
    const sseUrl = `${baseUrl}/${encodeURIComponent(key)}/stream`
    const eventSource = new EventSource(sseUrl)

    eventSource.addEventListener("nota-update", (event: MessageEvent) => {
      const newContent = event.data
      if (!isTypingRef.current) {
        setText(newContent)
      }
    })

    eventSource.onerror = (err) => {
      console.debug("Conexão SSE reconectando...", err)
    }

    return () => {
      eventSource.close()
    }
  }, [key])

  const saveToBackend: DebouncedFunc<(slug: string, content: string) => void> = useMemo(
    () =>
      debounce((slug: string, content: string) => {
        notaService
          .upsert(slug, content)
          .then(() => setSaveStatus("saved"))
          .catch((err) => {
            setSaveStatus("error")
            console.error("Falha ao salvar no banco:", err)
          })
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

    if (key) {
      setSaveStatus("saving")
      saveToBackend(key, newText)
    }
  }

  useEffect(() => {
    const titleText = (key ?? "").trim().substring(0, 10)
    document.title = titleText ? `${titleText} | escreveaqui` : "escreveaqui"
  }, [key])

  useEffect(() => {
    // Notas são acessíveis a qualquer pessoa com a URL (ver Política de Privacidade),
    // então páginas individuais não devem ser indexadas por buscadores.
    const robotsMeta = document.querySelector('meta[name="robots"]')
    const previousContent = robotsMeta?.getAttribute("content") ?? "index, follow"
    robotsMeta?.setAttribute("content", "noindex, follow")
    return () => {
      robotsMeta?.setAttribute("content", previousContent)
    }
  }, [])

  return (
    <div className="w-full h-screen bg-background">
      <Link
        to="/"
        className="fixed left-5 top-4 z-10 font-mono text-sm text-foreground/55 hover:text-foreground transition-colors"
      >
        escreveaqui.com.br
      </Link>
      {saveStatus !== "idle" && (
        <div
          aria-live="polite"
          className={`pointer-events-none fixed right-5 top-4 z-10 flex items-center gap-1.5 text-sm ${
            saveStatus === "error" ? "text-destructive" : "text-foreground/55"
          }`}
        >
          {saveStatus === "saving" ? (
            <LoaderCircle aria-hidden="true" className="size-4 animate-spin opacity-60" />
          ) : saveStatus === "saved" ? (
            <Check aria-hidden="true" className="size-4 stroke-[3] opacity-60" />
          ) : (
            <X aria-hidden="true" className="size-4 stroke-[3] opacity-60" />
          )}
          {saveStatus === "saving"
            ? "Salvando…"
            : saveStatus === "saved"
              ? "Salvo"
              : "Erro ao salvar"}
        </div>
      )}
      <Textarea
        value={text}
        onChange={handleChange}
        placeholder={`Escrevendo em: ${key}`}
        autoFocus
        className="w-full h-full resize-none border-none rounded-none p-5 pt-14 font-mono text-[18px] leading-6 focus-visible:ring-0 focus-visible:ring-offset-0 placeholder:text-muted-foreground/40 [scrollbar-width:thin] [scrollbar-color:hsl(var(--border))_transparent]"
        style={{ caretColor: BR_COLORS[caretIndex] }}
      />
    </div>
  )
}
