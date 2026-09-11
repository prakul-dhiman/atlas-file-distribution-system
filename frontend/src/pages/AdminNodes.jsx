import { useState, useEffect, useCallback, useRef } from 'react'
import { nodeApi } from '../api/services'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import EmptyState from '../components/EmptyState'
import Spinner from '../components/Spinner'
import {
  IconServer, IconRefresh, IconAlert, IconCheck,
} from '../components/Icons'
import { formatBytes } from '../utils/format'

function NodeCard({ node, onSimulateFailure, onRecover, busy, canManage }) {
  const isHealthy = node.status === 'HEALTHY' || node.status === 'UP'
  const isDown = node.status === 'DOWN'
  const usedPct = node.totalCapacityBytes > 0
    ? Math.round((node.usedCapacityBytes / node.totalCapacityBytes) * 100)
    : 0

  const statusColor = isDown
    ? 'bg-red-500'
    : isHealthy
    ? 'bg-green-500'
    : 'bg-yellow-500'

  return (
    <div className={`card p-5 transition-shadow hover:shadow-card-hover ${isDown ? 'border-red-300 dark:border-red-800' : ''}`}>
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-3">
          <div className={`flex h-11 w-11 items-center justify-center rounded-xl ${isDown ? 'bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400' : 'bg-primary-50 text-primary-600 dark:bg-primary-900/30 dark:text-primary-400'}`}>
            <IconServer className="h-6 w-6" />
          </div>
          <div>
            <h3 className="font-semibold text-gray-900 dark:text-gray-100">{node.nodeName}</h3>
            <p className="text-xs text-gray-400">Node #{node.nodeId}</p>
          </div>
        </div>
        <span className={`flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ${isDown ? 'bg-red-50 text-red-600 dark:bg-red-900/30 dark:text-red-400' : isHealthy ? 'bg-green-50 text-green-600 dark:bg-green-900/30 dark:text-green-400' : 'bg-yellow-50 text-yellow-600 dark:bg-yellow-900/30 dark:text-yellow-400'}`}>
          <span className={`h-2 w-2 rounded-full ${statusColor} ${isHealthy ? 'animate-pulse' : ''}`} />
          {node.status}
        </span>
      </div>

      <p className="mt-4 truncate rounded-md bg-gray-50 px-3 py-2 text-xs text-gray-500 dark:bg-gray-700/50 dark:text-gray-400">
        {node.endpointUrl}
      </p>

      {/* Capacity bar */}
      <div className="mt-4">
        <div className="mb-1 flex items-center justify-between text-xs">
          <span className="text-gray-500 dark:text-gray-400">Capacity</span>
          <span className="font-medium text-gray-700 dark:text-gray-200">{usedPct}% used</span>
        </div>
        <div className="h-2 w-full overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
          <div
            className={`h-full rounded-full ${usedPct > 90 ? 'bg-red-500' : usedPct > 70 ? 'bg-yellow-500' : 'bg-green-500'}`}
            style={{ width: `${usedPct}%` }}
          />
        </div>
        <p className="mt-1.5 text-xs text-gray-400">
          {formatBytes(node.usedCapacityBytes)} / {formatBytes(node.totalCapacityBytes)}
        </p>
      </div>

      {/* Actions */}
      {canManage ? (
      <div className="mt-4 flex gap-2">
        {isDown ? (
          <button
            onClick={() => onRecover(node)}
            disabled={busy === node.nodeId}
            className="btn-primary flex-1"
          >
            {busy === node.nodeId ? <Spinner size="sm" className="border-white" /> : <IconRefresh className="h-4 w-4" />}
            Recover
          </button>
        ) : (
          <button
            onClick={() => onSimulateFailure(node)}
            disabled={busy === node.nodeId}
            className="btn-secondary w-full border-red-200 text-red-600 hover:bg-red-50 dark:border-red-800 dark:text-red-400 dark:hover:bg-red-900/30"
          >
            {busy === node.nodeId ? <Spinner size="sm" /> : <IconAlert className="h-4 w-4" />}
            Simulate Failure
          </button>
        )}
      </div>
      ) : (
        <p className="mt-4 rounded-md bg-gray-50 px-3 py-2 text-xs text-gray-500 dark:bg-gray-700/50 dark:text-gray-400">
          Monitoring only. Admin role is required to simulate failure or recover nodes.
        </p>
      )}
    </div>
  )
}

