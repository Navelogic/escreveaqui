import { useParams, useNavigate, Link } from "react-router-dom"
import { useState, useEffect, useRef, useMemo, type FormEvent } from "react"
import axios from "axios"
import { Textarea } from "@/components/ui/textarea"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import Modal from "@/components/Modal"
import { notaService, encodeSecret, API_BASE_URL } from "@/services/notaService"
import { formatSlug } from "@/lib/utils"
import debounce from "lodash.debounce"
import type { DebouncedFunc } from "lodash"
import { Check, Lock, LockOpen, LoaderCircle, X } from "lucide-react"

const BR_COLORS = ["#009c3b", "#ffdf00", "#002776"]
const INACTIVITY_TIMEOUT = 2000
const AUTO_SAVE_DELAY = 1000

type SaveStatus = "idle" | "saving" | "saved" | "error"

const isUnauthorized = (err: unknown) => axios.isAxiosError(err) && err.response?.status === 401

export default function Editor() {
  const { key } = useParams<{ key: string }>()
  const navigate = useNavigate()
  const [text, setText] = useState("")
  const [isTyping, setIsTyping] = useState(false)
  const [caretIndex, setCaretIndex] = useState(0)
  const [saveStatus, setSaveStatus] = useState<SaveStatus>("idle")
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  // Segredo opcional da nota: fica só em memória, some ao recarregar a página.
  const [secret, setSecret] = useState<string | null>(null)
  const [hasSecret, setHasSecret] = useState(false)
  const [locked, setLocked] = useState(false)
  const [secretInput, setSecretInput] = useState("")
  const [secretError, setSecretError] = useState<string | null>(null)
  const [isSecretModalOpen, setIsSecretModalOpen] = useState(false)

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
        if (!isMounted) return
        setText(nota.content ?? "")
        setHasSecret(nota.hasSecret)
      })
      .catch((err) => {
        if (!isMounted) return
        if (isUnauthorized(err)) {
          setHasSecret(true)
          setLocked(true)
          return
        }
        console.error("Erro na busca inicial da nota:", err)
      })

    return () => {
      isMounted = false
    }
  }, [key])

  // 2. Conexão SSE para atualizações remotas em tempo real (não resseta ao digitar)
  useEffect(() => {
    if (!key || locked) return

    const query = secret ? `?secret=${encodeURIComponent(encodeSecret(secret))}` : ""
    const eventSource = new EventSource(`${API_BASE_URL}/${encodeURIComponent(key)}/stream${query}`)

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
  }, [key, locked, secret])

  const saveToBackend: DebouncedFunc<(slug: string, content: string, noteSecret: string | null) => void> = useMemo(
    () =>
      debounce((slug: string, content: string, noteSecret: string | null) => {
        notaService
          .upsert(slug, content, noteSecret)
          .then(() => setSaveStatus("saved"))
          .catch((err) => {
            setSaveStatus("error")
            if (isUnauthorized(err)) setLocked(true)
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
      saveToBackend(key, newText, secret)
    }
  }

  const handleUnlock = async (e: FormEvent) => {
    e.preventDefault()
    if (!key || !secretInput) return
    setSecretError(null)
    try {
      const nota = await notaService.getBySlug(key, secretInput)
      setText(nota.content ?? "")
      setHasSecret(nota.hasSecret)
      setSecret(secretInput)
      setSecretInput("")
      setLocked(false)
    } catch (err) {
      setSecretError(isUnauthorized(err) ? "Segredo incorreto." : "Não foi possível abrir a nota.")
    }
  }

  const handleDefineSecret = async (e: FormEvent) => {
    e.preventDefault()
    if (!key || !secretInput) return
    setSecretError(null)
    try {
      await notaService.upsert(key, text, secretInput)
      setSecret(secretInput)
      setHasSecret(true)
      setSecretInput("")
      setIsSecretModalOpen(false)
    } catch (err) {
      setSecretError(isUnauthorized(err) ? "Segredo incorreto." : "Não foi possível definir o segredo.")
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

  const homeLink = (
    <Link
      to="/"
      className="fixed left-5 top-4 z-10 font-mono text-sm text-foreground/55 hover:text-foreground transition-colors"
    >
      escreveaqui.com.br
    </Link>
  )

  if (locked) {
    return (
      <div className="w-full h-screen bg-background flex items-center justify-center px-4">
        {homeLink}
        <form onSubmit={handleUnlock} className="w-full max-w-sm space-y-3">
          <div className="flex items-center gap-2 font-mono text-sm text-foreground/70">
            <Lock aria-hidden="true" className="size-4" />
            Esta nota tem segredo
          </div>
          <label htmlFor="note-secret" className="sr-only">
            Segredo da nota
          </label>
          <Input
            id="note-secret"
            type="password"
            autoFocus
            value={secretInput}
            onChange={(e) => setSecretInput(e.target.value)}
            placeholder="Segredo"
          />
          {secretError && <p className="text-sm text-destructive">{secretError}</p>}
          <Button type="submit" className="w-full font-mono">
            abrir
          </Button>
        </form>
      </div>
    )
  }

  return (
    <div className="w-full h-screen bg-background">
      {homeLink}
      <div className="fixed right-5 top-4 z-10 flex items-center gap-3">
        {saveStatus !== "idle" && (
          <div
            aria-live="polite"
            className={`pointer-events-none flex items-center gap-1.5 text-sm ${
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
        {hasSecret ? (
          <Lock aria-label="Nota protegida por segredo" className="size-4 text-foreground/55" />
        ) : (
          <button
            type="button"
            onClick={() => {
              setSecretError(null)
              setSecretInput("")
              setIsSecretModalOpen(true)
            }}
            aria-label="Definir segredo da nota"
            title="Definir segredo da nota"
            className="text-foreground/55 hover:text-foreground transition-colors"
          >
            <LockOpen aria-hidden="true" className="size-4" />
          </button>
        )}
      </div>
      <Textarea
        value={text}
        onChange={handleChange}
        placeholder={`Escrevendo em: ${key}`}
        autoFocus
        className="w-full h-full resize-none border-none rounded-none p-5 pt-14 font-mono text-[18px] leading-6 focus-visible:ring-0 focus-visible:ring-offset-0 placeholder:text-muted-foreground/40 [scrollbar-width:thin] [scrollbar-color:hsl(var(--border))_transparent]"
        style={{ caretColor: BR_COLORS[caretIndex] }}
      />

      <Modal
        isOpen={isSecretModalOpen}
        onClose={() => setIsSecretModalOpen(false)}
        title="Definir segredo"
      >
        <form onSubmit={handleDefineSecret} className="space-y-3">
          <p>
            Quem não souber o segredo não consegue ler nem editar esta nota. O segredo não pode ser
            alterado nem recuperado depois.
          </p>
          <label htmlFor="new-note-secret" className="sr-only">
            Novo segredo
          </label>
          <Input
            id="new-note-secret"
            type="password"
            maxLength={128}
            value={secretInput}
            onChange={(e) => setSecretInput(e.target.value)}
            placeholder="Segredo"
          />
          {secretError && <p className="text-destructive">{secretError}</p>}
          <Button type="submit" className="w-full font-mono">
            proteger
          </Button>
        </form>
      </Modal>
    </div>
  )
}
