const API_BASE = '/api'

async function request(path, options = {}) {
  const token = localStorage.getItem('aiTodoToken')
  const headers = {
    ...(options.body ? { 'Content-Type': 'application/json' } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {})
  }

  let response

  try {
    response = await fetch(`${API_BASE}${path}`, {
      headers,
      ...options
    })
  } catch {
    throw new Error('无法连接服务器，请检查网络后重试。')
  }

  const contentType = response.headers.get('content-type') || ''
  const isJson = contentType.includes('json')
  const responseText = await response.text()
  let data = responseText

  if (isJson && responseText) {
    try {
      data = JSON.parse(responseText)
    } catch {
      data = responseText
    }
  }

  if (!response.ok) {
    throw new Error(resolveErrorMessage(data, response.status))
  }

  return data
}

function resolveErrorMessage(data, status) {
  if (data && typeof data === 'object') {
    const validationMessage = resolveValidationMessage(data.errors)

    if (validationMessage) {
      return validationMessage
    }

    const candidates = [data.detail, data.reason, data.message, data.error]

    for (const candidate of candidates) {
      const message = normalizeErrorMessage(candidate)

      if (message && !isGenericHttpMessage(message, status)) {
        return message
      }
    }
  }

  const textMessage = normalizeErrorMessage(data)

  if (textMessage && !isGenericHttpMessage(textMessage, status)) {
    return textMessage
  }

  return fallbackStatusMessage(status)
}

function resolveValidationMessage(errors) {
  if (!Array.isArray(errors)) {
    return ''
  }

  const messages = errors
    .map((error) => {
      if (typeof error === 'string') {
        return normalizeErrorMessage(error)
      }

      return normalizeErrorMessage(error?.defaultMessage || error?.message || error?.reason)
    })
    .filter(Boolean)

  return [...new Set(messages)].join('；')
}

