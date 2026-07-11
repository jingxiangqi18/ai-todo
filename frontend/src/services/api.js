const API_BASE = '/api'

async function request(path, options = {}) {
  const token = localStorage.getItem('aiTodoToken')
  const headers = {
    ...(options.body ? { 'Content-Type': 'application/json' } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {})
  }

  const response = await fetch(`${API_BASE}${path}`, {
    headers,
    ...options
  })

  const contentType = response.headers.get('content-type') || ''
  const isJson = contentType.includes('json')
  const data = isJson ? await response.json() : await response.text()

  if (!response.ok) {
    throw new Error(resolveErrorMessage(data, response.status))
  }

  return data
}

function resolveErrorMessage(data, status) {
  if (data && typeof data === 'object') {
    return data.message || data.error || data.detail || `请求失败，状态码：${status}`
  }

  return data || `请求失败，状态码：${status}`
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
