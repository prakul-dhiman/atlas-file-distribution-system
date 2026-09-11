import { useState, useEffect, useCallback, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { folderApi, fileApi } from '../api/services'
import { useToast } from '../context/ToastContext'
import { formatBytes, formatDateTime, getFileColor, getFileIcon } from '../utils/format'
import Modal from '../components/Modal'
import EmptyState from '../components/EmptyState'
import Spinner, { SkeletonCard } from '../components/Spinner'
import {
  IconFolder, IconFile, IconUpload, IconPlus, IconMore, IconGrid, IconList,
  IconChevronRight, IconDownload, IconTrash, IconEdit, IconMove, IconCopy,
  IconHistory, IconChevronLeft, IconX,
} from '../components/Icons'

// ===== File/Folder icon component =====
function ItemIcon({ item }) {
  if (item.type === 'folder') {
    return <IconFolder className="h-6 w-6 text-amber-500" />
  }
  const color = getFileColor(item.name)
  return <IconFile className={`h-6 w-6 ${color}`} />
}

// ===== Context menu =====
function ContextMenu({ x, y, item, onClose, onAction }) {
  const menuRef = useRef(null)

  useEffect(() => {
    const handleClick = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) onClose()
    }
    const handleEsc = (e) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('mousedown', handleClick)
    document.addEventListener('keydown', handleEsc)
    return () => {
      document.removeEventListener('mousedown', handleClick)
      document.removeEventListener('keydown', handleEsc)
    }
  }, [onClose])

  const menuItems = [
    ...(item.type === 'file' ? [{ label: 'Preview', icon: IconFile, action: () => onAction('preview') }] : []),
    { label: 'Rename', icon: IconEdit, action: () => onAction('rename') },
    { label: 'Move', icon: IconMove, action: () => onAction('move') },
    ...(item.type === 'file'
      ? [
          { label: 'Copy', icon: IconCopy, action: () => onAction('copy') },
          { label: 'Download', icon: IconDownload, action: () => onAction('download') },
          { label: 'Version History', icon: IconHistory, action: () => onAction('versions') },
        ]
      : []),
    { label: 'Delete', icon: IconTrash, action: () => onAction('delete'), danger: true },
  ]

  // Clamp position to viewport
  const menuWidth = 200
  const menuHeight = menuItems.length * 40 + 16
  const left = Math.min(x, window.innerWidth - menuWidth - 8)
  const top = Math.min(y, window.innerHeight - menuHeight - 8)

  return (
    <div
      ref={menuRef}
      className="fixed z-50 w-48 rounded-lg border border-gray-200 bg-white py-1.5 shadow-xl dark:border-gray-700 dark:bg-gray-800"
      style={{ left, top }}
    >
      {menuItems.map((mi) => (
        <button
          key={mi.label}
          onClick={() => {
            onClose()
            mi.action()
          }}
          className={`flex w-full items-center gap-2.5 px-3 py-2 text-left text-sm transition-colors ${
            mi.danger
              ? 'text-red-600 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-900/30'
              : 'text-gray-700 hover:bg-gray-100 dark:text-gray-200 dark:hover:bg-gray-700'
          }`}
        >
          <mi.icon className="h-4 w-4" />
          {mi.label}
        </button>
      ))}
    </div>
  )
}

// ===== Rename modal =====
function RenameModal({ open, onClose, item, onRename }) {
  const [name, setName] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (open && item) setName(item.name)
  }, [open, item])

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!name.trim()) return
    setSaving(true)
    try {
      await onRename(name.trim())
      onClose()
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Rename">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="label">New name</label>
          <input
            type="text"
            className="input"
            value={name}
            onChange={(e) => setName(e.target.value)}
            autoFocus
            required
          />
        </div>
        <div className="flex justify-end gap-2">
          <button type="button" className="btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn-primary" disabled={saving}>
            {saving ? <Spinner size="sm" className="border-white" /> : 'Rename'}
          </button>
        </div>
      </form>
    </Modal>
  )
}