function normalizeErrorMessage(value) {
  if (typeof value !== 'string') {
    return ''
  }

  const message = value.trim()

  if (!message) {
    return ''
  }

  const quotedReason = message.match(/["“]([^"”]+)["”]\s*$/)

  return quotedReason?.[1]?.trim() || message
}

function isGenericHttpMessage(message, status) {
  const normalized = message.trim().toLowerCase().replaceAll('_', ' ')
  const genericMessages = new Set([
    String(status),
    `${status}`,
    'bad request',
    'unauthorized',
    'forbidden',
    'not found',
    'method not allowed',
    'too many requests',
    'internal server error',
    'service unavailable',
    'request failed',
    'invalid request content.',
    'validation failed'
  ])

  return genericMessages.has(normalized) || normalized === `${status} ${fallbackEnglishStatus(status)}`
}

function fallbackEnglishStatus(status) {
  const messages = {
    400: 'bad request',
    401: 'unauthorized',
    403: 'forbidden',
    404: 'not found',
    405: 'method not allowed',
    429: 'too many requests',
    500: 'internal server error',
    503: 'service unavailable'
  }

  return messages[status] || ''
}

function fallbackStatusMessage(status) {
  const messages = {
    400: '提交的信息有误，请检查后重试。',
    401: '登录状态已失效，请重新登录。',
    403: '当前账号无权执行此操作。',
    404: '请求的内容不存在或已被删除。',
    405: '当前操作暂不受支持。',
    429: '操作过于频繁，请稍后再试。',
    500: '服务器处理失败，请稍后重试。',
    502: 'AI 服务暂时没有返回有效结果，请稍后重试。',
    503: '服务暂时不可用，请稍后重试。'
  }

  return messages[status] || `请求失败，状态码：${status}`
}

export function registerUser(payload) {
  return request('/users/register', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function loginUser(payload) {
  return request('/users/login', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function getCurrentUser() {
  return request('/users/me')
}

export function listTasks(filters = {}) {
  const params = new URLSearchParams()

  if (filters.status) {
    params.set('status', filters.status)
  }

  if (filters.priority) {
    params.set('priority', filters.priority)
  }

  if (filters.keyword) {
    params.set('keyword', filters.keyword)
  }

  if (filters.page) {
    params.set('page', String(filters.page))
  }

  if (filters.size) {
    params.set('size', String(filters.size))
  }

  const query = params.toString()

  return request(query ? `/tasks?${query}` : '/tasks')
}

export function getTaskStats() {
  return request('/tasks/stats')
}

export function getTaskReminders(minutes = 60) {
  const params = new URLSearchParams({ minutes: String(minutes) })

  return request(`/tasks/reminders?${params.toString()}`)
}

export function getTask(id) {
  return request(`/tasks/${id}`)
}

export function createTask(payload) {
  return request('/tasks', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function updateTask(id, payload) {
  return request(`/tasks/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(payload)
  })
}

export function updateTaskStatus(id, payload) {
  return request(`/tasks/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify(payload)
  })
}

export function deleteTask(id) {
  return request(`/tasks/${id}`, {
    method: 'DELETE'
  })
}

export function listTaskSteps(taskId) {
  return request(`/tasks/${taskId}/steps`)
}

export function createTaskStep(taskId, payload) {
  return request(`/tasks/${taskId}/steps`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function createTaskStepsBatch(taskId, payload) {
  return request(`/tasks/${taskId}/steps/batch`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function updateTaskStep(taskId, stepId, payload) {
  return request(`/tasks/${taskId}/steps/${stepId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload)
  })
}

export function deleteTaskStep(taskId, stepId) {
  return request(`/tasks/${taskId}/steps/${stepId}`, {
    method: 'DELETE'
  })
}

export function getTaskAdvice(payload) {
  return request('/ai/task-advice', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function generateTaskStepDrafts(taskId, payload) {
  return request(`/ai/tasks/${taskId}/step-drafts`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function createGroup(payload) {
  return request('/groups', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function listGroups() {
  return request('/groups')
}

export function getGroup(groupId) {
  return request(`/groups/${groupId}`)
}

export function listGroupMembers(groupId) {
  return request(`/groups/${groupId}/members`)
}

export function createGroupInvitation(groupId, payload) {
  return request(`/groups/${groupId}/invitations`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function createGroupTask(groupId, payload) {
  return request(`/groups/${groupId}/tasks`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function listGroupTasks(groupId, page = 1, size = 10) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size)
  })

  return request(`/groups/${groupId}/tasks?${params.toString()}`)
}

export function getGroupTask(groupId, taskId) {
  return request(`/groups/${groupId}/tasks/${taskId}`)
}

export function updateGroupTask(groupId, taskId, payload) {
  return request(`/groups/${groupId}/tasks/${taskId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload)
  })
}

export function updateGroupTaskStatus(groupId, taskId, status) {
  return request(`/groups/${groupId}/tasks/${taskId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status })
  })
}

export function updateGroupTaskAssignee(groupId, taskId, assigneeId) {
  return request(`/groups/${groupId}/tasks/${taskId}/assignee`, {
    method: 'PATCH',
    body: JSON.stringify({ assigneeId })
  })
}

export function deleteGroupTask(groupId, taskId) {
  return request(`/groups/${groupId}/tasks/${taskId}`, {
    method: 'DELETE'
  })
}

export function listPendingGroupInvitations() {
  return request('/group-invitations/pending')
}

export function acceptGroupInvitation(invitationId) {
  return request(`/group-invitations/${invitationId}/accept`, {
    method: 'POST'
  })
}

export function rejectGroupInvitation(invitationId) {
  return request(`/group-invitations/${invitationId}/reject`, {
    method: 'POST'
  })
}

export function leaveGroup(groupId) {
  return request(`/groups/${groupId}/members/me`, {
    method: 'DELETE'
  })
}

export function updateGroupMemberRole(groupId, memberUserId, role) {
  return request(`/groups/${groupId}/members/${memberUserId}/role`, {
    method: 'PATCH',
    body: JSON.stringify({ role })
  })
}
