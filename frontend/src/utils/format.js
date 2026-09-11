export function formatBytes(bytes) {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`
}

export function formatDate(dateStr) {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  return d.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

export function formatDateTime(dateStr) {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  return d.toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function getFileIcon(name) {
  const ext = name?.split('.').pop()?.toLowerCase() || ''
  const imageExts = ['png', 'jpg', 'jpeg', 'gif', 'svg', 'webp', 'bmp', 'ico']
  const videoExts = ['mp4', 'mov', 'avi', 'mkv', 'webm', 'wmv']
  const audioExts = ['mp3', 'wav', 'ogg', 'flac', 'aac', 'm4a']
  const docExts = ['pdf', 'doc', 'docx', 'txt', 'md', 'rtf']
  const sheetExts = ['xls', 'xlsx', 'csv', 'tsv']
  const slideExts = ['ppt', 'pptx']
  const archiveExts = ['zip', 'rar', '7z', 'tar', 'gz', 'bz2']
  const codeExts = ['js', 'ts', 'jsx', 'tsx', 'py', 'java', 'c', 'cpp', 'go', 'rs', 'html', 'css', 'json', 'xml', 'yml', 'yaml', 'sh', 'sql']

  if (imageExts.includes(ext)) return 'image'
  if (videoExts.includes(ext)) return 'video'
  if (audioExts.includes(ext)) return 'audio'
  if (docExts.includes(ext)) return 'document'
  if (sheetExts.includes(ext)) return 'sheet'
  if (slideExts.includes(ext)) return 'slides'
  if (archiveExts.includes(ext)) return 'archive'
  if (codeExts.includes(ext)) return 'code'
  return 'file'
}

export function getFileColor(name) {
  const type = getFileIcon(name)
  const colors = {
    image: 'text-purple-500',
    video: 'text-pink-500',
    audio: 'text-amber-500',
    document: 'text-blue-500',
    sheet: 'text-green-500',
    slides: 'text-orange-500',
    archive: 'text-yellow-600',
    code: 'text-cyan-500',
    file: 'text-gray-400',
  }
  return colors[type] || colors.file
}