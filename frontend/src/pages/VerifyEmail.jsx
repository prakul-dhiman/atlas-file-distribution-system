import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { authApi } from '../api/services'
import Spinner from '../components/Spinner'
import { IconCheck, IconX } from '../components/Icons'

export default function VerifyEmail() {
  const [searchParams] = useSearchParams()
  const [status, setStatus] = useState('loading')
  const [message, setMessage] = useState('Verifying your account...')

  useEffect(() => {
    const token = searchParams.get('token')
    if (!token) {
      setStatus('error')
      setMessage('Verification token is missing.')
      return
    }

    authApi.verifyEmail(token)
      .then(() => {
        setStatus('success')
        setMessage('Your email is verified. You can now sign in.')
      })
      .catch((err) => {
        setStatus('error')
        setMessage(err.message || 'Verification link is invalid or expired.')
      })
  }, [searchParams])

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 p-4 dark:bg-gray-900">
      <div className="card w-full max-w-md p-8 text-center">
        <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-primary-50 text-primary-600 dark:bg-primary-900/30 dark:text-primary-300">
          {status === 'loading' && <Spinner />}
          {status === 'success' && <IconCheck className="h-7 w-7" />}
          {status === 'error' && <IconX className="h-7 w-7 text-red-500" />}
        </div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Account Verification</h1>
        <p className="mt-3 text-sm text-gray-500 dark:text-gray-400">{message}</p>
        <Link to="/login" className="btn-primary mt-6 inline-flex">
          Go to Sign In
        </Link>
      </div>
    </div>
  )
}
