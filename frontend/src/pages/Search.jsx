import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { searchApi } from '../api/services'
import { useToast } from '../context/ToastContext'
import { formatBytes, formatDateTime, getFileColor } from '../utils/format'
import EmptyState from '../components/EmptyState'
import Spinner from '../components/Spinner'
import { IconSearch, IconFolder, IconFile, IconX } from '../components/Icons'

export default function Search() {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState(null)
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)
  const debounceRef = useRef(null)
  const { error } = useToast()
  const navigate = useNavigate()

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current)
    if (!query.trim()) {
      setResults(null)
      setSearched(false)
      return
    }
    debounceRef.current = setTimeout(async () => {
      setLoading(true)
      try {
        const resp = await searchApi.search(query.trim())
        setResults(resp.data)
        setSearched(true)
      } catch (e) {
        error(e.message)
      } finally {
        setLoading(false)
      }
    }, 400)
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current)
    }
  }, [query, error])

  const folders = results?.folders || []
  const files = results?.files || []
  const total = results?.totalResults || 0

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Search</h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Find files and folders across your storage
        </p>
      </div>

      {/* Search input */}
      <div className="relative">
        <IconSearch className="pointer-events-none absolute left-3.5 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
        <input
          type="text"
          className="input py-3 pl-11 pr-10 text-base"
          placeholder="Search by name..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          autoFocus
        />
        {query && (
          <button
            onClick={() => setQuery('')}
            className="absolute right-3 top-1/2 -translate-y-1/2 rounded-lg p-1 text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            <IconX className="h-4 w-4" />
          </button>
        )}
      </div>

      {/* Results */}
      {loading ? (
        <div className="flex justify-center py-16"><Spinner size="lg" /></div>
      ) : searched && total === 0 ? (
        <div className="card">
          <EmptyState
            icon={<IconSearch className="h-8 w-8" />}
            title="No results found"
            description={`No files or folders match "${query}". Try a different search term.`}
          />
        </div>
      ) : results ? (
        <div className="space-y-6">
          {total > 0 && (
            <p className="text-sm text-gray-500 dark:text-gray-400">
              {total} result{total !== 1 ? 's' : ''} for "{query}"
            </p>
          )}

          {folders.length > 0 && (
            <div>
              <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400">
                Folders
              </h2>
              <div className="card divide-y divide-gray-100 dark:divide-gray-700">
                {folders.map((f) => (
                  <button
                    key={f.id}
                    onClick={() => navigate('/', { state: { folderId: f.parentId } })}
                    className="flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-gray-50 dark:hover:bg-gray-700/50"
                  >
                    <IconFolder className="h-5 w-5 shrink-0 text-amber-500" />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">{f.name}</p>
                      <p className="text-xs text-gray-400">Folder · {formatDateTime(f.updatedAt)}</p>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          )}

          {files.length > 0 && (
            <div>
              <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400">
                Files
              </h2>
              <div className="card divide-y divide-gray-100 dark:divide-gray-700">
                {files.map((f) => (
                  <button
                    key={f.id}
                    onClick={() => navigate('/', { state: { folderId: f.parentFolderId || f.folderId || null } })}
                    className="flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-gray-50 dark:hover:bg-gray-700/50"
                  >
                    <IconFile className={`h-5 w-5 shrink-0 ${getFileColor(f.name)}`} />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">{f.name}</p>
                      <p className="text-xs text-gray-400">
                        {formatBytes(f.sizeBytes)} · {formatDateTime(f.updatedAt)}
                      </p>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      ) : (
        <div className="card">
          <EmptyState
            icon={<IconSearch className="h-8 w-8" />}
            title="Search your storage"
            description="Type a name to search across all your files and folders."
          />
        </div>
      )}
    </div>
  )
}