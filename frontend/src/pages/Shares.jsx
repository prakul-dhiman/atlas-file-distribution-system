import { useState, useEffect, useCallback, useRef } from 'react'
import { shareApi, folderApi } from '../api/services'
import { useToast } from '../context/ToastContext'
import Modal from '../components/Modal'
import EmptyState from '../components/EmptyState'
import Spinner from '../components/Spinner'
import {
  IconShare, IconPlus, IconCopy, IconTrash, IconFolder, IconFile,
  IconChevronRight, IconChevronLeft, IconLock, IconClock, IconCheck, IconDownload,
} from '../components/Icons'
import { formatDate, formatDateTime, getFileColor } from '../utils/format'
import { shareOrCopy } from '../utils/share'

import CountdownTimer from '../components/CountdownTimer'

function QrModal({ open, onClose, share }) {
  return (
    <Modal open={open} onClose={onClose} title="QR Code" maxWidth="max-w-sm">
      <div className="flex flex-col items-center gap-4 py-2">
        <p className="text-sm text-gray-500 dark:text-gray-400 text-center">
          Scan to open <span className="font-medium text-gray-700 dark:text-gray-200">{share?.name}</span>
        </p>
        {share?.qrCodeUrl && (
          <img
            src={share.qrCodeUrl}
            alt="QR Code"
            width={200}
            height={200}
            className="rounded-lg border border-gray-200 dark:border-gray-700"
          />
        )}
        <p className="text-xs text-gray-400 break-all text-center max-w-xs">{share?.shareUrl}</p>
      </div>
    </Modal>
  )
}

