import { useEffect, useState, useCallback } from 'react'
import { directShareApi } from '../api/services'
import { useToast } from '../context/ToastContext'
import Spinner from '../components/Spinner'
import { IconShare, IconTrash, IconUser } from '../components/Icons'

const PERMISSION_COLORS = {
  VIEWER: 'bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300',
  EDITOR: 'bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300',
  ADMIN: 'bg-purple-100 text-purple-800 dark:bg-purple-900/40 dark:text-purple-300',
}

function ShareWithUserModal({ open, onClose, onCreated }) {
  const [recipientIdentifier, setRecipientIdentifier] = useState('')
  const [fileId, setFileId] = useState('')
  const [folderId, setFolderId] = useState('')
  const [permissionLevel, setPermissionLevel] = useState('VIEWER')
  const [saving, setSaving] = useState(false)
  const { success, error } = useToast()

  if (!open) return null

  const handleShare = async () => {
    if (!recipientIdentifier.trim()) return
    if (!fileId && !folderId) { error('Provide a File ID or Folder ID'); return }
    setSaving(true)
    try {
      await directShareApi.create({
        recipientIdentifier: recipientIdentifier.trim(),
        fileId: fileId ? Number(fileId) : undefined,
        folderId: folderId ? Number(folderId) : undefined,
        permissionLevel,
      })
      success('Item shared successfully')
      onCreated()
      onClose()
    } catch (e) {
      error(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
      <div className="card w-full max-w-md p-6 space-y-4">
        <h2 className="text-lg font-bold text-gray-900 dark:text-gray-100 flex items-center gap-2">
          <IconShare className="h-5 w-5 text-primary-500" /> Share Directly with User
        </h2>

        <div>
          <label className="label">Recipient (username or email)</label>
          <input className="input" placeholder="john@example.com or johndoe" value={recipientIdentifier}
            onChange={(e) => setRecipientIdentifier(e.target.value)} />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="label">File ID (optional)</label>
            <input className="input" type="number" placeholder="e.g. 42" value={fileId}
              onChange={(e) => setFileId(e.target.value)} />
          </div>
          <div>
            <label className="label">Folder ID (optional)</label>
            <input className="input" type="number" placeholder="e.g. 12" value={folderId}
              onChange={(e) => setFolderId(e.target.value)} />
          </div>
        </div>

        <div>
          <label className="label">Permission Level</label>
          <select className="input" value={permissionLevel} onChange={(e) => setPermissionLevel(e.target.value)}>
            <option value="VIEWER">Viewer — Read-only access</option>
            <option value="EDITOR">Editor — Can edit files</option>
            <option value="ADMIN">Admin — Full control</option>
          </select>
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <button className="btn-secondary" onClick={onClose}>Cancel</button>
          <button className="btn-primary" onClick={handleShare} disabled={saving || !recipientIdentifier}>
            {saving ? <Spinner size="sm" className="border-white" /> : 'Share'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default function DirectShares() {
  const [sharedWithMe, setSharedWithMe] = useState([])
  const [loading, setLoading] = useState(true)
  const [showModal, setShowModal] = useState(false)
  const { success, error } = useToast()
  const errorRef = { current: error }

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const resp = await directShareApi.sharedWithMe()
      setSharedWithMe(resp.data || [])
    } catch (e) {
      errorRef.current(e.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  const handleRevoke = async (id) => {
    try {
      await directShareApi.revoke(id)
      success('Share revoked')
      load()
    } catch (e) {
      error(e.message)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Shared With Me</h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Files and folders other users have shared with you directly
          </p>
        </div>
        <button className="btn-primary" onClick={() => setShowModal(true)}>
          <IconShare className="h-4 w-4" /> Share an Item
        </button>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner size="lg" /></div>
      ) : sharedWithMe.length === 0 ? (
        <div className="card p-16 text-center">
          <IconUser className="mx-auto h-12 w-12 text-gray-300 dark:text-gray-600" />
          <h3 className="mt-4 text-base font-semibold text-gray-700 dark:text-gray-300">No items shared with you</h3>
          <p className="mt-1 text-sm text-gray-400">When someone shares a file or folder with you, it appears here.</p>
        </div>
      ) : (
        <div className="card overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100 dark:border-gray-700 bg-gray-50/50 dark:bg-gray-800/50">
                <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-400">Item</th>
                <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-400">Shared By</th>
                <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-400">Permission</th>
                <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-400">Shared On</th>
                <th className="px-4 py-3"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
              {sharedWithMe.map((share) => (
                <tr key={share.id} className="hover:bg-gray-50 dark:hover:bg-gray-800/40 transition-colors">
                  <td className="px-4 py-3 font-medium text-gray-900 dark:text-gray-100">
                    {share.fileName || share.folderName || `#${share.fileId || share.folderId}`}
                    <span className="ml-2 text-xs text-gray-400">{share.fileId ? '📄 File' : '📁 Folder'}</span>
                  </td>
                  <td className="px-4 py-3 text-gray-600 dark:text-gray-300">{share.grantedByUsername}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${PERMISSION_COLORS[share.permissionLevel]}`}>
                      {share.permissionLevel}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-400 text-xs">
                    {new Date(share.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => handleRevoke(share.id)}
                      className="rounded p-1.5 text-gray-400 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-900/30 transition-colors"
                      title="Remove from my shares"
                    >
                      <IconTrash className="h-4 w-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <ShareWithUserModal open={showModal} onClose={() => setShowModal(false)} onCreated={load} />
    </div>
  )
}
