import { parseLocalDateTime } from './dateTime'

export function statusText(status) {
  const labels = {
    TODO: '待办',
    IN_PROGRESS: '进行中',
    DONE: '已完成'
  }

  return labels[status] || status || '待办'
}

export function priorityText(priority) {
  const labels = {
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高'
  }

  return labels[priority] || priority || '中'
}

export function isTaskOverdue(task) {
  const due = parseLocalDateTime(task?.dueAt)

  return Boolean(due && task?.status !== 'DONE' && due.getTime() < Date.now())
}
