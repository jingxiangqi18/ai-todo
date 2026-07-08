<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  CalendarDays,
  Inbox,
  ListTodo,
  LogOut,
  Plus,
  Search,
  Sparkles,
  Star,
  UserRound
} from '@lucide/vue'
import { createTask, getCurrentUser, listTasks, loginUser, registerUser } from './services/api'

const authMode = ref('login')
const user = ref(null)
const tasks = ref([])
const activeView = ref('all')
const query = ref('')
const errorMessage = ref('')
const successMessage = ref('')
const isBooting = ref(true)
const isAuthSubmitting = ref(false)
const isTaskSubmitting = ref(false)
const isDuePanelOpen = ref(false)

const authForm = reactive({
  account: '',
  username: '',
  email: '',
  password: ''
})

const taskForm = reactive({
  title: '',
  description: '',
  priority: 'MEDIUM',
  dueDate: '',
  dueTime: ''
})

const priorityOptions = [
  { value: 'LOW', label: '低', tone: 'priority-LOW' },
  { value: 'MEDIUM', label: '中', tone: 'priority-MEDIUM' },
  { value: 'HIGH', label: '高', tone: 'priority-HIGH' }
]

const dueButtonLabel = computed(() => {
  if (!taskForm.dueDate) {
    return '设置时间'
  }

  const time = taskForm.dueTime || '23:59'

  return `${formatShortDate(taskForm.dueDate)} ${time}`
})

const views = computed(() => [
  { key: 'all', label: '全部任务', icon: ListTodo, count: tasks.value.length },
  { key: 'today', label: '我的一天', icon: Sparkles, count: todayTasks.value.length },
  { key: 'planned', label: '计划内', icon: CalendarDays, count: plannedTasks.value.length },
  { key: 'important', label: '重要', icon: Star, count: importantTasks.value.length }
])

const todayKey = computed(() => toLocalDateKey(new Date()))
const todayTasks = computed(() => tasks.value.filter((task) => formatDateKey(task.dueAt) === todayKey.value))
const plannedTasks = computed(() => tasks.value.filter((task) => Boolean(task.dueAt)))
const importantTasks = computed(() => tasks.value.filter((task) => task.priority === 'HIGH'))

const visibleTasks = computed(() => {
  const source = resolveViewTasks()
  const keyword = query.value.trim().toLowerCase()

  if (!keyword) {
    return source
  }

  return source.filter((task) => {
    return `${task.title || ''} ${task.description || ''}`.toLowerCase().includes(keyword)
  })
})

const currentView = computed(() => views.value.find((item) => item.key === activeView.value) || views.value[0])
const taskStats = computed(() => {
  return {
    total: tasks.value.length,
    high: importantTasks.value.length,
    planned: plannedTasks.value.length
  }
})

const isLogin = computed(() => authMode.value === 'login')
const isAuthValid = computed(() => {
  if (isLogin.value) {
    return authForm.account.trim() && authForm.password.trim()
  }

  return (
    authForm.username.trim().length >= 3 &&
    authForm.username.trim().length <= 20 &&
    authForm.email.includes('@') &&
    authForm.password.length >= 6 &&
    authForm.password.length <= 50
  )
})

const isTaskValid = computed(() => taskForm.title.trim().length > 0 && taskForm.title.trim().length <= 100)

onMounted(async () => {
  const token = localStorage.getItem('aiTodoToken')

  if (!token) {
    isBooting.value = false
    return
  }

  try {
    user.value = await getCurrentUser()
    await refreshTasks()
  } catch (error) {
    localStorage.removeItem('aiTodoToken')
    errorMessage.value = error.message || '登录状态已失效，请重新登录。'
  } finally {
    isBooting.value = false
  }
})

async function handleAuthSubmit() {
  errorMessage.value = ''
  successMessage.value = ''

  if (!isAuthValid.value) {
    errorMessage.value = isLogin.value ? '请输入账号和密码。' : '请按后端校验规则填写注册信息。'
    return
  }

  isAuthSubmitting.value = true

  try {
    if (isLogin.value) {
      const result = await loginUser({
        account: authForm.account.trim(),
        password: authForm.password
      })

      localStorage.setItem('aiTodoToken', result.token)
      user.value = result.user
      await refreshTasks()
    } else {
      await registerUser({
        username: authForm.username.trim(),
        email: authForm.email.trim(),
        password: authForm.password
      })

      authMode.value = 'login'
      authForm.account = authForm.username.trim()
      authForm.password = ''
      successMessage.value = '注册成功，请登录。'
    }
  } catch (error) {
    errorMessage.value = error.message || '请求失败，请稍后重试。'
  } finally {
    isAuthSubmitting.value = false
  }
}

