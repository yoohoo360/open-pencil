import dev from './config.dev'
import local from './config.local'
import prod from './config.prod'
import type { AppConfig } from './types'

const configs = {
  local,
  dev,
  test: dev,
  prod
} as const satisfies Record<string, AppConfig>

function loadConfig(): AppConfig {
  const env = import.meta.env.APP_ENV
  if (env && env in configs) return configs[env as keyof typeof configs]
  return local
}

export type { AppConfig, OssTransferMode } from './types'
export default loadConfig()
