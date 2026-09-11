import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import Spinner from '../components/Spinner'

function PasswordStrength({ password }) {
  const len = password.length
  const hasUpper = /[A-Z]/.test(password)
  const hasLower = /[a-z]/.test(password)
  const hasNumber = /[0-9]/.test(password)
  const hasSpecial = /[^A-Za-z0-9]/.test(password)

  let score = 0
  if (len >= 10) score++
  if (len >= 14) score++
  if (hasUpper && hasLower) score++
  if (hasNumber) score++
  if (hasSpecial) score++

  const labels = ['Very Weak', 'Weak', 'Fair', 'Good', 'Strong', 'Very Strong']
  const colors = ['bg-red-500', 'bg-red-400', 'bg-yellow-400', 'bg-yellow-500', 'bg-green-500', 'bg-green-600']
  const textColors = ['text-red-500', 'text-red-400', 'text-yellow-500', 'text-yellow-600', 'text-green-500', 'text-green-600']

  return (
    <div className="mt-2">
      <div className="flex gap-1">
        {[0, 1, 2, 3, 4].map((i) => (
          <div
            key={i}
            className={`h-1.5 flex-1 rounded-full ${i < score ? colors[score] : 'bg-gray-200 dark:bg-gray-700'}`}
          />
        ))}
      </div>
      <div className="mt-1.5 flex items-center justify-between text-xs">
        <span className={textColors[score]}>{labels[score]}</span>
        <span className={len >= 10 ? 'text-green-500' : 'text-gray-400 dark:text-gray-500'}>
          {len >= 10 ? '✓' : '✗'} Min 10 characters
        </span>
      </div>
    </div>
  )
}

export default function Register() {
  const [form, setForm] = useState({
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
  })
  const [error, setError] = useState('')
  const { register, loading } = useAuth()
  const { success } = useToast()
  const navigate = useNavigate()

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    if (form.password.length < 10) {
      setError('Password must be at least 10 characters long')
      return
    }
    try {
      await register(form)
      success('Account created successfully! You can now sign in.')
      navigate('/login')
    } catch (err) {
      setError(err.message)
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
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Create Account</h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Join Project Atlas distributed storage
          </p>
        </div>

        <div className="card p-8">
          <form onSubmit={handleSubmit} className="space-y-4">
            {error && (
              <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/30 dark:text-red-300">
                {error}
              </div>
            )}

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="label" htmlFor="firstName">First Name</label>
                <input
                  id="firstName"
                  name="firstName"
                  type="text"
                  className="input"
                  value={form.firstName}
                  onChange={handleChange}
                />
              </div>
              <div>
                <label className="label" htmlFor="lastName">Last Name</label>
                <input
                  id="lastName"
                  name="lastName"
                  type="text"
                  className="input"
                  value={form.lastName}
                  onChange={handleChange}
                />
              </div>
            </div>

            <div>
              <label className="label" htmlFor="username">Username</label>
              <input
                id="username"
                name="username"
                type="text"
                className="input"
                placeholder="johndoe"
                value={form.username}
                onChange={handleChange}
                required
                minLength={3}
                maxLength={50}
              />
            </div>

            <div>
              <label className="label" htmlFor="email">Email</label>
              <input
                id="email"
                name="email"
                type="email"
                className="input"
                placeholder="you@example.com"
                value={form.email}
                onChange={handleChange}
                required
              />
            </div>

            <div>
              <label className="label" htmlFor="password">Password</label>
              <input
                id="password"
                name="password"
                type="password"
                className="input"
                placeholder="At least 10 characters"
                value={form.password}
                onChange={handleChange}
                required
              />
              <PasswordStrength password={form.password} />
            </div>

            <button type="submit" className="btn-primary w-full" disabled={loading}>
              {loading ? <Spinner size="sm" className="border-white" /> : 'Create Account'}
            </button>
          </form>

          <div className="mt-6 text-center text-sm text-gray-500 dark:text-gray-400">
            Already have an account?{' '}
            <Link to="/login" className="font-medium text-primary-600 hover:text-primary-700 dark:text-primary-400">
              Sign in
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
