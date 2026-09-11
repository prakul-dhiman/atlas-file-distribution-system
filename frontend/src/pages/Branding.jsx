import { useEffect, useState } from 'react'
import { brandingApi } from '../api/services'
import { useToast } from '../context/ToastContext'
import Spinner from '../components/Spinner'

const PRESETS = [
  { name: 'Atlas Dark', primary: '#6366f1', accent: '#8b5cf6', bg: '#0f172a' },
  { name: 'Midnight Blue', primary: '#3b82f6', accent: '#06b6d4', bg: '#0c1a2e' },
  { name: 'Forest', primary: '#22c55e', accent: '#16a34a', bg: '#0d1f14' },
  { name: 'Crimson', primary: '#ef4444', accent: '#f97316', bg: '#1a0d0d' },
  { name: 'Rose Gold', primary: '#f43f5e', accent: '#ec4899', bg: '#1a0d12' },
]

export default function Branding() {
  const [form, setForm] = useState({
    brandName: '',
    logoUrl: '',
    primaryColor: '#6366f1',
    accentColor: '#8b5cf6',
    backgroundColor: '#0f172a',
    welcomeMessage: '',
    supportEmail: '',
    showPoweredBy: true,
  })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const { success, error } = useToast()

  useEffect(() => {
    brandingApi.get().then((resp) => {
      if (resp.data) setForm((prev) => ({ ...prev, ...resp.data }))
    }).catch(() => {}).finally(() => setLoading(false))
  }, [])

  const handleSave = async () => {
    setSaving(true)
    try {
      await brandingApi.upsert(form)
      success('Branding saved successfully!')
    } catch (e) {
      error(e.message)
    } finally {
      setSaving(false)
    }
  }

  const applyPreset = (preset) => {
    setForm((f) => ({ ...f, primaryColor: preset.primary, accentColor: preset.accent, backgroundColor: preset.bg }))
  }

  if (loading) return <div className="flex justify-center py-20"><Spinner size="lg" /></div>

  const previewStyle = {
    backgroundColor: form.backgroundColor,
    borderColor: form.primaryColor + '44',
  }

  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Branded Share Portal</h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Customize the experience recipients see when they open your share links
        </p>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Settings Panel */}
        <div className="card p-6 space-y-5">
          <h2 className="font-semibold text-gray-900 dark:text-gray-100">Settings</h2>

          <div>
            <label className="label">Brand Name</label>
            <input className="input" placeholder="Acme Corp" value={form.brandName}
              onChange={(e) => setForm((f) => ({ ...f, brandName: e.target.value }))} />
          </div>

          <div>
            <label className="label">Logo URL</label>
            <input className="input" placeholder="https://example.com/logo.png" value={form.logoUrl}
              onChange={(e) => setForm((f) => ({ ...f, logoUrl: e.target.value }))} />
          </div>

          <div>
            <label className="label">Welcome Message</label>
            <textarea className="input min-h-[80px] resize-y" placeholder="Hello! Here are your files..."
              value={form.welcomeMessage}
              onChange={(e) => setForm((f) => ({ ...f, welcomeMessage: e.target.value }))} />
          </div>

          <div>
            <label className="label">Support Email</label>
            <input className="input" type="email" placeholder="support@example.com" value={form.supportEmail}
              onChange={(e) => setForm((f) => ({ ...f, supportEmail: e.target.value }))} />
          </div>

          <div className="space-y-3">
            <label className="label">Color Presets</label>
            <div className="flex flex-wrap gap-2">
              {PRESETS.map((p) => (
                <button
                  key={p.name}
                  onClick={() => applyPreset(p)}
                  className="flex items-center gap-1.5 rounded-lg border border-gray-200 dark:border-gray-700 px-3 py-1.5 text-xs font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
                >
                  <span className="h-3 w-3 rounded-full" style={{ backgroundColor: p.primary }} />
                  {p.name}
                </button>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-3 gap-3">
            {[
              { label: 'Primary', key: 'primaryColor' },
              { label: 'Accent', key: 'accentColor' },
              { label: 'Background', key: 'backgroundColor' },
            ].map(({ label, key }) => (
              <div key={key}>
                <label className="label">{label}</label>
                <div className="flex items-center gap-2">
                  <input type="color" className="h-9 w-12 cursor-pointer rounded border border-gray-200 dark:border-gray-700 p-0.5"
                    value={form[key]} onChange={(e) => setForm((f) => ({ ...f, [key]: e.target.value }))} />
                  <span className="text-xs text-gray-500 font-mono">{form[key]}</span>
                </div>
              </div>
            ))}
          </div>

          <label className="flex items-center gap-2 cursor-pointer text-sm text-gray-700 dark:text-gray-300">
            <input type="checkbox" className="rounded" checked={form.showPoweredBy}
              onChange={(e) => setForm((f) => ({ ...f, showPoweredBy: e.target.checked }))} />
            Show "Powered by Atlas" footer
          </label>

          <button className="btn-primary w-full" onClick={handleSave} disabled={saving}>
            {saving ? <Spinner size="sm" className="border-white" /> : 'Save Branding'}
          </button>
        </div>

        {/* Live Preview */}
        <div>
          <h2 className="label mb-2">Live Preview</h2>
          <div
            className="rounded-xl border-2 p-6 space-y-4 transition-all duration-300"
            style={previewStyle}
          >
            {form.logoUrl && (
              <img src={form.logoUrl} alt="Brand Logo" className="h-10 object-contain"
                onError={(e) => { e.target.style.display = 'none' }} />
            )}
            <h3 className="text-lg font-bold" style={{ color: form.primaryColor }}>
              {form.brandName || 'Your Brand Name'}
            </h3>
            {form.welcomeMessage && (
              <p className="text-sm text-gray-400">{form.welcomeMessage}</p>
            )}
            <div className="rounded-lg p-3 text-sm" style={{ border: `1px solid ${form.primaryColor}33`, backgroundColor: form.primaryColor + '11' }}>
              <p className="font-medium text-gray-200">📄 example-report.pdf</p>
              <p className="text-xs text-gray-400 mt-0.5">2.4 MB · Shared file</p>
            </div>
            <button className="w-full py-2 rounded-lg text-sm font-semibold text-white transition-opacity hover:opacity-90"
              style={{ backgroundColor: form.primaryColor }}>
              Download File
            </button>
            {form.supportEmail && (
              <p className="text-xs text-center text-gray-500">Need help? <a className="underline" href={`mailto:${form.supportEmail}`}>{form.supportEmail}</a></p>
            )}
            {form.showPoweredBy && (
              <p className="text-xs text-center text-gray-600">Powered by <span style={{ color: form.accentColor }}>Atlas</span></p>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
