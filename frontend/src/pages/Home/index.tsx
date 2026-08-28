import { useState, type SyntheticEvent } from "react"
import { useNavigate } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import Modal from "@/components/Modal"
import Contributors from "@/components/Contributors"
import { formatSlug } from "@/lib/utils"

export default function Home() {
  const [path, setPath] = useState("")
  const [activeModal, setActiveModal] = useState<"privacy" | "cookies" | null>(null)
  const navigate = useNavigate()

  const handleSubmit = (e: SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault()
    const sanitizedPath = formatSlug(path)
    if (!sanitizedPath) return
    navigate("/" + sanitizedPath)
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center px-4">
      <div className="w-full max-w-md space-y-6">
        <div className="space-y-1">
          <h1 className="font-mono text-4xl font-bold tracking-tight">
            escreve <span className="text-primary">aqui</span>
          </h1>
          <p className="text-sm text-muted-foreground">
            <a
              href="https://github.com/Navelogic/escreveaqui"
              target="_blank"
              rel="noopener noreferrer"
              className="underline underline-offset-4 hover:text-foreground transition-colors"
            >
              Código aberto
            </a>{" "}
            • Feito no Brasil 🇧🇷
          </p>
        </div>

        <Contributors />

        <form onSubmit={handleSubmit} className="flex items-center">
          <label htmlFor="note-slug" className="sr-only">
            Nome da nota
          </label>
          <span
            id="note-slug-prefix"
            className="flex h-10 items-center rounded-l-md border border-r-0 border-input bg-muted px-3 text-sm text-muted-foreground whitespace-nowrap select-none"
          >
            {window.location.host}/
          </span>
          <Input
            id="note-slug"
            value={path}
            onChange={(e) => setPath(e.target.value)}
            placeholder="minha-nota"
            aria-describedby="note-slug-prefix"
            className="rounded-none border-x-0 focus-visible:ring-0 focus-visible:ring-offset-0 focus-visible:border-ring"
          />
          <Button type="submit" className="rounded-l-none font-mono">
            criar
          </Button>
        </form>
      </div>

      <footer className="absolute bottom-6 flex gap-2">
        <Button
          variant="link"
          size="sm"
          onClick={() => setActiveModal("privacy")}
        >
          Privacidade
        </Button>
        <Button
          variant="link"
          size="sm"
          onClick={() => setActiveModal("cookies")}
        >
          Cookies
        </Button>
      </footer>

      <Modal
        isOpen={activeModal === "privacy"}
        onClose={() => setActiveModal(null)}
        title="Política de Privacidade"
      >
        <p>
          O <strong>escreveaqui</strong> é um serviço minimalista e focado na privacidade.
        </p>
        <ul>
          <li>Não solicitamos informações pessoais como nome, email ou telefone.</li>
          <li>O conteúdo das notas é armazenado associado apenas à URL que você criou.</li>
          <li>Qualquer pessoa com acesso à URL da nota poderá ler e editar seu conteúdo, a menos que você defina um segredo para ela.</li>
          <li>O segredo é guardado apenas como hash e não pode ser recuperado se você esquecê-lo.</li>
          <li>Recomendamos não armazenar informações sensíveis (senhas, dados bancários, etc).</li>
        </ul>
      </Modal>

      <Modal
        isOpen={activeModal === "cookies"}
        onClose={() => setActiveModal(null)}
        title="Política de Cookies"
      >
        <p>
          Utilizamos cookies e armazenamento local apenas para funcionalidades essenciais do sistema.
        </p>
        <ul>
          <li>Não utilizamos cookies de rastreamento ou publicidade.</li>
          <li>Podemos usar LocalStorage para salvar suas preferências de uso.</li>
        </ul>
      </Modal>
    </div>
  )
}
