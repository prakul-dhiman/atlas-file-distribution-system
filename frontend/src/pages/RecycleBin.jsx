import { useState, useEffect, useCallback } from 'react'
import { recycleBinApi } from '../api/services'
import { useToast } from '../context/ToastContext'
import { formatBytes, formatDateTime, getFileColor } from '../utils/format'
import EmptyState from '../components/EmptyState'
import Spinner from '../components/Spinner'
import {
  IconTrash, IconFolder, IconFile, IconRefresh, IconX,
} from '../components/Icons'

export default function RecycleBin() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(null) // tracks the item id being acted on
  const { success, error } = useToast()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const resp = await recycleBinApi.list()
      setData(resp.data)
    } catch (e) {
      error(e.message)
    } finally {
      setLoading(false)
    }
  }, [error])

  useEffect(() => {
    load()
  }, [load])

  const handleRestore = async (item) => {
    setBusy(item.id)
    try {
      if (item.type === 'folder') {
        await recycleBinApi.restoreFolder(item.id)
      } else {
        await recycleBinApi.restoreFile(item.id)
      }
      success(`Restored "${item.name}"`)
      load()
    } catch (e) {
      error(e.message)
    } finally {
      setBusy(null)
    }
  }

  const handlePurge = async (item) => {
    if (!window.confirm(`Permanently delete "${item.name}"? This cannot be undone.`)) return
    setBusy(item.id)
    try {
      if (item.type === 'folder') {
        await recycleBinApi.purgeFolder(item.id)
      } else {
        await recycleBinApi.purgeFile(item.id)
      }
      success(`Purged "${item.name}" permanently`)
      load()
    } catch (e) {
      error(e.message)
    } finally {
      setBusy(null)
    }
  }

  const handleEmpty = async () => {
    if (!window.confirm('Permanently delete ALL items in the recycle bin? This cannot be undone.')) return
    try {
      await recycleBinApi.empty()
      success('Recycle bin emptied')
      load()
    } catch (e) {
      error(e.message)
    }
  }

  const folders = data?.trashedFolders || []
  const files = data?.trashedFiles || []
  const total = data?.totalItems || 0

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Recycle Bin</h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Deleted items are kept for 30 days before being permanently removed
          </p>
        </div>
        {total > 0 && (
          <button className="btn-danger" onClick={handleEmpty}>
            <IconTrash className="h-4 w-4" /> Empty Trash
          </button>
        )}
      </div>

      {loading ? (
        <div className="flex justify-center py-16"><Spinner size="lg" /></div>
      ) : total === 0 ? (
        <div className="card">
          <EmptyState
            icon={<IconTrash className="h-8 w-8" />}
            title="Recycle bin is empty"
            description="Files and folders you delete will appear here for 30 days."
          />
        </div>
      ) : (
        <div className="card divide-y divide-gray-100 dark:divide-gray-700">
          {folders.map((f) => (
            <div key={`folder-${f.id}`} className="flex items-center gap-3 px-4 py-3">
              <IconFolder className="h-5 w-5 shrink-0 text-amber-500" />
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">{f.name}</p>
                <p className="text-xs text-gray-400">
                  Folder · Deleted {formatDateTime(f.updatedAt)}
                </p>
              </div>
              <div className="flex shrink-0 items-center gap-1">
                <button
                  onClick={() => handleRestore({ ...f, type: 'folder' })}
                  disabled={busy === f.id}
                  className="btn-secondary"
                >
                  {busy === f.id ? <Spinner size="sm" /> : <IconRefresh className="h-4 w-4" />} Restore
                </button>
                <button
                  onClick={() => handlePurge({ ...f, type: 'folder' })}
                  disabled={busy === f.id}
                  className="btn-secondary text-red-600 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-900/30"
                >
                  <IconX className="h-4 w-4" /> Purge
                </button>
              </div>
            </div>
          ))}

          {files.map((f) => (
            <div key={`file-${f.id}`} className="flex items-center gap-3 px-4 py-3">
              <IconFile className={`h-5 w-5 shrink-0 ${getFileColor(f.name)}`} />
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">{f.name}</p>
                <p className="text-xs text-gray-400">
                  {formatBytes(f.sizeBytes)} · Deleted {formatDateTime(f.updatedAt)}
                </p>
              </div>
              <div className="flex shrink-0 items-center gap-1">
                <button
                  onClick={() => handleRestore({ ...f, type: 'file' })}
                  disabled={busy === f.id}
                  className="btn-secondary"
                >
                  {busy === f.id ? <Spinner size="sm" /> : <IconRefresh className="h-4 w-4" />} Restore
                </button>
                <button
                  onClick={() => handlePurge({ ...f, type: 'file' })}
                  disabled={busy === f.id}
                  className="btn-secondary text-red-600 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-900/30"
                >
                  <IconX className="h-4 w-4" /> Purge
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}