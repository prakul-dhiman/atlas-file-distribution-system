import { useEffect, useState, useCallback, useRef } from 'react'
import { auditApi } from '../api/services'
import { useToast } from '../context/ToastContext'
import Spinner from '../components/Spinner'

const SEVERITY_STYLES = {
  INFO:     'bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300',
  WARN:     'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300',
  WARNING:  'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300',
  ERROR:    'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300',
  CRITICAL: 'bg-purple-100 text-purple-700 dark:bg-purple-900/40 dark:text-purple-300',
}

function SeverityBadge({ severity }) {
  const s = (severity || 'INFO').toUpperCase()
  return (
    <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${SEVERITY_STYLES[s] || SEVERITY_STYLES.INFO}`}>
      {s}
    </span>
  )
}

export default function AuditLogs() {
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [viewMode, setViewMode] = useState('all') // 'all' | 'mine'
  const [exporting, setExporting] = useState(false)
  const { error } = useToast()
  const errorRef = useRef(error)
  errorRef.current = error

  const load = useCallback(async (p = 0) => {
    setLoading(true)
    try {
      const resp = viewMode === 'mine'
        ? await auditApi.getMyLogs(p, 20)
        : await auditApi.getLogs(p, 50)
      const pageData = resp.data
      setLogs(pageData.content || [])
      setTotalPages(pageData.totalPages || 0)
      setPage(p)
    } catch (e) {
      errorRef.current(e.message)
    } finally {
      setLoading(false)
    }
  }, [viewMode])

  useEffect(() => { load(0) }, [load])

  const handleExport = async () => {
    setExporting(true)
    try {
      const from = new Date(Date.now() - 30 * 86400 * 1000).toISOString()
      const to = new Date().toISOString()
      const resp = await auditApi.exportCsv(from, to)
      const url = URL.createObjectURL(resp.data)
      const a = document.createElement('a')
      a.href = url; a.download = 'audit-logs.csv'; a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      errorRef.current(e.message)
    } finally {
      setExporting(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Audit Logs</h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Immutable compliance log of all system events
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="flex rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden">
            {['all', 'mine'].map((mode) => (
              <button
                key={mode}
                onClick={() => setViewMode(mode)}
                className={`px-4 py-2 text-sm font-medium transition-colors ${viewMode === mode
                  ? 'bg-primary-600 text-white'
                  : 'bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700'}`}
              >
                {mode === 'all' ? 'All Events' : 'My Events'}
              </button>
            ))}
          </div>
          <button className="btn-secondary text-sm" onClick={handleExport} disabled={exporting}>
            {exporting ? <Spinner size="sm" /> : '⬇ Export CSV'}
          </button>
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner size="lg" /></div>
      ) : logs.length === 0 ? (
        <div className="card p-16 text-center">
          <p className="text-gray-400 text-sm">No audit events recorded yet.</p>
        </div>
      ) : (
        <>
          <div className="card overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 dark:border-gray-700 bg-gray-50/50 dark:bg-gray-800/50">
                  <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-400">Time</th>
                  <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-400">User</th>
                  <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-400">Event</th>
                  <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-400">Description</th>
                  <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-400">IP</th>
                  <th className="px-4 py-3 text-left font-semibold text-gray-600 dark:text-gray-400">Severity</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
                {logs.map((log) => (
                  <tr key={log.id} className="hover:bg-gray-50 dark:hover:bg-gray-800/40 transition-colors">
                    <td className="px-4 py-3 text-gray-400 text-xs whitespace-nowrap">
                      {new Date(log.createdAt).toLocaleString()}
                    </td>
                    <td className="px-4 py-3 font-medium text-gray-800 dark:text-gray-200">
                      {log.username || log.user?.username || <span className="text-gray-400 italic">anonymous</span>}
                    </td>
                    <td className="px-4 py-3 font-mono text-xs text-primary-600 dark:text-primary-400 whitespace-nowrap">
                      {log.eventType}
                    </td>
                    <td className="px-4 py-3 text-gray-600 dark:text-gray-300 max-w-xs truncate" title={log.eventDescription}>
                      {log.eventDescription}
                    </td>
                    <td className="px-4 py-3 text-gray-400 text-xs font-mono">{log.ipAddress || '—'}</td>
                    <td className="px-4 py-3"><SeverityBadge severity={log.severity} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2">
              <button className="btn-secondary text-sm" disabled={page === 0} onClick={() => load(page - 1)}>
                ← Prev
              </button>
              <span className="text-sm text-gray-500 dark:text-gray-400">
                Page {page + 1} of {totalPages}
              </span>
              <button className="btn-secondary text-sm" disabled={page >= totalPages - 1} onClick={() => load(page + 1)}>
                Next →
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
