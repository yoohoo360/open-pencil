export type OssObjectKind = 'fig' | 'img'

export function quarterDirectory(date = new Date()): string {
  const year = date.getFullYear()
  const quarter = Math.floor(date.getMonth() / 3) + 1
  return `${year}Q${quarter}`
}

export function sanitizeStorageSegment(value: string): string {
  const cleaned = value.trim().replace(/[\\/]+/g, '-').replace(/\s+/g, '-')
  return cleaned || 'anonymous'
}

export function withCacheBust(url: string, revision?: string | number): string {
  if (!url) return url
  const token = revision === undefined || revision === '' ? Date.now() : revision
  const separator = url.includes('?') ? '&' : '?'
  return `${url}${separator}t=${encodeURIComponent(String(token))}`
}

export function ossObjectDirectory(
  kind: OssObjectKind,
  username: string,
  date = new Date()
): string {
  return `${kind}/${sanitizeStorageSegment(username)}/${quarterDirectory(date)}`
}
