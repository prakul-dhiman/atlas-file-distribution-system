import { createContext, useContext, useState, useCallback } from 'react'

const ToastContext = createContext(null)

let idCounter = 0

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const showToast = useCallback(
    (message, type = 'success') => {
      const id = ++idCounter
      setToasts((prev) => [...prev, { id, message, type }])
      setTimeout(() => removeToast(id), 4000)
    },
    [removeToast]
  )

  const success = useCallback((msg) => showToast(msg, 'success'), [showToast])
  const error = useCallback((msg) => showToast(msg, 'error'), [showToast])
  const info = useCallback((msg) => showToast(msg, 'info'), [showToast])

  return (
    <ToastContext.Provider value={{ success, error, info }}>
      {children}
      {/* Toast container */}
      <div className="fixed bottom-4 right-4 z-[100] flex flex-col gap-2">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className={`flex items-center gap-3 rounded-lg px-4 py-3 text-sm font-medium text-white shadow-lg animate-[slideIn_0.2s_ease-out] ${
              toast.type === 'success'
                ? 'bg-green-600'
                : toast.type === 'error'
                ? 'bg-red-600'
                : 'bg-blue-600'
            }`}
          >
            <span>
              {toast.type === 'success' ? '✓' : toast.type === 'error' ? '✕' : 'ℹ'}
            </span>
            <span>{toast.message}</span>
            <button
              onClick={() => removeToast(toast.id)}
              className="ml-2 text-white/70 hover:text-white"
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within ToastProvider')
  return ctx
}