import { parseLocalDateTime } from './dateTime'

export function groupRoleLabel(role) {
  const labels = {
    OWNER: '负责人',
    ADMIN: '管理员',
    MEMBER: '成员'
  }

  return labels[role] || '成员'
}

export function groupInitial(name) {
  return String(name || '组').trim().slice(0, 1).toLocaleUpperCase() || '组'
}

export function formatGroupDate(value) {
  const date = parseLocalDateTime(value)

  if (!date) {
    return '暂无记录'
  }

  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

export function formatInvitationDate(value) {
  const date = parseLocalDateTime(value)

  if (!date) {
    return '刚刚收到'
  }

  return date.toLocaleString('zh-CN', {
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}
