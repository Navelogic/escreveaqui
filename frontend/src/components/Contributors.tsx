import { useEffect, useState } from "react"
import { Tooltip, TooltipContent, TooltipTrigger } from "./ui/tooltip"
import { Avatar, AvatarFallback, AvatarGroup, AvatarGroupCount, AvatarImage } from "./ui/avatar"

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

interface ContributorsCountCache {
  count: number | null 
  expiresAt: number
}

const CONTRIBUTORS_GITHUB_GRAPH_URL = "https://github.com/Navelogic/escreveaqui/graphs/contributors"
const CONTRIBUTORS_API_URL = "https://api.github.com/repos/Navelogic/escreveaqui/contributors"
const CONTRIBUTORS_CACHE_KEY = "escreveaqui:contributors"
const CONTRIBUTORS_COUNT_CACHE_KEY = "escreveaqui:contributors-count"
const CONTRIBUTORS_CACHE_TTL_MS = 60 * 60 * 1000
const CONTRIBUTORS_SLICE_COUNT = 8

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

async function getContributorsCount() {
  const res = await fetch(
    `${CONTRIBUTORS_API_URL}?per_page=1&anon=true`,
  )

  if (!res.ok) throw new Error(`GitHub respondeu com status ${res.status}`)

  const link = res.headers.get("Link")

  if (!link) {
    const data = await res.json()
    return Array.isArray(data) ? data.length : 0
  }

  const lastPageLink = link
    .split(",")
    .find((part) => part.includes('rel="last"'))

  const lastPageUrl = lastPageLink?.match(/<([^>]+)>/)?.[1]
  const lastPage = lastPageUrl
    ? new URL(lastPageUrl).searchParams.get("page")
    : null

  return lastPage ? Number(lastPage) : 0
}


function getCachedContributorsCount(): number | null {
  try {
    const cachedValue = localStorage.getItem(CONTRIBUTORS_COUNT_CACHE_KEY)
    if (!cachedValue) return null

    const cache: ContributorsCountCache = JSON.parse(cachedValue)
    if (cache.expiresAt <= Date.now() || !Number.isInteger(cache.count)) {
      localStorage.removeItem(CONTRIBUTORS_COUNT_CACHE_KEY)
      return null
    }

    return cache.count
  } catch {
    return null
  }
}

function cacheContributorsCount(count: number | null) {
  try {
    const cache: ContributorsCountCache = {
      count,
      expiresAt: Date.now() + CONTRIBUTORS_CACHE_TTL_MS,
    }

    localStorage.setItem(CONTRIBUTORS_COUNT_CACHE_KEY, JSON.stringify(cache))
  } catch {
    // A falha no cache não deve impedir a exibição do número de contribuidores.
  }
}


export default function Contributors() {
  const [contributors, setContributors] = useState<Contributor[]>(
    () => getCachedContributors() ?? [],
  )
  const [contributorsCount, setContributorsCount] = useState<number | null>(
    () => getCachedContributorsCount()
  )

  useEffect(() => {
    if (getCachedContributors() && getCachedContributorsCount()) return

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

    getContributorsCount()
      .then((data: unknown) => {
        if (!Number.isInteger(data)) return

        const fetchedCount = data as number
        const slicedCount = Math.max(0, fetchedCount - CONTRIBUTORS_SLICE_COUNT)
        cacheContributorsCount(slicedCount)
        setContributorsCount(slicedCount)
      })
      .catch((err: unknown) => console.error("Falha ao buscar número de contribuidores", err))

  }, [])

  if (contributors.length === 0) return null

  return (
    <div className="flex flex-wrap items-center gap-2">
      <span className="text-xs text-muted-foreground">Contribuidores</span>
      <AvatarGroup>
        {contributors.slice(0, CONTRIBUTORS_SLICE_COUNT).map((contributor) => (
          <Tooltip key={contributor.id}>
            <TooltipTrigger
              render={
                <a
                  href={contributor.html_url}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label={`Abrir perfil de ${contributor.login} no GitHub`}
                  className="relative transition-transform hover:z-10 hover:scale-110 focus-visible:z-10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                />
              }
            >
              <Avatar>
                <AvatarImage src={contributor.avatar_url} alt={contributor.login} />
                <AvatarFallback>
                  {contributor.login.slice(0, 2).toUpperCase()}
                </AvatarFallback>
              </Avatar>
            </TooltipTrigger>
            <TooltipContent>{contributor.login}</TooltipContent>
          </Tooltip>
        ))}
        {contributorsCount !== 0 ? (
          <Tooltip>
            <TooltipTrigger
              render={
                <a
                  href={CONTRIBUTORS_GITHUB_GRAPH_URL}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label="Abrir painel de contribuidores no GitHub"
                  className="relative transition-transform hover:z-10 hover:scale-110 focus-visible:z-10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                />
              }
            >
              <AvatarGroupCount>
                +{contributorsCount}
              </AvatarGroupCount>
            </TooltipTrigger>
            <TooltipContent>
              ver mais contribuidores
            </TooltipContent>
          </Tooltip>
        ): null}
      </AvatarGroup>
    </div>
  )
}
