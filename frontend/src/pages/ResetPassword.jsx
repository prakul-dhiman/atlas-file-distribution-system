import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { authApi } from '../api/services'
import { useToast } from '../context/ToastContext'
import Spinner from '../components/Spinner'

export default function ResetPassword() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') || ''
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [errorMsg, setErrorMsg] = useState('')
  const [loading, setLoading] = useState(false)
  const { success, error } = useToast()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setErrorMsg('')
    if (password.length < 10) {
      setErrorMsg('Password must be at least 10 characters long')
      return
    }
    if (password !== confirm) {
      setErrorMsg('Passwords do not match')
      return
    }
    setLoading(true)
    try {
      await authApi.confirmPasswordReset({ token, newPassword: password })
      success('Password reset successfully! Please sign in.')
      navigate('/login')
    } catch (err) {
      error(err.message)
      setErrorMsg(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-primary-50 via-white to-primary-100 p-4 dark:from-gray-900 dark:via-gray-900 dark:to-gray-800">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-primary-600 text-white shadow-lg">
            <svg className="h-7 w-7" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Set New Password</h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Choose a new password for your account
          </p>
        </div>

        <div className="card p-8">
          {!token && (
            <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/30 dark:text-red-300">
              Invalid reset link. The token is missing — please request a new reset link.
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {errorMsg && (
              <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/30 dark:text-red-300">
                {errorMsg}
              </div>
            )}

            <div>
              <label className="label" htmlFor="password">New Password</label>
              <input
                id="password"
                type="password"
                className="input"
                placeholder="At least 10 characters"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                minLength={10}
                autoFocus
              />
            </div>

            <div>
              <label className="label" htmlFor="confirm">Confirm Password</label>
              <input
                id="confirm"
                type="password"
                className="input"
                placeholder="Re-enter your password"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                required
              />
            </div>

            <button type="submit" className="btn-primary w-full" disabled={loading || !token}>
              {loading ? <Spinner size="sm" className="border-white" /> : 'Reset Password'}
            </button>
          </form>

          <div className="mt-6 text-center text-sm text-gray-500 dark:text-gray-400">
            Remembered your password?{' '}
            <Link to="/login" className="font-medium text-primary-600 hover:text-primary-700 dark:text-primary-400">
              Sign in
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}