function AnalyticsModal({ open, onClose, share }) {
  return (
    <Modal open={open} onClose={onClose} title="Download Analytics" maxWidth="max-w-lg">
      <div className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <div className="rounded-lg bg-gray-50 dark:bg-gray-800 p-3">
            <p className="text-xs text-gray-500 dark:text-gray-400">Total Downloads</p>
            <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{share?.accessCount ?? 0}</p>
          </div>
          <div className="rounded-lg bg-gray-50 dark:bg-gray-800 p-3">
            <p className="text-xs text-gray-500 dark:text-gray-400">Last Accessed</p>
            <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
              {share?.lastAccessedAt ? formatDateTime(share.lastAccessedAt) : 'Never'}
            </p>
          </div>
        </div>
        <p className="text-sm font-medium text-gray-700 dark:text-gray-300">Recent Downloads</p>
        {share?.recentDownloads?.length > 0 ? (
          <ul className="divide-y divide-gray-100 dark:divide-gray-700 rounded-lg border border-gray-200 dark:border-gray-700 max-h-48 overflow-y-auto">
            {share.recentDownloads.map((d, i) => (
              <li key={i} className="px-3 py-2 text-xs flex justify-between gap-2">
                <span className="font-mono text-gray-600 dark:text-gray-400 shrink-0">{d.ipAddress || 'Unknown'}</span>
                <span className="truncate text-gray-400 flex-1 text-center">{d.userAgent?.split('/')[0]}</span>
                <span className="shrink-0 text-gray-400">{formatDateTime(d.downloadedAt)}</span>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-sm text-gray-400 py-4 text-center">No downloads yet</p>
        )}
      </div>
    </Modal>
  )
}

function CreateShareModal({ open, onClose, onCreated }) {
  const [items, setItems] = useState([])
  const [path, setPath] = useState([])
  const [loading, setLoading] = useState(false)
  const [selected, setSelected] = useState(null)
  const [password, setPassword] = useState('')
  const [expiryMode, setExpiryMode] = useState('7days') // 7days, 1day, 30days, custom, never
  const [customDateTime, setCustomDateTime] = useState('')
  const [neverExpire, setNeverExpire] = useState(false)
  const [expireAfterFirst, setExpireAfterFirst] = useState(false)
  const [requireEmailOtp, setRequireEmailOtp] = useState(false)
  const [maxAccessCount, setMaxAccessCount] = useState('')
  const [saving, setSaving] = useState(false)
  const { success, error } = useToast()

  const loadFolder = useCallback(async (parentId) => {
    setLoading(true)
    try {
      const resp = await folderApi.list(parentId)
      const folders = (resp.data?.subfolders || []).map((f) => ({ ...f, type: 'folder' }))
      const files = (resp.data?.files || []).map((f) => ({ ...f, type: 'file' }))
      setItems([...folders, ...files])
    } catch (e) {
      error(e.message)
    } finally {
      setLoading(false)
    }
  }, [error])

  useEffect(() => {
    if (open) {
      setPath([]); setSelected(null); setPassword('')
      setExpiryMode('7days'); setCustomDateTime(''); setNeverExpire(false)
      setExpireAfterFirst(false); setRequireEmailOtp(false); setMaxAccessCount('')
      loadFolder(null)
    }
  }, [open, loadFolder])

  const navigateTo = (folder) => { setPath((p) => [...p, folder]); loadFolder(folder.id) }
  const goBack = () => {
    if (!path.length) return
    const newPath = [...path]; newPath.pop(); setPath(newPath)
    loadFolder(newPath.length ? newPath[newPath.length - 1].id : null)
  }

  const handleCreate = async () => {
    if (!selected) return
    setSaving(true)
    try {
      let expiresAfterDays = 7
      let customExpiresAt = null
      let isNever = neverExpire || expiryMode === 'never'

      if (!isNever) {
        if (expiryMode === '1day') expiresAfterDays = 1
        else if (expiryMode === '7days') expiresAfterDays = 7
        else if (expiryMode === '30days') expiresAfterDays = 30
        else if (expiryMode === 'custom' && customDateTime) {
          customExpiresAt = new Date(customDateTime).toISOString()
        }
      }

      await shareApi.create({
        ...(selected.type === 'file' ? { fileId: selected.id } : { folderId: selected.id }),
        ...(password ? { password } : {}),
        neverExpire: isNever,
        ...(customExpiresAt ? { customExpiresAt } : { expiresAfterDays }),
        expireAfterFirstDownload: expireAfterFirst,
        requireEmailOtp,
        ...(maxAccessCount && !expireAfterFirst ? { maxAccessCount: Number(maxAccessCount) } : {}),
      })
      success('Share link created successfully'); onCreated(); onClose()
    } catch (e) {
      error(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Create Share Link" maxWidth="max-w-xl">
      <div className="space-y-4">
        <div className="flex items-center gap-1 text-sm">
          <button onClick={() => { setPath([]); loadFolder(null) }} className="font-medium text-primary-600 hover:text-primary-700 dark:text-primary-400">My Files</button>
          {path.map((f, i) => (
            <span key={f.id} className="flex items-center gap-1">
              <IconChevronRight className="h-3.5 w-3.5 text-gray-400" />
              <button onClick={() => { const p = path.slice(0, i + 1); setPath(p); loadFolder(f.id) }} className="text-gray-600 hover:text-primary-600 dark:text-gray-300">{f.name}</button>
            </span>
          ))}
        </div>

        <div className="max-h-48 overflow-y-auto rounded-lg border border-gray-200 dark:border-gray-700">
          {loading ? (
            <div className="flex justify-center py-8"><Spinner /></div>
          ) : items.length === 0 ? (
            <p className="py-8 text-center text-sm text-gray-400">No items here — go back or create files first</p>
          ) : (
            <ul className="divide-y divide-gray-100 dark:divide-gray-700">
              {items.map((item) => {
                const isFolder = item.type === 'folder'
                const isSelected = selected?.id === item.id && selected?.type === item.type
                return (
                  <li key={`${item.type}-${item.id}`}>
                    <button
                      onClick={() => setSelected(item)}
                      className={`flex w-full items-center gap-3 px-4 py-2.5 text-left text-sm transition-colors ${isSelected ? 'bg-primary-50 dark:bg-primary-900/30' : 'hover:bg-gray-50 dark:hover:bg-gray-700'}`}
                    >
                      {isFolder ? <IconFolder className="h-5 w-5 text-amber-500" /> : <IconFile className={`h-5 w-5 ${getFileColor(item.name)}`} />}
                      <span className="flex-1 truncate text-gray-700 dark:text-gray-200">{item.name}</span>
                      {isFolder ? (
                        <button onClick={(e) => { e.stopPropagation(); navigateTo(item) }} className="rounded p-1 text-gray-400 hover:text-gray-600">
                          <IconChevronRight className="h-4 w-4" />
                        </button>
                      ) : (
                        <span className="text-xs text-gray-400">{item.sizeBytes ? `${(item.sizeBytes / 1024).toFixed(1)} KB` : ''}</span>
                      )}
                    </button>
                  </li>
                )
              })}
            </ul>
          )}
        </div>

        {selected && (
          <div className="rounded-lg border border-primary-200 bg-primary-50 px-4 py-2.5 text-sm text-primary-700 dark:border-primary-800 dark:bg-primary-900/20 dark:text-primary-300">
            Selected: <span className="font-medium">{selected.name}</span>
          </div>
        )}

        <div className="space-y-4">
          <div>
            <label className="label">Password Protection (optional)</label>
            <input
              type="password"
              className="input"
              placeholder="Protect link with a password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="label">Expiration Schedule</label>
              <select
                className="input"
                value={expiryMode}
                onChange={(e) => {
                  setExpiryMode(e.target.value)
                  if (e.target.value === 'never') setNeverExpire(true)
                  else setNeverExpire(false)
                }}
              >
                <option value="1day">Expire after 1 day</option>
                <option value="7days">Expire after 7 days (Default)</option>
                <option value="30days">Expire after 30 days</option>
                <option value="custom">Custom Date & Time</option>
                <option value="never">Never Expire</option>
              </select>
            </div>

            {expiryMode === 'custom' && (
              <div>
                <label className="label">Select Expiration Date & Time</label>
                <input
                  type="datetime-local"
                  className="input"
                  value={customDateTime}
                  onChange={(e) => setCustomDateTime(e.target.value)}
                />
              </div>
            )}
          </div>

          <div className="space-y-2 border-t border-gray-100 pt-3 dark:border-gray-700">
            <label className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300 cursor-pointer">
              <input
                type="checkbox"
                className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                checked={expireAfterFirst}
                onChange={(e) => setExpireAfterFirst(e.target.checked)}
              />
              <span className="font-medium">Expire after first download (Single-use link)</span>
            </label>

            <label className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300 cursor-pointer">
              <input
                type="checkbox"
                className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                checked={requireEmailOtp}
                onChange={(e) => setRequireEmailOtp(e.target.checked)}
              />
              <span className="font-medium">Require Email OTP Verification (6-digit code via email)</span>
            </label>

            {!expireAfterFirst && (
              <div>
                <label className="label">Max Access Limit (optional)</label>
                <input
                  type="number"
                  className="input"
                  min={1}
                  placeholder="Unlimited downloads"
                  value={maxAccessCount}
                  onChange={(e) => setMaxAccessCount(e.target.value)}
                />
              </div>
            )}
          </div>
        </div>

        <div className="flex items-center justify-between pt-2">
          <button onClick={goBack} disabled={!path.length} className="btn-secondary disabled:opacity-40">
            <IconChevronLeft className="h-4 w-4" /> Back
          </button>
          <div className="flex gap-2">
            <button className="btn-secondary" onClick={onClose}>Cancel</button>
            <button className="btn-primary" onClick={handleCreate} disabled={saving || !selected}>
              {saving ? <Spinner size="sm" className="border-white" /> : 'Create Link'}
            </button>
          </div>
        </div>
      </div>
    </Modal>
  )
}

export default function Shares() {
  const [shares, setShares] = useState([])
  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)
  const [copiedId, setCopiedId] = useState(null)
  const [qrShare, setQrShare] = useState(null)
  const [analyticsShare, setAnalyticsShare] = useState(null)
  const { success, error } = useToast()
  const errorRef = useRef(error)
  errorRef.current = error

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const resp = await shareApi.list()
      setShares(resp.data || [])
    } catch (e) {
      errorRef.current(e.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  const handleCopy = async (share) => {
    try {
      await navigator.clipboard.writeText(share.shareUrl)
      setCopiedId(share.id); setTimeout(() => setCopiedId(null), 2000)
      success('Share link copied to clipboard')
    } catch { error('Could not copy link') }
  }

  const handleShare = async (share) => {
    const result = await shareOrCopy({ title: `Share "${share.name}"`, text: `Check out "${share.name}" on Project Atlas`, url: share.shareUrl, onCopy: () => handleCopy(share) })
    if (result?.copied) { setCopiedId(share.id); setTimeout(() => setCopiedId(null), 2000); success('Share link copied to clipboard') }
  }

  const handleRevoke = async (share) => {
    if (!window.confirm(`Revoke share "${share.name}"? The link will stop working immediately.`)) return
    try { await shareApi.revoke(share.id); success('Share revoked'); load() }
    catch (e) { error(e.message) }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Shared Links</h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">Share files and folders with custom expiration & password protection</p>
        </div>
        <button className="btn-primary" onClick={() => setShowCreate(true)}><IconPlus className="h-4 w-4" /> New Share</button>
      </div>

      {loading ? (
        <div className="flex justify-center py-16"><Spinner size="lg" /></div>
      ) : shares.length === 0 ? (
        <div className="card">
          <EmptyState
            icon={<IconShare className="h-8 w-8" />}
            title="No shares yet"
            description="Create a share link to let anyone download your files or folders."
            action={<button className="btn-primary" onClick={() => setShowCreate(true)}><IconPlus className="h-4 w-4" /> Create Your First Share</button>}
          />
        </div>
      ) : (
        <div className="card divide-y divide-gray-100 dark:divide-gray-700">
          {shares.map((share) => (
            <div key={share.id} className="flex flex-col gap-3 px-4 py-4 sm:flex-row sm:items-center">
              <div className="flex min-w-0 flex-1 items-center gap-3">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary-50 text-primary-600 dark:bg-primary-900/30 dark:text-primary-400">
                  {share.fileId ? <IconFile className="h-5 w-5" /> : <IconFolder className="h-5 w-5 text-amber-500" />}
                </div>
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">{share.name}</p>
                  <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-gray-400">
                    <span>{share.fileId ? 'File' : 'Folder'}</span>
                    {share.hasPassword && <span className="flex items-center gap-1"><IconLock className="h-3 w-3" /> Password</span>}
                    <CountdownTimer targetDate={share.expiresAt} />
                    <span>{share.accessCount ?? 0}{share.maxAccessCount ? ` / ${share.maxAccessCount}` : ''} downloads</span>
                    {share.expireAfterFirstDownload && <span className="text-amber-500 font-medium">Single-Use</span>}
                  </div>
                </div>
              </div>
              <div className="flex shrink-0 flex-wrap items-center gap-2">
                <button className="btn-secondary" onClick={() => setQrShare(share)} title="Show QR code">QR</button>
                <button className="btn-secondary" onClick={() => setAnalyticsShare(share)} title="Download analytics">
                  <IconDownload className="h-4 w-4" /> Stats
                </button>
                <button className="btn-secondary" onClick={() => handleShare(share)}><IconShare className="h-4 w-4" /> Share</button>
                <button className="btn-secondary" onClick={() => handleCopy(share)}>
                  {copiedId === share.id ? <IconCheck className="h-4 w-4 text-green-500" /> : <IconCopy className="h-4 w-4" />}
                  {copiedId === share.id ? 'Copied' : 'Copy'}
                </button>
                {share.isActive && (
                  <button onClick={() => handleRevoke(share)} className="btn-secondary text-red-600 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-900/30">
                    <IconTrash className="h-4 w-4" /> Revoke
                  </button>
                )}
                {!share.isActive && (
                  <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-500 dark:bg-gray-700 dark:text-gray-400">Revoked</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      <CreateShareModal open={showCreate} onClose={() => setShowCreate(false)} onCreated={load} />
      <QrModal open={!!qrShare} onClose={() => setQrShare(null)} share={qrShare} />
      <AnalyticsModal open={!!analyticsShare} onClose={() => setAnalyticsShare(null)} share={analyticsShare} />
    </div>
  )
}
