import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'

const STORAGE_KEY = 'aiTodoDetailPanelWidth'
const DEFAULT_WIDTH = 520
const MIN_WIDTH = 360
const MAX_WIDTH = 760

export function useResizablePanel() {
  const width = ref(readStoredWidth())
  const bounds = reactive({ min: MIN_WIDTH, max: MAX_WIDTH })
  const isResizing = ref(false)
  let resizeStartX = 0
  let resizeStartWidth = 0

  function readStoredWidth() {
    try {
      const storedWidth = Number(localStorage.getItem(STORAGE_KEY))

      return Number.isFinite(storedWidth) && storedWidth > 0 ? storedWidth : DEFAULT_WIDTH
    } catch {
      return DEFAULT_WIDTH
    }
  }

  function updateBounds() {
    const viewportWidth = window.innerWidth
    const sidebarWidth = viewportWidth > 1240 ? 240 : viewportWidth > 900 ? 82 : 0
    const minimumBoardWidth = viewportWidth > 1240 ? 500 : 0
    const availableWidth = viewportWidth - sidebarWidth - minimumBoardWidth
    const maximumWidth = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, availableWidth))

    bounds.min = Math.min(MIN_WIDTH, maximumWidth)
    bounds.max = maximumWidth

    if (viewportWidth > 680) {
      width.value = clampWidth(width.value)
    }
  }

  function clampWidth(value) {
    return Math.min(bounds.max, Math.max(bounds.min, Math.round(value)))
  }

  function persistWidth() {
    try {
      localStorage.setItem(STORAGE_KEY, String(width.value))
    } catch {
      // Resizing still works for the current session when storage is unavailable.
    }
  }

  function startResize(event) {
    if (window.innerWidth <= 680 || (event.pointerType === 'mouse' && event.button !== 0)) {
      return
    }

    event.preventDefault()
    updateBounds()
    resizeStartX = event.clientX
    resizeStartWidth = width.value
    isResizing.value = true
    document.body.classList.add('detail-resizing')
    window.addEventListener('pointermove', handleResize)
    window.addEventListener('pointerup', finishResize)
    window.addEventListener('pointercancel', finishResize)
  }

  function handleResize(event) {
    if (!isResizing.value) {
      return
    }

    width.value = clampWidth(resizeStartWidth + resizeStartX - event.clientX)
  }

  function finishResize() {
    if (isResizing.value) {
      persistWidth()
    }

    isResizing.value = false
    document.body.classList.remove('detail-resizing')
    window.removeEventListener('pointermove', handleResize)
    window.removeEventListener('pointerup', finishResize)
    window.removeEventListener('pointercancel', finishResize)
  }

  function resetWidth() {
    updateBounds()
    width.value = clampWidth(DEFAULT_WIDTH)
    persistWidth()
  }

  function handleResizeKeydown(event) {
    const resizeStep = event.shiftKey ? 64 : 24
    let nextWidth = width.value

    if (event.key === 'ArrowLeft') {
      nextWidth += resizeStep
    } else if (event.key === 'ArrowRight') {
      nextWidth -= resizeStep
    } else if (event.key === 'Home') {
      nextWidth = bounds.min
    } else if (event.key === 'End') {
      nextWidth = bounds.max
    } else {
      return
    }

    event.preventDefault()
    updateBounds()
    width.value = clampWidth(nextWidth)
    persistWidth()
  }

  onMounted(() => {
    window.addEventListener('resize', updateBounds)
    updateBounds()
  })

  onBeforeUnmount(() => {
    window.removeEventListener('resize', updateBounds)
    finishResize()
  })

  return {
    width,
    bounds,
    isResizing,
    startResize,
    resetWidth,
    handleResizeKeydown
  }
}
