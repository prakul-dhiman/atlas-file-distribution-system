export async function shareOrCopy({ title, text, url, onCopy }) {
  if (typeof navigator !== 'undefined' && navigator.share) {
    try {
      await navigator.share({ title, text, url })
      return { shared: true }
    } catch {
      if (onCopy) onCopy()
      return { copied: true }
    }
  }
  if (onCopy) onCopy()
  return { copied: true }
}