async function handleCreateTask() {
  errorMessage.value = ''
  successMessage.value = ''

  if (!isTaskValid.value) {
    errorMessage.value = '任务标题不能为空，且不能超过 100 个字符。'
    return
  }

  isTaskSubmitting.value = true

  try {
    const created = await createTask({
      title: taskForm.title.trim(),
      description: taskForm.description.trim() || null,
      priority: taskForm.priority,
      dueAt: resolveDueAt()
    })

    tasks.value = [created, ...tasks.value]
    taskForm.title = ''
    taskForm.description = ''
    taskForm.priority = 'MEDIUM'
    taskForm.dueDate = ''
    taskForm.dueTime = ''
    isDuePanelOpen.value = false
    successMessage.value = '任务已添加。'
  } catch (error) {
    errorMessage.value = error.message || '创建任务失败。'
  } finally {
    isTaskSubmitting.value = false
  }
}

async function refreshTasks() {
  tasks.value = await listTasks()
}

function switchAuthMode(mode) {
  authMode.value = mode
  errorMessage.value = ''
  successMessage.value = ''
}

function logout() {
  localStorage.removeItem('aiTodoToken')
  user.value = null
  tasks.value = []
  authForm.password = ''
  errorMessage.value = ''
  successMessage.value = ''
}

function resolveViewTasks() {
  if (activeView.value === 'today') {
    return todayTasks.value
  }

  if (activeView.value === 'planned') {
    return plannedTasks.value
  }

  if (activeView.value === 'important') {
    return importantTasks.value
  }

  return tasks.value
}

function formatDateKey(value) {
  return value ? String(value).slice(0, 10) : ''
}

