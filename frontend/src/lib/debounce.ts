export function debounce<Args extends unknown[]>(
  callback: (...args: Args) => void,
  delay: number
) {
  let timeout: ReturnType<typeof setTimeout> | undefined

  const cancel = () => {
    clearTimeout(timeout)
    timeout = undefined
  }

  const debounced = (...args: Args) => {
    cancel()
    timeout = setTimeout(() => {
      timeout = undefined
      callback(...args)
    }, delay)
  }

  debounced.cancel = cancel
  return debounced
}
