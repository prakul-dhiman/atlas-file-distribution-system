export default function Spinner({ size = 'md', className = '' }) {
  const sizes = {
    sm: 'h-4 w-4 border-2',
    md: 'h-8 w-8 border-2',
    lg: 'h-12 w-12 border-3',
  }
  return (
    <div
      className={`inline-block animate-spin rounded-full border-primary-600 border-t-transparent ${sizes[size]} ${className}`}
      role="status"
      aria-label="Loading"
    />
  )
}

export function Skeleton({ className = '' }) {
  return (
    <div className={`animate-pulse rounded-lg bg-gray-200 dark:bg-gray-700 ${className}`} />
  )
}

export function SkeletonCard() {
  return (
    <div className="card p-4">
      <div className="flex items-center gap-3">
        <Skeleton className="h-10 w-10 rounded-lg" />
        <div className="flex-1 space-y-2">
          <Skeleton className="h-4 w-3/4" />
          <Skeleton className="h-3 w-1/2" />
        </div>
      </div>
    </div>
  )
}

export function PageLoader() {
  return (
    <div className="flex flex-col items-center justify-center py-20 gap-4">
      <Spinner size="lg" />
      <p className="text-sm text-gray-500 dark:text-gray-400">Loading...</p>
    </div>
  )
}