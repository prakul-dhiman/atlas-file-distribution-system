import api from './client'

// ===== AUTH =====
export const authApi = {
  register: (data) => api.post('/api/v1/auth/register', data),
  login: (data) => api.post('/api/v1/auth/login', data),
  refresh: (refreshToken) => api.post('/api/v1/auth/refresh', { refreshToken }),
  logout: (refreshToken) => api.post('/api/v1/auth/logout', { refreshToken }),
  requestPasswordReset: (email) => api.post('/api/v1/auth/password-reset/request', { email }),
  confirmPasswordReset: (data) => api.post('/api/v1/auth/password-reset/confirm', data),
  verifyEmail: (token) => api.post('/api/v1/auth/verify-email', null, { params: { token } }),
  resendVerification: (email) => api.post('/api/v1/auth/resend-verification', { email }),
}

// ===== FOLDERS =====
export const folderApi = {
  list: (parentId) =>
    api.get('/api/v1/folders', { params: parentId ? { parentId } : {} }),
  create: (data) => api.post('/api/v1/folders', data),
  rename: (id, newName) => api.put(`/api/v1/folders/${id}/rename`, { newName }),
  move: (id, targetFolderId) => api.put(`/api/v1/folders/${id}/move`, { targetFolderId }),
  delete: (id) => api.delete(`/api/v1/folders/${id}`),
}

// ===== FILES =====
export const fileApi = {
  upload: (file, folderId, onProgress) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/api/v1/files/upload', formData, {
      params: folderId ? { folderId } : {},
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (onProgress && e.total) {
          onProgress(Math.round((e.loaded / e.total) * 100))
        }
      },
    })
  },
  get: (id) => api.get(`/api/v1/files/${id}`),
  rename: (id, newName) => api.put(`/api/v1/files/${id}/rename`, { newName }),
  move: (id, targetFolderId) => api.put(`/api/v1/files/${id}/move`, { targetFolderId }),
  copy: (id, targetFolderId) => api.post(`/api/v1/files/${id}/copy`, { targetFolderId }),
  delete: (id) => api.delete(`/api/v1/files/${id}`),
  download: (id) => api.get(`/api/v1/files/${id}/download`, { responseType: 'blob' }),
  versions: (id) => api.get(`/api/v1/files/${id}/versions`),
  restoreVersion: (id, version) => api.post(`/api/v1/files/${id}/versions/${version}/restore`),
  toggleStar: (id) => api.put(`/api/v1/files/${id}/star`),
  getStarred: () => api.get('/api/v1/files/starred'),
  downloadZip: (fileIds) => api.post('/api/v1/files/download-zip', fileIds, { responseType: 'blob' }),
}

// ===== SEARCH =====
export const searchApi = {
  search: (q) => api.get('/api/v1/search', { params: { q } }),
}

// ===== RECYCLE BIN =====
export const recycleBinApi = {
  list: () => api.get('/api/v1/recycle-bin'),
  restoreFile: (id) => api.post(`/api/v1/recycle-bin/files/${id}/restore`),
  restoreFolder: (id) => api.post(`/api/v1/recycle-bin/folders/${id}/restore`),
  purgeFile: (id) => api.delete(`/api/v1/recycle-bin/files/${id}/purge`),
  purgeFolder: (id) => api.delete(`/api/v1/recycle-bin/folders/${id}/purge`),
  empty: () => api.delete('/api/v1/recycle-bin/empty'),
}

// ===== SHARES =====
export const shareApi = {
  list: () => api.get('/api/v1/shares'),
  create: (data) => api.post('/api/v1/shares', data),
  revoke: (id) => api.delete(`/api/v1/shares/${id}`),
  getPublicMetadata: (token) => api.get(`/api/v1/shares/public/${token}`),
  accessPublic: (token, password) =>
    api.post(`/api/v1/shares/public/${token}/access`, { password }),
  signDownload: (token, fileId, password) =>
    api.post(`/api/v1/shares/public/${token}/sign-download`, { password }, { params: { fileId } }),
  requestOtp: (token, email) =>
    api.post(`/api/v1/shares/public/${token}/otp/request`, { email }),
  verifyOtp: (token, email, otp) =>
    api.post(`/api/v1/shares/public/${token}/otp/verify`, { email, otp }),
}

// ===== ADMIN / NODES =====
export const nodeApi = {
  list: () => api.get('/api/v1/admin/nodes'),
  simulateFailure: (id) => api.post(`/api/v1/admin/nodes/${id}/simulate-failure`),
  recover: (id) => api.post(`/api/v1/admin/nodes/${id}/recover`),
}

// ===== USERS =====
export const userApi = {
  me: () => api.get('/api/v1/users/me'),
  uploadProfileImage: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/api/v1/users/profile-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  changePassword: (data) => api.post('/api/v1/users/me/change-password', data),
}

// ===== ANALYTICS =====
export const analyticsApi = {
  getOverview: () => api.get('/api/v1/analytics/overview'),
  exportReport: () => api.get('/api/v1/analytics/export', { responseType: 'blob' }),
}

// ===== HEALTH =====
export const healthApi = {
  get: () => api.get('/api/v1/health'),
}

// ===== DIRECT SHARES (RBAC) =====
export const directShareApi = {
  create: (data) => api.post('/api/v1/direct-shares', data),
  sharedWithMe: () => api.get('/api/v1/direct-shares/shared-with-me'),
  collaborators: (fileId, folderId) =>
    api.get('/api/v1/direct-shares/collaborators', { params: { fileId, folderId } }),
  updatePermission: (shareId, permissionLevel) =>
    api.patch(`/api/v1/direct-shares/${shareId}/permission`, { permissionLevel }),
  revoke: (shareId) => api.delete(`/api/v1/direct-shares/${shareId}`),
}

// ===== AUDIT LOGS =====
export const auditApi = {
  getLogs: (page = 0, size = 50) =>
    api.get('/api/v1/audit', { params: { page, size } }),
  getMyLogs: (page = 0, size = 20) =>
    api.get('/api/v1/audit/mine', { params: { page, size } }),
  exportCsv: (from, to) =>
    api.get('/api/v1/audit/export', { params: { from, to }, responseType: 'blob' }),
}

// ===== BRANDING =====
export const brandingApi = {
  get: () => api.get('/api/v1/branding'),
  upsert: (data) => api.put('/api/v1/branding', data),
  getPublic: (shareToken) => api.get(`/api/v1/branding/public/${shareToken}`),
}

