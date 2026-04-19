import { useEffect, useState } from "react"

interface Contributor {
  id: number
  login: string
  avatar_url: string
  html_url: string
}

export default function Contributors() {
  const [contributors, setContributors] = useState<Contributor[]>([])

  useEffect(() => {
    fetch("https://api.github.com/repos/Navelogic/escreveaqui/contributors")
      .then((res) => res.json())
      .then((data) => {
        if (Array.isArray(data)) setContributors(data)
      })
      .catch((err) => console.error("Falha ao buscar contribuidores", err))
  }, [])

  if (contributors.length === 0) return null

  return (
    <div className="flex flex-wrap items-center gap-2">
      <span className="text-xs text-muted-foreground">Contribuidores</span>
      {contributors.map((contributor) => (
        <a
          key={contributor.id}
          href={contributor.html_url}
          target="_blank"
          rel="noopener noreferrer"
          title={contributor.login}
          className="transition-transform hover:scale-110"
        >
          <img
            src={contributor.avatar_url}
            alt={contributor.login}
            className="w-8 h-8 rounded-full"
          />
        </a>
      ))}
    </div>
  )
}
