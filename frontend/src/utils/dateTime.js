export const END_OF_DAY_TIME = '23:59'

export function formatDateKey(value) {
  return value ? String(value).slice(0, 10) : ''
}

export function toLocalDateKey(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

export function parseLocalDateTime(value) {
  if (!value) {
    return null
  }

  const date = new Date(String(value))

  return Number.isNaN(date.getTime()) ? null : date
}

export function formatTaskDateTime(value) {
  const date = parseLocalDateTime(value)

  if (!date) {
    return '未安排'
  }

  const datePart = date.toLocaleDateString('zh-CN', {
    month: 'long',
    day: 'numeric',
    weekday: 'short'
  })
  const timePart = date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })

  return `${datePart} ${timePart}`
}

export function formatDueAt(value) {
  return value ? formatTaskDateTime(value) : '未安排'
}

export function formatFullDateTime(value) {
  const date = parseLocalDateTime(value)

  if (!date) {
    return '暂无记录'
  }

  const datePart = date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'short'
  })
  const timePart = date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })

  return `${datePart} ${timePart}`
}

export function formatDateLabel(value) {
  if (!value) {
    return '未设置日期'
  }

  const [year, month, day] = value.split('-')

  return `${year}年${month}月${day}日`
}

export function formatTimeLabel(value) {
  return value || '默认 23:59'
}

export function formatShortDate(date) {
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

export function getNextWeekEndDate() {
  const nextWeekEnd = new Date()
  const currentDay = nextWeekEnd.getDay()
  const daysUntilNextWeekEnd = currentDay === 0 ? 7 : 14 - currentDay
  nextWeekEnd.setDate(nextWeekEnd.getDate() + daysUntilNextWeekEnd)
  return nextWeekEnd
}

export function resolveDuePresetDate(preset) {
  const date = new Date()

  if (preset === 'tomorrow') {
    date.setDate(date.getDate() + 1)
  } else if (preset === 'next-week') {
    return getNextWeekEndDate()
  }

  return date
}

export function resolveDuePartValues(parts) {
  const { year, month, day, hour, minute } = parts
  const hasDatePart = Boolean(year || month || day)
  const hasTimePart = Boolean(hour || minute)

  if (!hasDatePart) {
    return hasTimePart
      ? { error: '请先填写截止日期。', date: '', time: '' }
      : { error: '', date: '', time: '' }
  }

  if (year.length !== 4 || !month || !day) {
    return { error: '请完整填写截止日期。', date: '', time: '' }
  }

  const yearNumber = Number(year)
  const monthNumber = Number(month)
  const dayNumber = Number(day)
  const candidate = new Date(yearNumber, monthNumber - 1, dayNumber)
  const isValidDate =
    yearNumber >= 1000 &&
    monthNumber >= 1 &&
    monthNumber <= 12 &&
    dayNumber >= 1 &&
    dayNumber <= 31 &&
    candidate.getFullYear() === yearNumber &&
    candidate.getMonth() === monthNumber - 1 &&
    candidate.getDate() === dayNumber

  if (!isValidDate) {
    return { error: '截止日期无效，请重新填写。', date: '', time: '' }
  }

  if (hasTimePart && (!hour || !minute)) {
    return { error: '请完整填写截止时间。', date: '', time: '' }
  }

  const hourNumber = hasTimePart ? Number(hour) : 23
  const minuteNumber = hasTimePart ? Number(minute) : 59

  if (hourNumber < 0 || hourNumber > 23 || minuteNumber < 0 || minuteNumber > 59) {
    return { error: '截止时间无效，请重新填写。', date: '', time: '' }
  }

  return {
    error: '',
    date: `${year}-${String(monthNumber).padStart(2, '0')}-${String(dayNumber).padStart(2, '0')}`,
    time: `${String(hourNumber).padStart(2, '0')}:${String(minuteNumber).padStart(2, '0')}`
  }
}

export function splitDateParts(value) {
  const [year = '', month = '', day = ''] = value ? value.split('-') : []

  return { year, month, day }
}

export function splitTimeParts(value) {
  const [hour = '', minute = ''] = value ? value.split(':') : []

  return { hour, minute }
}
