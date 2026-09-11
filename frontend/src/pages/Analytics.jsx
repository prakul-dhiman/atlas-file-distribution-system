import { useState, useEffect } from 'react'
import { analyticsApi } from '../api/services'
import { useToast } from '../context/ToastContext'
import Spinner from '../components/Spinner'
import {
  IconDownload, IconGlobe, IconDevice, IconChartBar, IconFile, IconCheck, IconAlert
} from '../components/Icons'
import { formatBytes } from '../utils/format'

export default function Analytics() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [exporting, setExporting] = useState(false)
  const { success, error } = useToast()

  const loadData = async () => {
    setLoading(true)
    try {
      const resp = await analyticsApi.getOverview()
      setData(resp.data)
    } catch (err) {
      error(err.message || 'Failed to load analytics')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const handleExportCsv = async () => {
    setExporting(true)
    try {
      const resp = await analyticsApi.exportReport()
      const url = window.URL.createObjectURL(new Blob([resp.data]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', `analytics-report-${new Date().toISOString().slice(0, 10)}.csv`)
      document.body.appendChild(link)
      link.click()
      link.remove()
      success('Analytics report exported successfully')
    } catch (err) {
      error('Failed to export analytics report')
    } finally {
      setExporting(false)
    }
  }

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Spinner size="lg" />
      </div>
    )
  }

  const {
    totalDownloads = 0,
    uniqueDownloads = 0,
    failedDownloads = 0,
    suspiciousDownloads = 0,
    totalBytesTransferred = 0,
    countries = [],
    devices = [],
    browsers = [],
    operatingSystems = [],
    topFiles = [],
    recentEvents = []
  } = data || {}

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100 flex items-center gap-2">
            <IconChartBar className="h-7 w-7 text-primary-600 dark:text-primary-400" />
            Smart Download Analytics
          </h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Real-time traffic metrics, country breakdowns, device tracking, and security auditing.
          </p>
        </div>
        <button
          className="btn-primary"
          onClick={handleExportCsv}
          disabled={exporting}
        >
          {exporting ? <Spinner size="sm" className="border-white" /> : <IconDownload className="h-4 w-4" />}
          Export CSV Report
        </button>
      </div>

      {/* Overview Stat Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="card p-5">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-gray-500 dark:text-gray-400">Total Downloads</span>
            <div className="rounded-lg bg-primary-50 p-2 text-primary-600 dark:bg-primary-900/30 dark:text-primary-400">
              <IconDownload className="h-5 w-5" />
            </div>
          </div>
          <p className="mt-2 text-3xl font-extrabold text-gray-900 dark:text-gray-100">{totalDownloads}</p>
          <span className="mt-1 text-xs text-gray-400">Lifetime file access count</span>
        </div>

        <div className="card p-5">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-gray-500 dark:text-gray-400">Unique Visitors</span>
            <div className="rounded-lg bg-emerald-50 p-2 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400">
              <IconCheck className="h-5 w-5" />
            </div>
          </div>
          <p className="mt-2 text-3xl font-extrabold text-gray-900 dark:text-gray-100">{uniqueDownloads}</p>
          <span className="mt-1 text-xs text-emerald-500 font-medium">Distinct IP Addresses</span>
        </div>

        <div className="card p-5">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-gray-500 dark:text-gray-400">Total Data Served</span>
            <div className="rounded-lg bg-blue-50 p-2 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400">
              <IconGlobe className="h-5 w-5" />
            </div>
          </div>
          <p className="mt-2 text-3xl font-extrabold text-gray-900 dark:text-gray-100">{formatBytes(totalBytesTransferred)}</p>
          <span className="mt-1 text-xs text-gray-400">Bandwidth consumed</span>
        </div>

        <div className="card p-5">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-gray-500 dark:text-gray-400">Failed / Suspicious</span>
            <div className="rounded-lg bg-amber-50 p-2 text-amber-600 dark:bg-amber-900/30 dark:text-amber-400">
              <IconAlert className="h-5 w-5" />
            </div>
          </div>
          <p className="mt-2 text-3xl font-extrabold text-gray-900 dark:text-gray-100">{failedDownloads + suspiciousDownloads}</p>
          <span className="mt-1 text-xs text-amber-500 font-medium">Blocked & rate-limited</span>
        </div>
      </div>

      {/* Grid: Top Files & Geo Distribution */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Top Downloaded Files */}
        <div className="card p-5 space-y-4">
          <h2 className="text-base font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
            <IconFile className="h-5 w-5 text-primary-500" />
            Top Downloaded Files
          </h2>
          {topFiles.length === 0 ? (
            <p className="text-sm text-gray-400 py-6 text-center">No downloads logged yet</p>
          ) : (
            <ul className="divide-y divide-gray-100 dark:divide-gray-700">
              {topFiles.map((file, idx) => (
                <li key={file.fileId || idx} className="py-3 flex items-center justify-between">
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{file.fileName}</p>
                    <p className="text-xs text-gray-400">{formatBytes(file.totalBytes)} transferred</p>
                  </div>
                  <div className="ml-4 shrink-0 text-right">
                    <span className="inline-flex items-center rounded-full bg-primary-50 px-2.5 py-1 text-xs font-semibold text-primary-700 dark:bg-primary-900/30 dark:text-primary-300">
                      {file.count} downloads
                    </span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* Geographic Distribution */}
        <div className="card p-5 space-y-4">
          <h2 className="text-base font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
            <IconGlobe className="h-5 w-5 text-emerald-500" />
            Geographic Distribution
          </h2>
          {countries.length === 0 ? (
            <p className="text-sm text-gray-400 py-6 text-center">No location data captured yet</p>
          ) : (
            <ul className="space-y-3">
              {countries.map((c, i) => {
                const pct = totalDownloads > 0 ? Math.round((c.count / totalDownloads) * 100) : 0
                return (
                  <li key={c.countryCode || i} className="space-y-1">
                    <div className="flex justify-between text-xs font-medium text-gray-700 dark:text-gray-300">
                      <span>{c.country || 'Unknown Region'} ({c.countryCode})</span>
                      <span>{c.count} ({pct}%)</span>
                    </div>
                    <div className="h-2 w-full rounded-full bg-gray-100 dark:bg-gray-700 overflow-hidden">
                      <div
                        className="h-full bg-emerald-500 rounded-full transition-all duration-500"
                        style={{ width: `${pct}%` }}
                      />
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
        </div>
      </div>

      {/* Grid: Devices & Operating Systems */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="card p-5 space-y-3">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
            <IconDevice className="h-4 w-4 text-blue-500" />
            Devices
          </h3>
          {devices.length === 0 ? (
            <p className="text-xs text-gray-400 py-4 text-center">No data</p>
          ) : (
            <ul className="divide-y divide-gray-100 dark:divide-gray-700 text-xs">
              {devices.map((d, i) => (
                <li key={i} className="py-2 flex justify-between">
                  <span className="text-gray-600 dark:text-gray-300">{d.device}</span>
                  <span className="font-semibold text-gray-900 dark:text-gray-100">{d.count}</span>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="card p-5 space-y-3">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
            <IconDevice className="h-4 w-4 text-purple-500" />
            Browsers
          </h3>
          {browsers.length === 0 ? (
            <p className="text-xs text-gray-400 py-4 text-center">No data</p>
          ) : (
            <ul className="divide-y divide-gray-100 dark:divide-gray-700 text-xs">
              {browsers.map((b, i) => (
                <li key={i} className="py-2 flex justify-between">
                  <span className="text-gray-600 dark:text-gray-300">{b.browser}</span>
                  <span className="font-semibold text-gray-900 dark:text-gray-100">{b.count}</span>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="card p-5 space-y-3">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
            <IconDevice className="h-4 w-4 text-amber-500" />
            Operating Systems
          </h3>
          {operatingSystems.length === 0 ? (
            <p className="text-xs text-gray-400 py-4 text-center">No data</p>
          ) : (
            <ul className="divide-y divide-gray-100 dark:divide-gray-700 text-xs">
              {operatingSystems.map((os, i) => (
                <li key={i} className="py-2 flex justify-between">
                  <span className="text-gray-600 dark:text-gray-300">{os.os}</span>
                  <span className="font-semibold text-gray-900 dark:text-gray-100">{os.count}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      {/* Recent Activity Live Audit Feed */}
      <div className="card p-5 space-y-4">
        <h2 className="text-base font-semibold text-gray-900 dark:text-gray-100">Live Download Audit Feed</h2>
        {recentEvents.length === 0 ? (
          <p className="text-sm text-gray-400 py-6 text-center">No recent download activity</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-gray-600 dark:text-gray-300">
              <thead className="border-b border-gray-200 bg-gray-50 text-gray-700 uppercase dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400">
                <tr>
                  <th className="px-3 py-2.5">File</th>
                  <th className="px-3 py-2.5">IP Address</th>
                  <th className="px-3 py-2.5">Location</th>
                  <th className="px-3 py-2.5">Device & Browser</th>
                  <th className="px-3 py-2.5">Status</th>
                  <th className="px-3 py-2.5">Time</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
                {recentEvents.map((evt) => (
                  <tr key={evt.id} className="hover:bg-gray-50 dark:hover:bg-gray-800/50">
                    <td className="px-3 py-2.5 font-medium text-gray-900 dark:text-gray-100 truncate max-w-xs">{evt.fileName}</td>
                    <td className="px-3 py-2.5 font-mono text-gray-500">{evt.ipAddress || 'Unknown'}</td>
                    <td className="px-3 py-2.5">{evt.country ? `${evt.city || ''}, ${evt.country}` : 'Local Network'}</td>
                    <td className="px-3 py-2.5">{evt.device} · {evt.browser} ({evt.os})</td>
                    <td className="px-3 py-2.5">
                      <span className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold ${
                        evt.status === 'SUCCESS' ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400' : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'
                      }`}>
                        {evt.status}
                      </span>
                    </td>
                    <td className="px-3 py-2.5 text-gray-400">{evt.downloadedAt}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
