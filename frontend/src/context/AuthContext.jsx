import { createContext, useContext, useState, useCallback, useEffect } from 'react'
import { authApi, userApi } from '../api/services'
import { setTokens, clearTokens, setOnUnauthorized, getRefreshToken, getAccessToken } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [loading, setLoading] = useState(false)
  const [initializing, setInitializing] = useState(true)

  const handleAuthSuccess = useCallback((data) => {
    setTokens(data.accessToken, data.refreshToken)
    setUser(data.user)
    setIsAuthenticated(true)
  }, [])

  // Restore session from persisted tokens on app load
  useEffect(() => {
    const restoreSession = async () => {
      const accessToken = getAccessToken()
      const refreshToken = getRefreshToken()
      if (!accessToken && !refreshToken) {
        setInitializing(false)
        return
      }
      try {
        // Try to fetch the current user; if the access token is expired,
        // the interceptor will automatically refresh it using the refresh token.
        const resp = await userApi.me()
        setUser(resp.data)
        setIsAuthenticated(true)
      } catch (e) {
        // Session is invalid/expired — clear it
        clearTokens()
        setUser(null)
        setIsAuthenticated(false)
      } finally {
        setInitializing(false)
      }
    }
    restoreSession()
  }, [])

  const login = useCallback(async (credentials) => {
    setLoading(true)
    try {
      const resp = await authApi.login(credentials)
      handleAuthSuccess(resp.data)
      return resp.data
    } finally {
      setLoading(false)
    }
  }, [handleAuthSuccess])

  const register = useCallback(async (userData) => {
    setLoading(true)
    try {
      const resp = await authApi.register(userData)
      return resp.data
    } finally {
      setLoading(false)
    }
  }, [])

  const logout = useCallback(async () => {
    try {
      const refreshToken = getRefreshToken()
      if (refreshToken) {
        await authApi.logout(refreshToken)
      }
    } catch (e) {
      // Ignore logout errors
    }
    clearTokens()
    setUser(null)
    setIsAuthenticated(false)
  }, [])

  // Handle 401 -> redirect to login
  useEffect(() => {
    setOnUnauthorized(() => {
      clearTokens()
      setUser(null)
      setIsAuthenticated(false)
      window.location.href = '/login'
    })
  }, [])

  return (
    <AuthContext.Provider
      value={{ user, isAuthenticated, loading, initializing, login, register, logout, setUser }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
