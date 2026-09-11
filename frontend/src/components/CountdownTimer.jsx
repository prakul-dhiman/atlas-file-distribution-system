import { useState, useEffect } from 'react'
import { IconClock } from './Icons'

export default function CountdownTimer({ targetDate, className = '' }) {
  const [timeLeft, setTimeLeft] = useState('')
  const [isExpired, setIsExpired] = useState(false)

  useEffect(() => {
    if (!targetDate) return

    const updateTimer = () => {
      const target = new Date(targetDate).getTime()
      const now = new Date().getTime()
      const diff = target - now

      if (diff <= 0) {
        setTimeLeft('Expired')
        setIsExpired(true)
        return
      }

      const days = Math.floor(diff / (1000 * 60 * 60 * 24))
      const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
      const seconds = Math.floor((diff % (1000 * 60)) / 1000)

      let str = ''
      if (days > 0) str += `${days}d `
      if (hours > 0 || days > 0) str += `${hours}h `
      str += `${minutes}m ${seconds}s`

      setTimeLeft(str)
      setIsExpired(false)
    }

    updateTimer()
    const interval = setInterval(updateTimer, 1000)
    return () => clearInterval(interval)
  }, [targetDate])

  if (!targetDate) {
    return <span className={`text-xs text-gray-400 ${className}`}>Never expires</span>
  }

  return (
    <span
      className={`inline-flex items-center gap-1 text-xs font-mono font-medium ${
        isExpired
          ? 'text-red-600 dark:text-red-400'
          : 'text-amber-600 dark:text-amber-400'
      } ${className}`}
    >
      <IconClock className="h-3.5 w-3.5 shrink-0" />
      {isExpired ? 'Link Expired' : `${timeLeft} left`}
    </span>
  )
}
