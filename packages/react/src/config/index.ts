const env = import.meta.env.APP_ENV

function resolveAPIBaseURL(): string {
  if (env === 'test') return 'http://pencil.api.dev.yoohoo.cn'
  if (env === 'prod') return 'https://api.yoohoo.cn'
  return 'http://localhost:8080'
}

export default {
  API_BASE_URL: resolveAPIBaseURL()
}
