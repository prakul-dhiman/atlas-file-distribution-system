import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { userApi } from '../api/services'
import { useToast } from '../context/ToastContext'
import Spinner from '../components/Spinner'
import { IconCheck, IconLock, IconUser } from '../components/Icons'
import { formatDate } from '../utils/format'

export default function Settings() {
  const { user } = useAuth()
  const { success, error } = useToast()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [loading, setLoading] = useState(false)

  const handleChangePassword = async (e) => {
    e.preventDefault()
    if (!currentPassword || !newPassword) {
      error('Please fill in all required fields')
      return
    }
    if (newPassword.length < 10) {
      error('New password must be at least 10 characters long')
      return
    }
    if (newPassword !== confirmPassword) {
      error('New password and confirm password do not match')
      return
    }

    setLoading(true)
    try {
      await userApi.changePassword({ currentPassword, newPassword })
      success('Password changed successfully!')
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
    } catch (err) {
      error(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Settings</h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">Account information and security details.</p>
      </div>

      <section className="card p-5">
        <div className="flex items-start gap-4">
          <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-primary-50 text-primary-600 dark:bg-primary-900/30 dark:text-primary-300">
            <IconUser className="h-5 w-5" />
          </div>
          <div className="min-w-0 flex-1 space-y-3">
            <h2 className="font-semibold text-gray-900 dark:text-gray-100">Account Details</h2>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div className="rounded-lg border border-gray-200 p-3 dark:border-gray-700">
                <p className="text-xs text-gray-400 uppercase tracking-wide">Username</p>
                <p className="mt-1 text-sm font-medium text-gray-900 dark:text-gray-100">@{user?.username}</p>
              </div>
              <div className="rounded-lg border border-gray-200 p-3 dark:border-gray-700">
                <p className="text-xs text-gray-400 uppercase tracking-wide">Email</p>
                <p className="mt-1 text-sm font-medium text-gray-900 dark:text-gray-100">{user?.email}</p>
              </div>
              <div className="rounded-lg border border-gray-200 p-3 dark:border-gray-700">
                <p className="text-xs text-gray-400 uppercase tracking-wide">Member Since</p>
                <p className="mt-1 text-sm font-medium text-gray-900 dark:text-gray-100">
                  {user?.createdAt ? formatDate(user.createdAt) : '—'}
                </p>
              </div>
              <div className="rounded-lg border border-gray-200 p-3 dark:border-gray-700">
                <p className="text-xs text-gray-400 uppercase tracking-wide">Status</p>
                <p className="mt-1 inline-flex items-center gap-1.5 text-sm font-medium text-green-600 dark:text-green-400">
                  <IconCheck className="h-4 w-4" /> Active
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="card p-5">
        <div className="flex items-start gap-4">
          <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-primary-50 text-primary-600 dark:bg-primary-900/30 dark:text-primary-300">
            <IconLock className="h-5 w-5" />
          </div>
          <div className="min-w-0 flex-1 space-y-4">
            <div>
              <h2 className="font-semibold text-gray-900 dark:text-gray-100">Change Password</h2>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                Update your account password. Must be at least 10 characters long.
              </p>
            </div>

            <form onSubmit={handleChangePassword} className="space-y-4 max-w-md">
              <div>
                <label className="label">Current Password</label>
                <input
                  type="password"
                  className="input"
                  placeholder="Enter current password"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  required
                />
              </div>

              <div>
                <label className="label">New Password</label>
                <input
                  type="password"
                  className="input"
                  placeholder="Enter new password (min. 10 chars)"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                />
              </div>

              <div>
                <label className="label">Confirm New Password</label>
                <input
                  type="password"
                  className="input"
                  placeholder="Confirm new password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                />
              </div>

              <button
                type="submit"
                disabled={loading}
                className="btn-primary"
              >
                {loading ? <Spinner size="sm" className="border-white" /> : 'Update Password'}
              </button>
            </form>
          </div>
        </div>
      </section>
    </div>
  )
}
