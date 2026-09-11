import { useState, useEffect, useCallback, useRef } from 'react'
import { userApi, authApi } from '../api/services'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import Spinner from '../components/Spinner'
import { IconUser, IconCheck, IconX, IconClock, IconCamera } from '../components/Icons'
import { formatBytes, formatDate } from '../utils/format'

export default function Profile() {
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [resending, setResending] = useState(false)
  const fileInputRef = useRef(null)
  const { user, setUser } = useAuth()
  const { error, success } = useToast()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const resp = await userApi.me()
      setProfile(resp.data)
      setUser(resp.data)
    } catch (e) {
      error(e.message)
    } finally {
      setLoading(false)
    }
  }, [error, setUser])

  useEffect(() => {
    load()
  }, [load])

  const handleFileChange = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return

    // Validate file type
    const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
    if (!allowedTypes.includes(file.type)) {
      error('Only JPEG, PNG, GIF, and WebP images are allowed')
      return
    }
    // Validate file size (5 MB max)
    if (file.size > 5 * 1024 * 1024) {
      error('Profile image must be 5 MB or smaller')
      return
    }

    setUploading(true)
    try {
      const resp = await userApi.uploadProfileImage(file)
      setProfile(resp.data)
      setUser(resp.data)
      success('Profile picture updated')
    } catch (e) {
      error(e.message)
    } finally {
      setUploading(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  if (loading || !profile) {
    return (
      <div className="flex justify-center py-16"><Spinner size="lg" /></div>
    )
  }

  const handleResendVerification = async () => {
    if (!profile?.email) return
    setResending(true)
    try {
      await authApi.resendVerification(profile.email)
      success('Verification email sent! Check your inbox or dev logs.')
    } catch (e) {
      error(e.message)
    } finally {
      setResending(false)
    }
  }

  const usedPct = profile.storageQuotaBytes > 0
    ? Math.min(100, Math.round((profile.usedStorageBytes / profile.storageQuotaBytes) * 100))
    : 0

  const quotaColor = usedPct > 90 ? 'bg-red-500' : usedPct > 70 ? 'bg-yellow-500' : 'bg-primary-600'

  const initials = `${profile.firstName?.[0] || ''}${profile.lastName?.[0] || ''}`.toUpperCase() || profile.username?.[0]?.toUpperCase() || '?'

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Profile</h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Your account details and storage usage
        </p>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Account card */}
        <div className="card p-6 lg:col-span-2">
          <div className="flex items-center gap-4">
            {/* Avatar / Profile image */}
            <div className="relative">
              {profile.profileImageUrl ? (
                <img
                  src={profile.profileImageUrl}
                  alt="Profile"
                  className="h-16 w-16 rounded-2xl object-cover"
                  onError={(e) => { e.target.style.display = 'none' }}
                />
              ) : (
                <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-primary-100 text-2xl font-bold text-primary-700 dark:bg-primary-900/40 dark:text-primary-300">
                  {initials}
                </div>
              )}
              {/* Upload button */}
              <button
                onClick={() => fileInputRef.current?.click()}
                disabled={uploading}
                title="Upload profile picture"
                className="absolute -bottom-1 -right-1 flex h-7 w-7 items-center justify-center rounded-full bg-primary-600 text-white shadow-md transition-colors hover:bg-primary-700 disabled:opacity-50"
              >
                {uploading ? <Spinner size="xs" className="border-white" /> : <IconCamera className="h-3.5 w-3.5" />}
              </button>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/gif,image/webp"
                className="hidden"
                onChange={handleFileChange}
              />
            </div>
            <div>
              <h2 className="text-xl font-bold text-gray-900 dark:text-gray-100">
                {profile.firstName} {profile.lastName}
              </h2>
              <p className="text-sm text-gray-500 dark:text-gray-400">@{profile.username}</p>
            </div>
          </div>

          <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
              <p className="text-xs font-medium uppercase tracking-wide text-gray-400">Email</p>
              <div className="mt-1 flex flex-wrap items-center justify-between gap-2">
                <span className="text-sm font-medium text-gray-900 dark:text-gray-100">{profile.email}</span>
                {profile.isEmailVerified ? (
                  <span className="flex items-center gap-0.5 text-xs text-green-600 dark:text-green-400">
                    <IconCheck className="h-3.5 w-3.5" /> Verified
                  </span>
                ) : (
                  <div className="flex items-center gap-2">
                    <span className="flex items-center gap-0.5 text-xs text-amber-600 dark:text-amber-400">
                      <IconX className="h-3.5 w-3.5" /> Unverified
                    </span>
                    <button
                      onClick={handleResendVerification}
                      disabled={resending}
                      className="rounded bg-amber-50 px-2 py-0.5 text-xs font-medium text-amber-700 hover:bg-amber-100 dark:bg-amber-900/30 dark:text-amber-300 dark:hover:bg-amber-900/50"
                    >
                      {resending ? 'Sending...' : 'Resend link'}
                    </button>
                  </div>
                )}
              </div>
            </div>

            <div className="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
              <p className="text-xs font-medium uppercase tracking-wide text-gray-400">Member Since</p>
              <p className="mt-1 flex items-center gap-2 text-sm font-medium text-gray-900 dark:text-gray-100">
                <IconClock className="h-4 w-4 text-gray-400" />
                {formatDate(profile.createdAt)}
              </p>
            </div>

            <div className="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
              <p className="text-xs font-medium uppercase tracking-wide text-gray-400">Roles</p>
              <div className="mt-1 flex flex-wrap gap-1.5">
                {profile.roles?.map((role) => (
                  <span
                    key={role}
                    className="rounded-full bg-primary-50 px-2.5 py-1 text-xs font-medium text-primary-700 dark:bg-primary-900/30 dark:text-primary-300"
                  >
                    {role}
                  </span>
                ))}
              </div>
            </div>

            <div className="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
              <p className="text-xs font-medium uppercase tracking-wide text-gray-400">Account Status</p>
              <p className="mt-1 text-sm font-medium text-gray-900 dark:text-gray-100">
                {profile.isActive ? (
                  <span className="flex items-center gap-1 text-green-600 dark:text-green-400">
                    <IconCheck className="h-4 w-4" /> Active
                  </span>
                ) : (
                  <span className="flex items-center gap-1 text-red-600 dark:text-red-400">
                    <IconX className="h-4 w-4" /> Disabled
                  </span>
                )}
              </p>
            </div>
          </div>
        </div>

        {/* Storage usage card */}
        <div className="card p-6">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-50 text-primary-600 dark:bg-primary-900/30 dark:text-primary-400">
              <IconUser className="h-5 w-5" />
            </div>
            <div>
              <h3 className="font-semibold text-gray-900 dark:text-gray-100">Storage Usage</h3>
              <p className="text-xs text-gray-400">{usedPct}% of quota used</p>
            </div>
          </div>

          <div className="mt-4">
            <div className="h-3 w-full overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
              <div
                className={`h-full rounded-full ${quotaColor} transition-all`}
                style={{ width: `${usedPct}%` }}
              />
            </div>
            <div className="mt-2 flex items-center justify-between text-sm">
              <span className="font-medium text-gray-900 dark:text-gray-100">
                {formatBytes(profile.usedStorageBytes)}
              </span>
              <span className="text-gray-400">of {formatBytes(profile.storageQuotaBytes)}</span>
            </div>
          </div>

          {usedPct > 90 && (
            <p className="mt-4 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-700 dark:border-amber-800 dark:bg-amber-900/30 dark:text-amber-300">
              You are running low on storage. Consider deleting old files or emptying the recycle bin.
            </p>
          )}
        </div>
      </div>
    </div>
  )
}