function toLocalDateKey(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function formatDueAt(value) {
  if (!value) {
    return '无截止时间'
  }

  return value.replace('T', ' ').slice(0, 16)
}

function formatDateLabel(value) {
  if (!value) {
    return '未设置日期'
  }

  const [year, month, day] = value.split('-')

  return `${year}年${month}月${day}日`
}

function formatTimeLabel(value) {
  return value || '默认 23:59'
}

function formatShortDate(value) {
  if (!value) {
    return ''
  }

  const [, month, day] = value.split('-')

  return `${month}/${day}`
}

function resolveDueAt() {
  if (!taskForm.dueDate) {
    return null
  }

  return `${taskForm.dueDate}T${taskForm.dueTime || '23:59'}`
}

function setDueToday() {
  taskForm.dueDate = todayKey.value
  taskForm.dueTime = taskForm.dueTime || '18:00'
}

function setDueTomorrow() {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  taskForm.dueDate = toLocalDateKey(tomorrow)
  taskForm.dueTime = taskForm.dueTime || '18:00'
}

function clearDue() {
  taskForm.dueDate = ''
  taskForm.dueTime = ''
}

function priorityText(priority) {
  const map = {
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高'
  }

  return map[priority] || priority || '中'
}
</script>

<template>
  <main v-if="isBooting" class="loading-screen">
    <div class="loader"></div>
  </main>

  <main v-else-if="!user" class="auth-shell">
    <section class="auth-card">
      <div class="auth-visual">
        <p class="brand">AI Todo</p>
        <h1>清晰整理今天要做的事</h1>
        <div class="preview-list">
          <div class="preview-item strong">
            <span></span>
            <p>完成 Spring Boot 任务接口联调</p>
          </div>
          <div class="preview-item">
            <span></span>
            <p>整理明天的学习计划</p>
          </div>
          <div class="preview-item">
            <span></span>
            <p>记录一个 AI 功能想法</p>
          </div>
        </div>
      </div>

      <form class="auth-form" @submit.prevent="handleAuthSubmit">
        <div class="auth-tabs" role="tablist">
          <button type="button" :class="{ active: isLogin }" @click="switchAuthMode('login')">
            登录
          </button>
          <button type="button" :class="{ active: !isLogin }" @click="switchAuthMode('register')">
            注册
          </button>
        </div>

        <div class="form-heading">
          <h2>{{ isLogin ? '欢迎回来' : '创建账户' }}</h2>
          <p>{{ isLogin ? '登录后查看和创建你的任务。' : '注册成功后再使用账号登录。' }}</p>
        </div>

        <label v-if="isLogin" class="field">
          <span>账号</span>
          <input v-model="authForm.account" type="text" autocomplete="username" placeholder="用户名或邮箱" />
        </label>

        <template v-else>
          <label class="field">
            <span>用户名</span>
            <input v-model="authForm.username" type="text" autocomplete="username" placeholder="3 到 20 个字符" />
          </label>

          <label class="field">
            <span>邮箱</span>
            <input v-model="authForm.email" type="email" autocomplete="email" placeholder="name@example.com" />
          </label>
        </template>

        <label class="field">
          <span>密码</span>
          <input
            v-model="authForm.password"
            :autocomplete="isLogin ? 'current-password' : 'new-password'"
            type="password"
            placeholder="至少 6 个字符"
          />
        </label>

        <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>
        <p v-if="successMessage" class="notice success">{{ successMessage }}</p>

        <button class="primary-button" type="submit" :disabled="isAuthSubmitting || !isAuthValid">
          {{ isAuthSubmitting ? '提交中...' : isLogin ? '登录' : '注册' }}
        </button>
      </form>
    </section>
  </main>

  <main v-else class="todo-app">
    <aside class="sidebar">
      <div class="account-box">
        <div class="avatar">
          <UserRound :size="20" />
        </div>
        <div>
          <strong>{{ user.username }}</strong>
          <span>{{ user.email }}</span>
        </div>
      </div>

      <nav class="nav-list" aria-label="任务视图">
        <button
          v-for="view in views"
          :key="view.key"
          type="button"
          :class="{ active: activeView === view.key }"
          @click="activeView = view.key"
        >
          <component :is="view.icon" :size="18" />
          <span>{{ view.label }}</span>
          <em>{{ view.count }}</em>
        </button>
      </nav>

      <div class="sidebar-footer">
        <button class="ghost-button" type="button" @click="logout">
          <LogOut :size="17" />
          <span>退出登录</span>
        </button>
      </div>
    </aside>

    <section class="task-board">
      <header class="board-header">
        <div>
          <p class="date-line">{{ new Date().toLocaleDateString('zh-CN', { weekday: 'long', month: 'long', day: 'numeric' }) }}</p>
          <h1>{{ currentView.label }}</h1>
        </div>

        <div class="search-box">
          <Search :size="18" />
          <input v-model="query" type="search" placeholder="搜索任务" />
        </div>
      </header>

      <div class="stat-strip">
        <div>
          <span>{{ taskStats.total }}</span>
          <p>全部</p>
        </div>
        <div>
          <span>{{ taskStats.planned }}</span>
          <p>有计划</p>
        </div>
        <div>
          <span>{{ taskStats.high }}</span>
          <p>高优先级</p>
        </div>
      </div>

      <form class="task-composer" @submit.prevent="handleCreateTask">
        <div class="composer-main">
          <Plus :size="20" />
          <input v-model="taskForm.title" type="text" maxlength="100" placeholder="添加任务" />
        </div>

        <div class="composer-options">
          <input v-model="taskForm.description" type="text" maxlength="100" placeholder="描述，最多 100 个字符" />
          <div class="priority-segment" aria-label="优先级">
            <button
              v-for="option in priorityOptions"
              :key="option.value"
              type="button"
              :class="[{ active: taskForm.priority === option.value }, option.tone]"
              @click="taskForm.priority = option.value"
            >
              {{ option.label }}
            </button>
          </div>
          <div class="due-menu" :class="{ open: isDuePanelOpen }">
            <button class="due-button" type="button" @click="isDuePanelOpen = !isDuePanelOpen">
              <CalendarDays :size="17" />
              <span>{{ dueButtonLabel }}</span>
            </button>

            <div v-if="isDuePanelOpen" class="due-popover">
              <div class="due-popover-header">
                <strong>截止时间</strong>
                <button type="button" @click="clearDue">清除</button>
              </div>

              <div class="due-presets">
                <button type="button" @click="setDueToday">今天</button>
                <button type="button" @click="setDueTomorrow">明天</button>
              </div>

              <label class="due-field">
                <span>日期</span>
                <input v-model="taskForm.dueDate" type="date" />
              </label>

              <label class="due-field">
                <span>时间</span>
                <input v-model="taskForm.dueTime" type="time" />
              </label>

              <div class="due-summary">
                {{ formatDateLabel(taskForm.dueDate) }} · {{ formatTimeLabel(taskForm.dueTime) }}
              </div>

              <button class="due-done" type="button" @click="isDuePanelOpen = false">完成</button>
            </div>
          </div>
          <button class="primary-button compact" type="submit" :disabled="isTaskSubmitting || !isTaskValid">
            {{ isTaskSubmitting ? '添加中...' : '添加' }}
          </button>
        </div>
      </form>

      <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>
      <p v-if="successMessage" class="notice success">{{ successMessage }}</p>

      <div class="task-list">
        <article v-for="task in visibleTasks" :key="task.id" class="task-item">
          <div class="task-check" aria-hidden="true"></div>
          <div class="task-content">
            <div class="task-title-row">
              <h2>{{ task.title }}</h2>
              <span class="priority-pill" :class="`priority-${task.priority || 'MEDIUM'}`">
                {{ priorityText(task.priority) }}
              </span>
            </div>
            <p v-if="task.description">{{ task.description }}</p>
            <div class="task-meta">
              <span><CalendarDays :size="15" /> {{ formatDueAt(task.dueAt) }}</span>
              <span><Inbox :size="15" /> {{ task.status }}</span>
            </div>
          </div>
        </article>

        <section v-if="visibleTasks.length === 0" class="empty-panel">
          <ListTodo :size="34" />
          <h2>这里还没有任务</h2>
          <p>使用上方输入框添加第一条任务。</p>
        </section>
      </div>
    </section>
  </main>
</template>
