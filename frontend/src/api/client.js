import axios from 'axios'

const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

const ACCESS_TOKEN_KEY = 'sdfds_access_token'
const REFRESH_TOKEN_KEY = 'sdfds_refresh_token'

let accessToken = localStorage.getItem(ACCESS_TOKEN_KEY) || null
let refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY) || null
let onUnauthorized = null

export const setTokens = (access, refresh) => {
  accessToken = access
  refreshToken = refresh
  if (access) localStorage.setItem(ACCESS_TOKEN_KEY, access)
  else localStorage.removeItem(ACCESS_TOKEN_KEY)
  if (refresh) localStorage.setItem(REFRESH_TOKEN_KEY, refresh)
  else localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export const clearTokens = () => {
  accessToken = null
  refreshToken = null
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export const getAccessToken = () => accessToken
export const getRefreshToken = () => refreshToken

export const setOnUnauthorized = (cb) => {
  onUnauthorized = cb
}

const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
    'ngrok-skip-browser-warning': 'true'
  },
})

api.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})

let isRefreshing = false
let pendingQueue = []

const processQueue = (error, token = null) => {
  pendingQueue.forEach((p) => (error ? p.reject(error) : p.resolve(token)))
  pendingQueue = []
}

api.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'success' in body) {
      if (body.success === false) return Promise.reject(new Error(body.message || 'Request failed'))
      return { ...response, data: body.data }
    }
    return response
  },
  async (error) => {
    const originalRequest = error.config
    const status = error.response?.status

    if ((status === 401 || status === 403) && !originalRequest._retry) {
      if (refreshToken) {
        if (isRefreshing) {
          return new Promise((resolve, reject) => {
            pendingQueue.push({ resolve, reject })
          }).then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            return api(originalRequest)
          })
        }

        originalRequest._retry = true
        isRefreshing = true

        try {
          const resp = await axios.post('/api/v1/auth/refresh', { refreshToken }, {
            headers: {
              'ngrok-skip-browser-warning': 'true'
            },
          })
          const body = resp.data
          if (!body.success) throw new Error(body.message || 'Refresh failed')
          const { accessToken: newAccess, refreshToken: newRefresh } = body.data
          setTokens(newAccess, newRefresh)
          processQueue(null, newAccess)
          originalRequest.headers.Authorization = `Bearer ${newAccess}`
          return api(originalRequest)
        } catch (refreshError) {
          processQueue(refreshError, null)
          clearTokens()
          if (onUnauthorized) onUnauthorized()
          return Promise.reject(refreshError)
        } finally {
          isRefreshing = false
        }
      } else {
        clearTokens()
        if (onUnauthorized) onUnauthorized()
      }
    }

    const msg = error.response?.data?.message || error.message || 'Request failed'
    return Promise.reject(new Error(msg))
  }
)

export default api
