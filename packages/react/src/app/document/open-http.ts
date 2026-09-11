import { downloadOSSObject } from '#react/app/document/oss'
import type { EditorStore } from '#react/app/editor/store'
import type { PencilDocument } from '#react/lib/client'

import { readFigFile } from '@open-pencil/core/io'
import { computeAllLayouts } from '@open-pencil/core/layout'

export async function applyFigBytes(
  store: EditorStore,
  bytes: Uint8Array,
  fileName: string
): Promise<void> {
  const fileBytes = new Uint8Array(bytes.byteLength)
  fileBytes.set(bytes)
  const file = new File([fileBytes.buffer], fileName, {
    type: 'application/octet-stream'
  })
  const imported = await readFigFile(file, { populate: 'first-page' })
  const firstPageId = imported.getPages()[0]?.id
  if (firstPageId) computeAllLayouts(imported, firstPageId)
  store.replaceGraph(imported)
  store.undo.clear()
  store.clearSelection()
  const pageId = store.graph.getPages()[0]?.id ?? store.graph.rootId
  await store.switchPage(pageId)
  store.zoomToFit()
}

export async function openHttpDocument(
  store: EditorStore,
  documentMeta: PencilDocument | undefined
): Promise<void> {
  const name = documentMeta?.name || 'Untitled'
  store.state.documentName = name
  store.state.documentVersion = documentMeta?.version ?? ''
    store.state.documentKey = documentMeta?.key ?? ''
    store.state.historyPreviewId = null
    store.state.loading = true
    store.notify()
    try {
      const figPath = documentMeta?.url
      if (!figPath) return
      const payload = await downloadOSSObject(figPath)
      if (payload.byteLength === 0) return
      await applyFigBytes(store, payload, `${name}.fig`)
      store.state.documentFigURL = figPath
  } finally {
    store.state.loading = false
    store.notify()
  }
}
