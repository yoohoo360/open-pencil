export type OssTransferMode = 'proxy' | 'direct'

export type AppConfig = {
  API_BASE_URL: string
  OSS_UPLOAD_MODE: OssTransferMode
  OSS_READ_MODE: OssTransferMode
  OSS_PUBLIC_BASE_URL?: string
  CANVASKIT_WASM_URL?: string
}
