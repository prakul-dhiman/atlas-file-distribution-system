import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { shareApi } from '../api/services'
import Spinner from '../components/Spinner'
import { IconDownload, IconFile, IconFolder, IconLock, IconShare, IconMail } from '../components/Icons'
import { formatBytes } from '../utils/format'
import CountdownTimer from '../components/CountdownTimer'

function FileRow({ file, token, password, setError }) {
  const [loading, setLoading] = useState(false)

  const download = async () => {
    setLoading(true)
    setError('')
    try {
      const resp = await shareApi.signDownload(token, file.id, password)
      window.location.href = resp.data
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex items-center gap-3 rounded-lg border border-gray-100 p-3 dark:border-gray-700">
      <IconFile className="h-5 w-5 text-primary-600 dark:text-primary-300" />
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">{file.name}</p>
        <p className="text-xs text-gray-400">{formatBytes(file.sizeBytes || 0)}</p>
      </div>
      <button className="btn-primary" onClick={download} disabled={loading}>
        {loading ? <Spinner size="sm" className="border-white" /> : <IconDownload className="h-4 w-4" />}
        Download
      </button>
    </div>
  )
}

export default function PublicShare() {
  const { token } = useParams()
  const [metadata, setMetadata] = useState(null)
  const [content, setContent] = useState(null)
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(true)
  const [unlocking, setUnlocking] = useState(false)
  const [error, setError] = useState('')

  // OTP state
  const [otpEmail, setOtpEmail] = useState('')
  const [otpSent, setOtpSent] = useState(false)
  const [otpCode, setOtpCode] = useState('')
  const [sendingOtp, setSendingOtp] = useState(false)
  const [verifyingOtp, setVerifyingOtp] = useState(false)
  const [otpVerified, setOtpVerified] = useState(false)
  const [otpMessage, setOtpMessage] = useState('')

  useEffect(() => {
    shareApi.getPublicMetadata(token)
      .then((resp) => setMetadata(resp.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [token])

  const handleRequestOtp = async () => {
    if (!otpEmail.trim()) return
    setSendingOtp(true)
    setError('')
    setOtpMessage('')
    try {
      await shareApi.requestOtp(token, otpEmail.trim())
      setOtpSent(true)
      setOtpMessage(`A 6-digit access code was sent to ${otpEmail}`)
    } catch (err) {
      setError(err.message)
    } finally {
      setSendingOtp(false)
    }
  }

  const handleVerifyOtp = async () => {
    if (!otpCode.trim()) return
    setVerifyingOtp(true)
    setError('')
    try {
      await shareApi.verifyOtp(token, otpEmail.trim(), otpCode.trim())
      setOtpVerified(true)
      setOtpMessage('Email verified successfully!')
    } catch (err) {
      setError(err.message)
    } finally {
      setVerifyingOtp(false)
    }
  }

  const unlock = async () => {
    setUnlocking(true)
    setError('')
    try {
      const resp = await shareApi.accessPublic(token, password)
      setContent(resp.data)
    } catch (err) {
      setError(err.message)
    } finally {
      setUnlocking(false)
    }
  }

  const files = metadata?.fileId
    ? content ? [content] : []
    : content?.files || []
  const folders = metadata?.folderId ? content?.subfolders || [] : []

  const needsOtp = metadata?.requireEmailOtp && !otpVerified
  const needsPassword = metadata?.hasPassword

  return (
    <div className="min-h-screen bg-gray-50 p-4 dark:bg-gray-900 sm:p-8">
      <div className="mx-auto max-w-3xl space-y-5">
        <Link to="/" className="inline-flex items-center gap-2 text-sm font-medium text-primary-600 dark:text-primary-300">
          <IconShare className="h-4 w-4" /> Project Atlas
        </Link>

        <section className="card p-6">
          {loading ? (
            <div className="flex justify-center py-16"><Spinner size="lg" /></div>
          ) : metadata ? (
            <>
              <div className="flex items-start gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary-50 text-primary-600 dark:bg-primary-900/30 dark:text-primary-300">
                  {metadata.fileId ? <IconFile className="h-6 w-6" /> : <IconFolder className="h-6 w-6" />}
                </div>
                <div className="min-w-0 flex-1">
                  <h1 className="truncate text-2xl font-bold text-gray-900 dark:text-gray-100">{metadata.name}</h1>
                  <div className="mt-1 flex items-center gap-3 text-sm text-gray-500 dark:text-gray-400">
                    <span>{metadata.fileId ? 'Shared file' : 'Shared folder'}</span>
                    <CountdownTimer targetDate={metadata.expiresAt} />
                  </div>
                </div>
              </div>

              {error && (
                <div className="mt-5 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/30 dark:text-red-300">
                  {error}
                </div>
              )}

              {otpMessage && (
                <div className="mt-5 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 dark:border-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-300">
                  {otpMessage}
                </div>
              )}

              {!content && (
                <div className="mt-6 space-y-4">
                  {/* Step 1: Email OTP Verification if required */}
                  {needsOtp && (
                    <div className="rounded-lg border border-blue-100 bg-blue-50/50 p-4 space-y-3 dark:border-blue-900/50 dark:bg-blue-950/20">
                      <div className="flex items-center gap-2 text-sm font-semibold text-blue-900 dark:text-blue-200">
                        <IconMail className="h-4 w-4 text-blue-600 dark:text-blue-400" />
                        Email OTP Verification Required
                      </div>

                      {!otpSent ? (
                        <div className="space-y-3">
                          <div>
                            <label className="label">Enter your email address</label>
                            <input
                              type="email"
                              className="input"
                              value={otpEmail}
                              onChange={(e) => setOtpEmail(e.target.value)}
                              placeholder="you@example.com"
                            />
                          </div>
                          <button
                            className="btn-primary"
                            onClick={handleRequestOtp}
                            disabled={sendingOtp || !otpEmail.trim()}
                          >
                            {sendingOtp ? <Spinner size="sm" className="border-white" /> : 'Send Access Code'}
                          </button>
                        </div>
                      ) : (
                        <div className="space-y-3">
                          <div>
                            <label className="label">Enter 6-digit access code</label>
                            <input
                              type="text"
                              maxLength={6}
                              className="input tracking-widest text-center font-mono text-lg"
                              value={otpCode}
                              onChange={(e) => setOtpCode(e.target.value)}
                              placeholder="123456"
                            />
                          </div>
                          <div className="flex gap-2">
                            <button
                              className="btn-primary"
                              onClick={handleVerifyOtp}
                              disabled={verifyingOtp || otpCode.length < 6}
                            >
                              {verifyingOtp ? <Spinner size="sm" className="border-white" /> : 'Verify Code'}
                            </button>
                            <button
                              className="btn-secondary text-xs"
                              onClick={handleRequestOtp}
                              disabled={sendingOtp}
                            >
                              Resend Code
                            </button>
                          </div>
                        </div>
                      )}
                    </div>
                  )}

                  {/* Step 2: Password protection (unlocked after OTP if OTP is required) */}
                  {!needsOtp && needsPassword && (
                    <div>
                      <label className="label flex items-center gap-2"><IconLock className="h-4 w-4" /> Password</label>
                      <input
                        type="password"
                        className="input"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="Enter share password"
                      />
                    </div>
                  )}

                  {/* Open Share Button */}
                  {!needsOtp && (
                    <button className="btn-primary" onClick={unlock} disabled={unlocking || (needsPassword && !password)}>
                      {unlocking ? <Spinner size="sm" className="border-white" /> : 'Open Share'}
                    </button>
                  )}
                </div>
              )}

              {content && (
                <div className="mt-6 space-y-3">
                  {folders.map((folder) => (
                    <div key={folder.id} className="flex items-center gap-3 rounded-lg border border-gray-100 p-3 dark:border-gray-700">
                      <IconFolder className="h-5 w-5 text-amber-500" />
                      <p className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">{folder.name}</p>
                    </div>
                  ))}
                  {files.map((file) => (
                    <FileRow key={file.id} file={file} token={token} password={password} setError={setError} />
                  ))}
                  {files.length === 0 && folders.length === 0 && (
                    <p className="rounded-lg bg-gray-50 p-4 text-sm text-gray-500 dark:bg-gray-800 dark:text-gray-400">This share is empty.</p>
                  )}
                </div>
              )}
            </>
          ) : (
            <div className="py-10 text-center">
              <h1 className="text-xl font-bold text-gray-900 dark:text-gray-100">Share unavailable</h1>
              <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">{error || 'This link is invalid or expired.'}</p>
            </div>
          )}
        </section>
      </div>
    </div>
  )
}
