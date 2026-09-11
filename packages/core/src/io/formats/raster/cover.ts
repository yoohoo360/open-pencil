import type { SceneGraph } from '@open-pencil/scene-graph'
import { computeDescendantVisualBounds } from '@open-pencil/scene-graph/geometry'

export const COVER_MAX_WIDTH = 1600
export const COVER_ASPECT_WIDTH = 4
export const COVER_ASPECT_HEIGHT = 3

export type CoverCapture = {
  x: number
  y: number
  width: number
  height: number
}

function collectCoverGraphicIds(graph: SceneGraph, pageId: string): string[] {
  const page = graph.getNode(pageId)
  if (!page) return []

  const ids: string[] = []
  for (const childId of page.childIds) {
    const child = graph.getNode(childId)
    if (!child?.visible) continue
    if (child.type === 'SECTION') {
      for (const nestedId of child.childIds) {
        const nested = graph.getNode(nestedId)
        if (nested?.visible) ids.push(nestedId)
      }
      continue
    }
    ids.push(childId)
  }
  return ids
}

function contentBounds(graph: SceneGraph, pageId: string) {
  return computeDescendantVisualBounds(
    collectCoverGraphicIds(graph, pageId),
    (nodeId) => graph.getNode(nodeId),
    (nodeId) => graph.getAbsolutePosition(nodeId)
  )
}

function clamp(value: number, min: number, max: number): number {
  if (max < min) return min
  return Math.min(max, Math.max(min, value))
}

/**
 * Cover at zoom 1: union of the first page's child graphics, width capped at
 * 1600, 4:3 crop centered on that bounds (clamped inside when the crop fits).
 */
export function computeCoverCapture(graph: SceneGraph, pageId: string): CoverCapture | null {
  const bounds = contentBounds(graph, pageId)
  if (!bounds) return null

  const contentW = bounds.maxX - bounds.minX
  const contentH = bounds.maxY - bounds.minY
  if (contentW <= 0 || contentH <= 0) return null

  const width = Math.max(1, Math.round(Math.min(contentW, COVER_MAX_WIDTH)))
  const height = Math.max(1, Math.round((width * COVER_ASPECT_HEIGHT) / COVER_ASPECT_WIDTH))
  const centerX = (bounds.minX + bounds.maxX) / 2
  const centerY = (bounds.minY + bounds.maxY) / 2
  let x = centerX - width / 2
  let y = centerY - height / 2
  if (width <= contentW) x = clamp(x, bounds.minX, bounds.maxX - width)
  if (height <= contentH) y = clamp(y, bounds.minY, bounds.maxY - height)

  return { x: Math.round(x), y: Math.round(y), width, height }
}
