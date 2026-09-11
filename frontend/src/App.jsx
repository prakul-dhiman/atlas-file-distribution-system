import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Layout from './components/Layout'
import Landing from './pages/Landing'
import Login from './pages/Login'
import Register from './pages/Register'
import ForgotPassword from './pages/ForgotPassword'
import ResetPassword from './pages/ResetPassword'
import FileBrowser from './pages/FileBrowser'
import RecycleBin from './pages/RecycleBin'
import Shares from './pages/Shares'
import Search from './pages/Search'
import AdminNodes from './pages/AdminNodes'
import Profile from './pages/Profile'
import PublicShare from './pages/PublicShare'
import Settings from './pages/Settings'
import Analytics from './pages/Analytics'
import DirectShares from './pages/DirectShares'
import AuditLogs from './pages/AuditLogs'
import Branding from './pages/Branding'
import Spinner from './components/Spinner'

function HomeRoute() {
  const { isAuthenticated, initializing } = useAuth()

  if (initializing) {
    return (
      <div className="flex h-screen items-center justify-center bg-gray-50 dark:bg-gray-900">
        <Spinner size="lg" />
      </div>
    )
  }

  return isAuthenticated ? <Navigate to="/files" replace /> : <Landing />
}

function ProtectedRoute() {
  const { isAuthenticated, initializing } = useAuth()

  if (initializing) {
    return (
      <div className="flex h-screen items-center justify-center bg-gray-50 dark:bg-gray-900">
        <Spinner size="lg" />
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}

function PublicOnlyRoute({ children }) {
  const { isAuthenticated, initializing } = useAuth()

  if (initializing) {
    return (
      <div className="flex h-screen items-center justify-center bg-gray-50 dark:bg-gray-900">
        <Spinner size="lg" />
      </div>
    )
  }

  if (isAuthenticated) {
    return <Navigate to="/files" replace />
  }

  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomeRoute />} />
      <Route path="/share/:token" element={<PublicShare />} />

      <Route path="/login" element={<PublicOnlyRoute><Login /></PublicOnlyRoute>} />
      <Route path="/register" element={<PublicOnlyRoute><Register /></PublicOnlyRoute>} />
      <Route path="/forgot-password" element={<PublicOnlyRoute><ForgotPassword /></PublicOnlyRoute>} />
      <Route path="/reset-password" element={<PublicOnlyRoute><ResetPassword /></PublicOnlyRoute>} />

      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/files" element={<FileBrowser />} />
          <Route path="/analytics" element={<Analytics />} />
          <Route path="/recycle-bin" element={<RecycleBin />} />
          <Route path="/shares" element={<Shares />} />
          <Route path="/search" element={<Search />} />
          <Route path="/admin/nodes" element={<AdminNodes />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/settings" element={<Settings />} />
          <Route path="/direct-shares" element={<DirectShares />} />
          <Route path="/audit" element={<AuditLogs />} />
          <Route path="/branding" element={<Branding />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
