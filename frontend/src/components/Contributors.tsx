import { useEffect, useState } from "react"

interface Contributor {
  id: number
  login: string
  avatar_url: string
  html_url: string
}

interface ContributorsCache {
  contributors: Contributor[]
  expiresAt: number
}

const CONTRIBUTORS_API_URL =
  "https://api.github.com/repos/Navelogic/escreveaqui/contributors"
const CONTRIBUTORS_CACHE_KEY = "escreveaqui:contributors"
const CONTRIBUTORS_CACHE_TTL_MS = 60 * 60 * 1000

function getCachedContributors(): Contributor[] | null {
  try {
    const cachedValue = localStorage.getItem(CONTRIBUTORS_CACHE_KEY)
    if (!cachedValue) return null

    const cache: ContributorsCache = JSON.parse(cachedValue)
    if (cache.expiresAt <= Date.now() || !Array.isArray(cache.contributors)) {
      localStorage.removeItem(CONTRIBUTORS_CACHE_KEY)
      return null
    }

    return cache.contributors
  } catch {
    return null
  }
}

function cacheContributors(contributors: Contributor[]) {
  try {
    const cache: ContributorsCache = {
      contributors,
      expiresAt: Date.now() + CONTRIBUTORS_CACHE_TTL_MS,
    }

    localStorage.setItem(CONTRIBUTORS_CACHE_KEY, JSON.stringify(cache))
  } catch {
    // A falha no cache não deve impedir a exibição dos contribuidores.
  }
}

export default function Contributors() {
  const [contributors, setContributors] = useState<Contributor[]>(
    () => getCachedContributors() ?? [],
  )

  useEffect(() => {
    if (getCachedContributors()) return

    fetch(CONTRIBUTORS_API_URL)
      .then((res) => {
        if (!res.ok) throw new Error(`GitHub respondeu com status ${res.status}`)
        return res.json()
      })
      .then((data: unknown) => {
        if (!Array.isArray(data)) return

        const fetchedContributors = data as Contributor[]
        cacheContributors(fetchedContributors)
        setContributors(fetchedContributors)
      })
      .catch((err: unknown) =>
        console.error("Falha ao buscar contribuidores", err),
      )
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