// ===== Move/Copy modal =====
function MoveModal({ open, onClose, item, onMove, onCopy }) {
  const [folders, setFolders] = useState([])
  const [currentFolder, setCurrentFolder] = useState(null)
  const [path, setPath] = useState([])
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)

  const loadFolder = useCallback(async (parentId) => {
    setLoading(true)
    try {
      const resp = await folderApi.list(parentId)
      setFolders(resp.data?.subfolders || [])
      setCurrentFolder(resp.data?.currentFolder || null)
    } catch (e) {
      // ignore
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (open) {
      setPath([])
      loadFolder(null)
    }
  }, [open, loadFolder])

  const navigateTo = (folder) => {
    setPath((p) => [...p, folder])
    loadFolder(folder.id)
  }

  const goBack = () => {
    if (path.length === 0) return
    const newPath = [...path]
    newPath.pop()
    setPath(newPath)
    const parentId = newPath.length > 0 ? newPath[newPath.length - 1].id : null
    loadFolder(parentId)
  }

  const handleConfirm = async () => {
    setSaving(true)
    try {
      const targetFolderId = currentFolder?.id || null
      if (onMove) await onMove(targetFolderId)
      if (onCopy) await onCopy(targetFolderId)
      onClose()
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title={onCopy ? 'Copy to folder' : 'Move to folder'}>
      <div className="space-y-4">
        {/* Breadcrumb */}
        <div className="flex items-center gap-1 text-sm">
          <button
            onClick={() => { setPath([]); loadFolder(null) }}
            className="font-medium text-primary-600 hover:text-primary-700 dark:text-primary-400"
          >
            My Files
          </button>
          {path.map((f, i) => (
            <span key={f.id} className="flex items-center gap-1">
              <IconChevronRight className="h-3.5 w-3.5 text-gray-400" />
              <button
                onClick={() => {
                  const newPath = path.slice(0, i + 1)
                  setPath(newPath)
                  loadFolder(f.id)
                }}
                className="text-gray-600 hover:text-primary-600 dark:text-gray-300"
              >
                {f.name}
              </button>
            </span>
          ))}
        </div>

        {/* Folder list */}
        <div className="max-h-64 overflow-y-auto rounded-lg border border-gray-200 dark:border-gray-700">
          {loading ? (
            <div className="flex justify-center py-8"><Spinner /></div>
          ) : folders.length === 0 ? (
            <p className="py-8 text-center text-sm text-gray-400">No subfolders here</p>
          ) : (
            <ul className="divide-y divide-gray-100 dark:divide-gray-700">
              {folders.map((f) => (
                <li key={f.id}>
                  <button
                    onClick={() => navigateTo(f)}
                    className="flex w-full items-center gap-3 px-4 py-2.5 text-left text-sm text-gray-700 hover:bg-gray-50 dark:text-gray-200 dark:hover:bg-gray-700"
                  >
                    <IconFolder className="h-5 w-5 text-amber-500" />
                    <span className="flex-1 truncate">{f.name}</span>
                    <IconChevronRight className="h-4 w-4 text-gray-400" />
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="flex items-center justify-between">
          <button
            onClick={goBack}
            disabled={path.length === 0}
            className="btn-secondary disabled:opacity-40"
          >
            <IconChevronLeft className="h-4 w-4" /> Back
          </button>
          <div className="flex gap-2">
            <button className="btn-secondary" onClick={onClose}>Cancel</button>
            <button className="btn-primary" onClick={handleConfirm} disabled={saving}>
              {saving ? <Spinner size="sm" className="border-white" /> : onCopy ? 'Copy here' : 'Move here'}
            </button>
          </div>
        </div>
      </div>
    </Modal>
  )
}

// ===== Version history modal =====
function VersionModal({ open, onClose, file, onRestore }) {
  const [versions, setVersions] = useState([])
  const [loading, setLoading] = useState(false)
  const [restoring, setRestoring] = useState(null)

  useEffect(() => {
    if (open && file) {
      setLoading(true)
      fileApi.versions(file.id)
        .then((resp) => setVersions(resp.data || []))
        .catch(() => setVersions([]))
        .finally(() => setLoading(false))
    }
  }, [open, file])

  const handleRestore = async (version) => {
    setRestoring(version)
    try {
      await onRestore(version)
    } finally {
      setRestoring(null)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title={`Version History — ${file?.name || ''}`} maxWidth="max-w-xl">
      {loading ? (
        <div className="flex justify-center py-8"><Spinner /></div>
      ) : versions.length === 0 ? (
        <p className="py-8 text-center text-sm text-gray-400">No versions available</p>
      ) : (
        <div className="max-h-96 space-y-2 overflow-y-auto">
          {versions.map((v) => (
            <div
              key={v.id}
              className="flex items-center justify-between rounded-lg border border-gray-200 px-4 py-3 dark:border-gray-700"
            >
              <div className="min-w-0">
                <p className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">{v.name}</p>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  v{v.version} · {formatBytes(v.sizeBytes)} · {formatDateTime(v.updatedAt)}
                </p>
              </div>
              <button
                onClick={() => handleRestore(v.version)}
                disabled={restoring === v.version}
                className="btn-secondary ml-4 shrink-0"
              >
                {restoring === v.version ? <Spinner size="sm" /> : 'Restore'}
              </button>
            </div>
          ))}
        </div>
      )}
    </Modal>
  )
}

// ===== Upload modal with drag & drop =====
function UploadModal({ open, onClose, folderId, onUploaded }) {
  const [files, setFiles] = useState([])
  const [uploading, setUploading] = useState(false)
  const [progress, setProgress] = useState({})
  const [dragOver, setDragOver] = useState(false)
  const fileInputRef = useRef(null)
  const { success, error } = useToast()

  useEffect(() => {
    if (open) {
      setFiles([])
      setProgress({})
      setUploading(false)
    }
  }, [open])

  const addFiles = (fileList) => {
    const newFiles = Array.from(fileList)
    setFiles((prev) => [...prev, ...newFiles])
  }

  const handleDrop = (e) => {
    e.preventDefault()
    setDragOver(false)
    addFiles(e.dataTransfer.files)
  }

  const handleUpload = async () => {
    if (files.length === 0) return
    setUploading(true)
    let successCount = 0
    for (let i = 0; i < files.length; i++) {
      const file = files[i]
      try {
        await fileApi.upload(file, folderId, (p) => {
          setProgress((prev) => ({ ...prev, [file.name]: p }))
        })
        successCount++
      } catch (e) {
        error(`Failed to upload ${file.name}: ${e.message}`)
      }
    }
    if (successCount > 0) {
      success(`Uploaded ${successCount} file${successCount > 1 ? 's' : ''} successfully`)
      onUploaded()
    }
    setUploading(false)
    onClose()
  }

  return (
    <Modal open={open} onClose={onClose} title="Upload Files" maxWidth="max-w-xl">
      <div className="space-y-4">
        <div
          onDragOver={(e) => { e.preventDefault(); setDragOver(true) }}
          onDragLeave={() => setDragOver(false)}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
          className={`flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed px-6 py-10 text-center transition-colors ${
            dragOver
              ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
              : 'border-gray-300 hover:border-primary-400 dark:border-gray-600'
          }`}
        >
          <IconUpload className="mb-3 h-10 w-10 text-gray-400" />
          <p className="text-sm font-medium text-gray-700 dark:text-gray-200">
            Drag & drop files here, or click to browse
          </p>
          <p className="mt-1 text-xs text-gray-400">Any file type supported</p>
          <input
            ref={fileInputRef}
            type="file"
            multiple
            className="hidden"
            onChange={(e) => addFiles(e.target.files)}
          />
        </div>

        {files.length > 0 && (
          <div className="max-h-48 space-y-2 overflow-y-auto">
            {files.map((f) => (
              <div key={f.name + f.size} className="flex items-center gap-3 rounded-lg border border-gray-200 px-3 py-2 dark:border-gray-700">
                <IconFile className="h-5 w-5 shrink-0 text-gray-400" />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm text-gray-700 dark:text-gray-200">{f.name}</p>
                  <p className="text-xs text-gray-400">{formatBytes(f.size)}</p>
                  {progress[f.name] !== undefined && (
                    <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
                      <div
                        className="h-full rounded-full bg-primary-600 transition-all"
                        style={{ width: `${progress[f.name]}%` }}
                      />
                    </div>
                  )}
                </div>
                <button
                  onClick={() => setFiles((prev) => prev.filter((x) => x !== f))}
                  className="text-gray-400 hover:text-red-500"
                  disabled={uploading}
                >
                  <IconX className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        )}

        <div className="flex justify-end gap-2">
          <button className="btn-secondary" onClick={onClose} disabled={uploading}>Cancel</button>
          <button className="btn-primary" onClick={handleUpload} disabled={uploading || files.length === 0}>
            {uploading ? <Spinner size="sm" className="border-white" /> : `Upload ${files.length > 0 ? `(${files.length})` : ''}`}
          </button>
        </div>
      </div>
    </Modal>
  )
}

// ===== New folder modal =====
function NewFolderModal({ open, onClose, parentId, onCreated }) {
  const [name, setName] = useState('')
  const [saving, setSaving] = useState(false)
  const { success, error } = useToast()

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!name.trim()) return
    setSaving(true)
    try {
      await folderApi.create({ name: name.trim(), parentId: parentId || null })
      success('Folder created')
      onCreated()
      onClose()
    } catch (err) {
      error(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="New Folder">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="label">Folder name</label>
          <input
            type="text"
            className="input"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="My new folder"
            autoFocus
            required
          />
        </div>
        <div className="flex justify-end gap-2">
          <button type="button" className="btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn-primary" disabled={saving}>
            {saving ? <Spinner size="sm" className="border-white" /> : 'Create'}
          </button>
        </div>
      </form>
    </Modal>
  )
}

// ===== Preview Modal =====
function PreviewModal({ open, onClose, file }) {
  const [loading, setLoading] = useState(false)
  const [blobUrl, setBlobUrl] = useState(null)
  const [textContent, setTextContent] = useState(null)
  const { error } = useToast()

  useEffect(() => {
    if (open && file) {
      setLoading(true)
      setBlobUrl(null)
      setTextContent(null)
      fileApi.download(file.id)
        .then((resp) => {
          const mime = file.mimeType || ''
          const blob = new Blob([resp.data], { type: mime })
          if (mime.startsWith('text/') || mime.includes('json') || mime.includes('javascript') || mime.includes('xml')) {
            const reader = new FileReader()
            reader.onload = (e) => setTextContent(e.target.result)
            reader.readAsText(blob)
          } else {
            const url = URL.createObjectURL(blob)
            setBlobUrl(url)
          }
        })
        .catch((err) => error('Failed to load preview: ' + err.message))
        .finally(() => setLoading(false))
    }
  }, [open, file, error])

  const isImage = file?.mimeType?.startsWith('image/')
  const isPdf = file?.mimeType === 'application/pdf'

  return (
    <Modal open={open} onClose={onClose} title={`Preview — ${file?.name || ''}`} maxWidth="max-w-4xl">
      {loading ? (
        <div className="flex justify-center py-12"><Spinner size="lg" /></div>
      ) : isImage && blobUrl ? (
        <div className="flex justify-center p-4">
          <img src={blobUrl} alt={file.name} className="max-h-[70vh] rounded-lg object-contain" />
        </div>
      ) : isPdf && blobUrl ? (
        <iframe src={blobUrl} title={file.name} className="h-[70vh] w-full rounded-lg border border-gray-200 dark:border-gray-700" />
      ) : textContent !== null ? (
        <pre className="max-h-[70vh] overflow-auto rounded-lg bg-gray-50 p-4 font-mono text-xs text-gray-800 dark:bg-gray-800 dark:text-gray-200">
          {textContent}
        </pre>
      ) : (
        <div className="py-12 text-center text-sm text-gray-400">
          No inline preview available for this file type ({file?.mimeType || 'unknown'}).
          <div className="mt-4">
            <button className="btn-primary" onClick={() => fileApi.download(file.id)}>Download File</button>
          </div>
        </div>
      )}
    </Modal>
  )
}

// ===== Main FileBrowser page =====
export default function FileBrowser() {
  const [currentFolder, setCurrentFolder] = useState(null)
  const [subfolders, setSubfolders] = useState([])
  const [files, setFiles] = useState([])
  const [path, setPath] = useState([])
  const [loading, setLoading] = useState(true)
  const [view, setView] = useState('grid')
  const [contextMenu, setContextMenu] = useState(null)
  const [selectedItem, setSelectedItem] = useState(null)
  const [showPreview, setShowPreview] = useState(false)
  const [showRename, setShowRename] = useState(false)
  const [showMove, setShowMove] = useState(false)
  const [showCopy, setShowCopy] = useState(false)
  const [showVersions, setShowVersions] = useState(false)
  const [showUpload, setShowUpload] = useState(false)
  const [showNewFolder, setShowNewFolder] = useState(false)
  const { success, error } = useToast()
  const navigate = useNavigate()
  const location = useLocation()

  const loadFolder = useCallback(async (parentId) => {
    setLoading(true)
    try {
      const resp = await folderApi.list(parentId)
      setCurrentFolder(resp.data?.currentFolder || null)
      setSubfolders(resp.data?.subfolders || [])
      setFiles(resp.data?.files || [])
    } catch (e) {
      error(e.message)
    } finally {
      setLoading(false)
    }
  }, [error])

  useEffect(() => {
    const targetFolderId = location.state?.folderId ?? null
    if (targetFolderId) {
      // Open the folder that contains the searched item
      loadFolder(targetFolderId)
    } else {
      loadFolder(null)
    }
    // Clear state so a refresh doesn't re-apply stale navigation
    if (location.state) {
      window.history.replaceState({}, '')
    }
  }, [loadFolder, location.state])

  const openFolder = (folder) => {
    setPath((p) => [...p, folder])
    loadFolder(folder.id)
  }

  const goBack = () => {
    if (path.length === 0) return
    const newPath = [...path]
    newPath.pop()
    setPath(newPath)
    const parentId = newPath.length > 0 ? newPath[newPath.length - 1].id : null
    loadFolder(parentId)
  }

  const handleContextMenu = (e, item) => {
    e.preventDefault()
    setSelectedItem(item)
    setContextMenu({ x: e.clientX, y: e.clientY })
  }

  const handleAction = (action) => {
    if (!selectedItem) return
    switch (action) {
      case 'preview': setShowPreview(true); break
      case 'rename': setShowRename(true); break
      case 'move': setShowMove(true); break
      case 'copy': setShowCopy(true); break
      case 'download': handleDownload(selectedItem); break
      case 'versions': setShowVersions(true); break
      case 'delete': handleDelete(selectedItem); break
    }
  }

  const handleRename = async (newName) => {
    try {
      if (selectedItem.type === 'folder') {
        await folderApi.rename(selectedItem.id, newName)
      } else {
        await fileApi.rename(selectedItem.id, newName)
      }
      success('Renamed successfully')
      loadFolder(currentFolder?.id || null)
    } catch (e) {
      error(e.message)
    }
  }

  const handleMove = async (targetFolderId) => {
    try {
      if (selectedItem.type === 'folder') {
        await folderApi.move(selectedItem.id, targetFolderId)
      } else {
        await fileApi.move(selectedItem.id, targetFolderId)
      }
      success('Moved successfully')
      loadFolder(currentFolder?.id || null)
    } catch (e) {
      error(e.message)
    }
  }

  const handleCopy = async (targetFolderId) => {
    try {
      await fileApi.copy(selectedItem.id, targetFolderId)
      success('File copied')
      loadFolder(currentFolder?.id || null)
    } catch (e) {
      error(e.message)
    }
  }

  const handleDelete = async (item) => {
    if (!window.confirm(`Move "${item.name}" to recycle bin?`)) return
    try {
      if (item.type === 'folder') {
        await folderApi.delete(item.id)
      } else {
        await fileApi.delete(item.id)
      }
      success('Moved to recycle bin')
      loadFolder(currentFolder?.id || null)
    } catch (e) {
      error(e.message)
    }
  }

  const handleDownload = async (item) => {
    try {
      const resp = await fileApi.download(item.id)
      const url = window.URL.createObjectURL(new Blob([resp.data]))
      const a = document.createElement('a')
      a.href = url
      a.download = item.name
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      a.remove()
      success('Download started')
    } catch (e) {
      error(e.message)
    }
  }

  const handleRestoreVersion = async (version) => {
    try {
      await fileApi.restoreVersion(selectedItem.id, version)
      success('Version restored')
      setShowVersions(false)
      loadFolder(currentFolder?.id || null)
    } catch (e) {
      error(e.message)
    }
  }

  const allItems = [
    ...subfolders.map((f) => ({ ...f, type: 'folder' })),
    ...files.map((f) => ({ ...f, type: 'file' })),
  ]

  const breadcrumbPath = [
    { id: null, name: 'My Files' },
    ...path,
  ]

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">My Files</h1>
          {/* Breadcrumb */}
          <nav className="mt-1 flex items-center gap-1 text-sm">
            {breadcrumbPath.map((p, i) => (
              <span key={p.id || 'root'} className="flex items-center gap-1">
                {i > 0 && <IconChevronRight className="h-3.5 w-3.5 text-gray-400" />}
                {i === breadcrumbPath.length - 1 ? (
                  <span className="font-medium text-gray-700 dark:text-gray-200">{p.name}</span>
                ) : (
                  <button
                    onClick={() => {
                      const newPath = path.slice(0, i)
                      setPath(newPath)
                      const parentId = i > 0 ? path[i - 1].id : null
                      loadFolder(parentId)
                    }}
                    className="text-gray-500 hover:text-primary-600 dark:text-gray-400 dark:hover:text-primary-400"
                  >
                    {p.name}
                  </button>
                )}
              </span>
            ))}
          </nav>
        </div>

        <div className="flex items-center gap-2">
          {/* View toggle */}
          <div className="flex rounded-lg border border-gray-200 bg-white p-0.5 dark:border-gray-700 dark:bg-gray-800">
            <button
              onClick={() => setView('grid')}
              className={`rounded-md p-1.5 ${view === 'grid' ? 'bg-primary-100 text-primary-700 dark:bg-primary-900/40 dark:text-primary-300' : 'text-gray-400 hover:text-gray-600 dark:hover:text-gray-300'}`}
              title="Grid view"
            >
              <IconGrid className="h-4 w-4" />
            </button>
            <button
              onClick={() => setView('list')}
              className={`rounded-md p-1.5 ${view === 'list' ? 'bg-primary-100 text-primary-700 dark:bg-primary-900/40 dark:text-primary-300' : 'text-gray-400 hover:text-gray-600 dark:hover:text-gray-300'}`}
              title="List view"
            >
              <IconList className="h-4 w-4" />
            </button>
          </div>

          <button className="btn-secondary" onClick={() => setShowNewFolder(true)}>
            <IconPlus className="h-4 w-4" /> New Folder
          </button>
          <button className="btn-primary" onClick={() => setShowUpload(true)}>
            <IconUpload className="h-4 w-4" /> Upload
          </button>
        </div>
      </div>

      {/* Back button when in subfolder */}
      {path.length > 0 && (
        <button onClick={goBack} className="btn-secondary">
          <IconChevronLeft className="h-4 w-4" /> Back
        </button>
      )}

      {/* Content */}
      {loading ? (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
          {[...Array(8)].map((_, i) => <SkeletonCard key={i} />)}
        </div>
      ) : allItems.length === 0 ? (
        <div className="card">
          <EmptyState
            icon={<IconFolder className="h-8 w-8" />}
            title="No files yet"
            description="Upload your first file or create a folder to get started."
            action={
              <div className="flex gap-2">
                <button className="btn-primary" onClick={() => setShowUpload(true)}>
                  <IconUpload className="h-4 w-4" /> Upload File
                </button>
                <button className="btn-secondary" onClick={() => setShowNewFolder(true)}>
                  <IconPlus className="h-4 w-4" /> New Folder
                </button>
              </div>
            }
          />
        </div>
      ) : view === 'grid' ? (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
          {allItems.map((item) => (
            <div
              key={`${item.type}-${item.id}`}
              onContextMenu={(e) => handleContextMenu(e, item)}
              onClick={() => item.type === 'folder' && openFolder(item)}
              className="group card cursor-pointer p-4 transition-shadow hover:shadow-card-hover"
            >
              <div className="flex items-start justify-between">
                <ItemIcon item={item} />
                <button
                  onClick={(e) => {
                    e.stopPropagation()
                    handleContextMenu(e, item)
                  }}
                  className="rounded-lg p-1 text-gray-400 opacity-0 transition-opacity hover:bg-gray-100 group-hover:opacity-100 dark:hover:bg-gray-700"
                >
                  <IconMore className="h-4 w-4" />
                </button>
              </div>
              <p className="mt-3 truncate text-sm font-medium text-gray-900 dark:text-gray-100" title={item.name}>
                {item.name}
              </p>
              <p className="mt-0.5 text-xs text-gray-400">
                {item.type === 'folder' ? 'Folder' : formatBytes(item.sizeBytes)}
              </p>
            </div>
          ))}
        </div>
      ) : (
        <div className="card overflow-hidden">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-gray-200 bg-gray-50 text-xs uppercase tracking-wide text-gray-500 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400">
              <tr>
                <th className="px-4 py-3 font-medium">Name</th>
                <th className="hidden px-4 py-3 font-medium sm:table-cell">Type</th>
                <th className="hidden px-4 py-3 font-medium md:table-cell">Size</th>
                <th className="hidden px-4 py-3 font-medium lg:table-cell">Modified</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
              {allItems.map((item) => (
                <tr
                  key={`${item.type}-${item.id}`}
                  onContextMenu={(e) => handleContextMenu(e, item)}
                  onClick={() => item.type === 'folder' && openFolder(item)}
                  className="cursor-pointer transition-colors hover:bg-gray-50 dark:hover:bg-gray-700/50"
                >
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      <ItemIcon item={item} />
                      <span className="truncate font-medium text-gray-900 dark:text-gray-100">{item.name}</span>
                    </div>
                  </td>
                  <td className="hidden px-4 py-3 text-gray-500 dark:text-gray-400 sm:table-cell">
                    {item.type === 'folder' ? 'Folder' : item.mimeType || 'File'}
                  </td>
                  <td className="hidden px-4 py-3 text-gray-500 dark:text-gray-400 md:table-cell">
                    {item.type === 'folder' ? '—' : formatBytes(item.sizeBytes)}
                  </td>
                  <td className="hidden px-4 py-3 text-gray-500 dark:text-gray-400 lg:table-cell">
                    {formatDateTime(item.updatedAt)}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={(e) => {
                        e.stopPropagation()
                        handleContextMenu(e, item)
                      }}
                      className="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700"
                    >
                      <IconMore className="h-4 w-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Context menu */}
      {contextMenu && selectedItem && (
        <ContextMenu
          x={contextMenu.x}
          y={contextMenu.y}
          item={selectedItem}
          onClose={() => setContextMenu(null)}
          onAction={handleAction}
        />
      )}

      {/* Modals */}
      <PreviewModal
        open={showPreview}
        onClose={() => setShowPreview(false)}
        file={selectedItem}
      />
      <RenameModal
        open={showRename}
        onClose={() => setShowRename(false)}
        item={selectedItem}
        onRename={handleRename}
      />
      <MoveModal
        open={showMove}
        onClose={() => setShowMove(false)}
        item={selectedItem}
        onMove={handleMove}
      />
      <MoveModal
        open={showCopy}
        onClose={() => setShowCopy(false)}
        item={selectedItem}
        onCopy={handleCopy}
      />
      <VersionModal
        open={showVersions}
        onClose={() => setShowVersions(false)}
        file={selectedItem}
        onRestore={handleRestoreVersion}
      />
      <UploadModal
        open={showUpload}
        onClose={() => setShowUpload(false)}
        folderId={currentFolder?.id || null}
        onUploaded={() => loadFolder(currentFolder?.id || null)}
      />
      <NewFolderModal
        open={showNewFolder}
        onClose={() => setShowNewFolder(false)}
        parentId={currentFolder?.id || null}
        onCreated={() => loadFolder(currentFolder?.id || null)}
      />
    </div>
  )
}