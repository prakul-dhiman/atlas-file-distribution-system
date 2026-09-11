import { useState, useEffect } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'
import { healthApi } from '../api/services'
import {
  IconFolder, IconTrash, IconShare, IconSearch, IconServer, IconUser,
  IconSun, IconMoon, IconLogout, IconHome, IconLock, IconChartBar,
} from './Icons'

const navItems = [
  { to: '/files', label: 'My Files', icon: IconFolder, end: true },
  { to: '/analytics', label: 'Analytics', icon: IconChartBar },
  { to: '/shares', label: 'Public Links', icon: IconShare },
  { to: '/direct-shares', label: 'Shared With Me', icon: IconUser },
  { to: '/audit', label: 'Audit Logs', icon: IconLock },
  { to: '/branding', label: 'Portal Branding', icon: IconShare },
  { to: '/recycle-bin', label: 'Recycle Bin', icon: IconTrash },
  { to: '/search', label: 'Search', icon: IconSearch },
  { to: '/admin/nodes', label: 'Storage Node Admin', icon: IconServer },
  { to: '/profile', label: 'Profile', icon: IconUser },
  { to: '/settings', label: 'Settings', icon: IconLock },
]

function HealthIndicator() {
  const [status, setStatus] = useState('unknown')
  const [version, setVersion] = useState('')

  useEffect(() => {
    let mounted = true
    const check = async () => {
      try {
        const resp = await healthApi.get()
        if (mounted) {
          setStatus(resp.data?.status || 'unknown')
          setVersion(resp.data?.version || '')
        }
      } catch (e) {
        if (mounted) setStatus('DOWN')
      }
    }
    check()
    const interval = setInterval(check, 15000)
    return () => {
      mounted = false
      clearInterval(interval)
    }
  }, [])

  const color =
    status === 'UP' ? 'bg-green-500' : status === 'DOWN' ? 'bg-red-500' : 'bg-yellow-500'

  return (
    <div className="flex items-center gap-2 px-4 py-3 text-xs text-gray-500 dark:text-gray-400">
      <span className={`h-2.5 w-2.5 rounded-full ${color} ${status === 'UP' ? 'animate-pulse' : ''}`} />
      <span className="font-medium uppercase tracking-wide">System</span>
      <span className="text-gray-400 dark:text-gray-500">{status}</span>
      {version && <span className="text-gray-400 dark:text-gray-500">v{version}</span>}
    </div>
  )
}

export default function Layout() {
  const { user, logout } = useAuth()
  const { dark, toggle } = useTheme()
  const navigate = useNavigate()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  const initials = user
    ? `${user.firstName?.[0] || ''}${user.lastName?.[0] || ''}`.toUpperCase() || user.username?.[0]?.toUpperCase() || '?'
    : '?'
  const displayName = [user?.firstName, user?.lastName].filter(Boolean).join(' ') || user?.username || 'Account'

  return (
    <div className="flex h-screen bg-gray-50 dark:bg-gray-900">
      {/* Mobile sidebar backdrop */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-30 bg-black/50 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-gray-200 bg-white transition-transform dark:border-gray-700 dark:bg-gray-800 lg:static lg:translate-x-0 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Logo */}
        <div className="flex items-center gap-3 px-6 py-5">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary-600 text-white">
            <IconHome className="h-5 w-5" />
          </div>
          <div>
            <h1 className="text-lg font-bold text-gray-900 dark:text-gray-100">Project Atlas</h1>
            <p className="text-xs text-gray-500 dark:text-gray-400">Distributed Storage</p>
          </div>
        </div>

        {/* Nav */}
        <nav className="flex-1 space-y-1 px-3 py-2">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              onClick={() => setSidebarOpen(false)}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300'
                    : 'text-gray-600 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-700'
                }`
              }
            >
              <item.icon className="h-5 w-5" />
              {item.label}
            </NavLink>
          ))}
        </nav>

        {/* Health + User */}
        <div className="border-t border-gray-200 dark:border-gray-700">
          <HealthIndicator />
          <div className="flex items-center gap-3 px-4 py-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary-100 text-sm font-semibold text-primary-700 dark:bg-primary-900/40 dark:text-primary-300">
              {initials}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">
                {displayName}
              </p>
              <p className="truncate text-xs text-gray-500 dark:text-gray-400">@{user?.username}</p>
            </div>
            <button
              onClick={toggle}
              className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-700"
              title={dark ? 'Switch to light mode' : 'Switch to dark mode'}
            >
              {dark ? <IconSun className="h-5 w-5" /> : <IconMoon className="h-5 w-5" />}
            </button>
            <button
              onClick={handleLogout}
              className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-700"
              title="Logout"
            >
              <IconLogout className="h-5 w-5" />
            </button>
          </div>
        </div>
      </aside>

      {/* Main content */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Mobile top bar */}
        <header className="flex items-center justify-between border-b border-gray-200 bg-white px-4 py-3 dark:border-gray-700 dark:bg-gray-800 lg:hidden">
          <button
            onClick={() => setSidebarOpen(true)}
            className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-700"
          >
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
          <h1 className="text-lg font-bold text-gray-900 dark:text-gray-100">Project Atlas</h1>
          <button
            onClick={toggle}
            className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-700"
          >
            {dark ? <IconSun className="h-5 w-5" /> : <IconMoon className="h-5 w-5" />}
          </button>
        </header>

        <main className="flex-1 overflow-y-auto p-4 md:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