export default function AdminNodes() {
  const [nodes, setNodes] = useState([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(null)
  const [refreshing, setRefreshing] = useState(false)
  const { success, error } = useToast()
  const errorRef = useRef(error)
  errorRef.current = error
  const { user } = useAuth()
  const canManage = user?.roles?.includes('ROLE_ADMIN')

  const load = useCallback(async (silent = false) => {
    if (!silent) setLoading(true)
    try {
      const resp = await nodeApi.list()
      setNodes(resp.data || [])
    } catch (e) {
      errorRef.current(e.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const handleSimulateFailure = async (node) => {
    if (!window.confirm(`Simulate failure of node "${node.nodeName}"? This tests failover and auto-healing.`)) return
    setBusy(node.nodeId)
    try {
      await nodeApi.simulateFailure(node.nodeId)
      success(`Simulated failure on ${node.nodeName}`)
      load(true)
    } catch (e) {
      error(e.message)
    } finally {
      setBusy(null)
    }
  }

  const handleRecover = async (node) => {
    setBusy(node.nodeId)
    try {
      await nodeApi.recover(node.nodeId)
      success(`${node.nodeName} recovered`)
      load(true)
    } catch (e) {
      error(e.message)
    } finally {
      setBusy(null)
    }
  }

  const handleRefresh = async () => {
    setRefreshing(true)
    try {
      await load(true)
      success('Cluster status refreshed')
    } finally {
      setRefreshing(false)
    }
  }

  const healthyCount = nodes.filter((n) => n.status !== 'DOWN').length

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Storage Nodes</h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Monitor the distributed storage cluster and test failover
          </p>
        </div>
        <button className="btn-secondary" onClick={handleRefresh} disabled={refreshing}>
          <IconRefresh className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} /> Refresh
        </button>
      </div>

      {/* Summary strip */}
      {nodes.length > 0 && (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          <div className="card p-4">
            <p className="text-sm text-gray-500 dark:text-gray-400">Total Nodes</p>
            <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-gray-100">{nodes.length}</p>
          </div>
          <div className="card p-4">
            <p className="text-sm text-gray-500 dark:text-gray-400">Healthy</p>
            <p className="mt-1 text-2xl font-bold text-green-600 dark:text-green-400">{healthyCount}</p>
          </div>
          <div className="card p-4">
            <p className="text-sm text-gray-500 dark:text-gray-400">Down</p>
            <p className="mt-1 text-2xl font-bold text-red-600 dark:text-red-400">{nodes.length - healthyCount}</p>
          </div>
          <div className="card p-4">
            <p className="text-sm text-gray-500 dark:text-gray-400">Total Capacity</p>
            <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-gray-100">
              {formatBytes(nodes.reduce((sum, n) => sum + n.totalCapacityBytes, 0))}
            </p>
          </div>
        </div>
      )}

      {loading ? (
        <div className="flex justify-center py-16"><Spinner size="lg" /></div>
      ) : nodes.length === 0 ? (
        <div className="card">
          <EmptyState
            icon={<IconServer className="h-8 w-8" />}
            title="No storage nodes"
            description="The storage cluster registry is empty."
          />
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {nodes.map((node) => (
            <NodeCard
              key={node.nodeId}
              node={node}
              busy={busy}
              canManage={canManage}
              onSimulateFailure={handleSimulateFailure}
              onRecover={handleRecover}
            />
          ))}
        </div>
      )}

      <div className="card p-4 text-sm text-gray-500 dark:text-gray-400">
        <p className="flex items-start gap-2">
          <IconCheck className="mt-0.5 h-4 w-4 shrink-0 text-green-500" />
          <span>
            Files are automatically chunked and replicated across the cluster. Simulating a node failure
            triggers the recovery scheduler to re-replicate chunks to healthy nodes.
          </span>
        </p>
      </div>
    </div>
  )
}
