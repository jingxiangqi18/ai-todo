const API_BASE = '/api'

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  })

  const contentType = response.headers.get('content-type') || ''
  const isJson = contentType.includes('application/json')
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
