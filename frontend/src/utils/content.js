export function parseAdvice(content) {
  if (!content?.trim()) {
    return []
  }

  const blocks = []
  let activeList = null

  const flushList = () => {
    if (activeList) {
      blocks.push(activeList)
      activeList = null
    }
  }

  for (const rawLine of content.split(/\r?\n/)) {
    const line = rawLine.trim()

    if (!line) {
      flushList()
      continue
    }

    const heading = line.match(/^#{1,3}\s+(.+)$/)
    const orderedItem = line.match(/^\d+[.、]\s*(.+)$/)
    const unorderedItem = line.match(/^[-*•]\s+(.+)$/)

    if (heading) {
      flushList()
      blocks.push({ type: 'heading', text: cleanAdviceText(heading[1]) })
      continue
    }

    if (orderedItem || unorderedItem) {
      const type = orderedItem ? 'ordered' : 'unordered'
      const text = cleanAdviceText((orderedItem || unorderedItem)[1])

      if (!activeList || activeList.type !== type) {
        flushList()
        activeList = { type, items: [] }
      }

      activeList.items.push(text)
      continue
    }

    flushList()
    blocks.push({ type: 'paragraph', text: cleanAdviceText(line) })
  }

  flushList()

  return blocks
}

function cleanAdviceText(text) {
  return text
    .replace(/\*\*(.*?)\*\*/g, '$1')
    .replace(/__(.*?)__/g, '$1')
    .replace(/`([^`]+)`/g, '$1')
}
