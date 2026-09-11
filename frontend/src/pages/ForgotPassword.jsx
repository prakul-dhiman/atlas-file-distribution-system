import { useState } from 'react'
import { Link } from 'react-router-dom'
import { authApi } from '../api/services'
import { useToast } from '../context/ToastContext'
import Spinner from '../components/Spinner'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [loading, setLoading] = useState(false)
  const { error } = useToast()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      await authApi.requestPasswordReset(email)
      setSubmitted(true)
    } catch (err) {
      error(err.message)
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
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Reset Password</h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Enter your email and we'll send you a reset link
          </p>
        </div>

        <div className="card p-8">
          {submitted ? (
            <div className="text-center">
              <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400">
                <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Check your email</h2>
              <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
                If an account exists for <span className="font-medium">{email}</span>, a password reset link
                has been sent. Follow the instructions in the email to reset your password.
              </p>
              <Link
                to="/login"
                className="mt-6 inline-block font-medium text-primary-600 hover:text-primary-700 dark:text-primary-400"
              >
                Back to login
              </Link>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-5">
              <div>
                <label className="label" htmlFor="email">Email</label>
                <input
                  id="email"
                  type="email"
                  className="input"
                  placeholder="you@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoFocus
                />
              </div>

              <button type="submit" className="btn-primary w-full" disabled={loading}>
                {loading ? <Spinner size="sm" className="border-white" /> : 'Send Reset Link'}
              </button>
            </form>
          )}

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