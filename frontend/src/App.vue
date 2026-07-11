<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Bell,
  CalendarDays,
  Check,
  ChevronLeft,
  ChevronRight,
  Circle,
  Inbox,
  ListTodo,
  LogOut,
  Plus,
  RefreshCw,
  Save,
  Search,
  SlidersHorizontal,
  Sparkles,
  Star,
  Trash2,
  X,
  UserRound
} from '@lucide/vue'
import {
  createTask,
  deleteTask,
  getCurrentUser,
  getTask,
  getTaskReminders,
  getTaskStats,
  listTasks,
  loginUser,
  registerUser,
  updateTask,
  updateTaskStatus
} from './services/api'

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
const isComposerOpen = ref(false)
const isFilterOpen = ref(false)
const isReminderOpen = ref(false)
const selectedTask = ref(null)
const reminders = ref([])
const detailError = ref('')
const isDetailLoading = ref(false)
const isDetailSaving = ref(false)

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

const editForm = reactive({
  title: '',
  description: '',
  priority: 'MEDIUM',
  dueDate: '',
  dueTime: ''
})

const listFilters = reactive({
  status: '',
  priority: ''
})

const taskPage = reactive({
  page: 1,
  size: 10,
  total: 0,
  pages: 1
})

const taskStats = reactive({
  total: 0,
  todo: 0,
  inProgress: 0,
  done: 0,
  highPriority: 0,
  dueToday: 0,
  overdue: 0
})

const editDateParts = computed(() => splitDateParts(editForm.dueDate))
const editTimeParts = computed(() => splitTimeParts(editForm.dueTime))

const priorityOptions = [
  { value: 'LOW', label: '低', tone: 'priority-LOW' },
  { value: 'MEDIUM', label: '中', tone: 'priority-MEDIUM' },
  { value: 'HIGH', label: '高', tone: 'priority-HIGH' }
]

const statusOptions = [
  { value: 'TODO', label: '待办' },
  { value: 'IN_PROGRESS', label: '进行中' },
  { value: 'DONE', label: '已完成' }
]

const dueButtonLabel = computed(() => {
  if (!taskForm.dueDate) {
    return '设置时间'
  }

  const time = taskForm.dueTime || '23:59'

  return `${formatShortDate(taskForm.dueDate)} ${time}`
})

const views = computed(() => [
  { key: 'all', label: '全部任务', icon: ListTodo, count: taskStats.total },
  { key: 'today', label: '我的一天', icon: Sparkles, count: taskStats.dueToday },
  { key: 'planned', label: '计划内', icon: CalendarDays, count: Math.max(0, taskStats.total - taskStats.done) },
  { key: 'important', label: '重要', icon: Star, count: taskStats.highPriority },
  { key: 'progress', label: '进行中', icon: RefreshCw, count: taskStats.inProgress },
  { key: 'done', label: '已完成', icon: Check, count: taskStats.done }
])

const todayKey = computed(() => toLocalDateKey(new Date()))
const activeTasks = computed(() => tasks.value.filter((task) => task.status !== 'DONE'))
const todayTasks = computed(() => activeTasks.value.filter((task) => formatDateKey(task.dueAt) === todayKey.value))
const plannedTasks = computed(() => activeTasks.value.filter((task) => Boolean(task.dueAt)))
const importantTasks = computed(() => activeTasks.value.filter((task) => task.priority === 'HIGH'))
const inProgressTasks = computed(() => tasks.value.filter((task) => task.status === 'IN_PROGRESS'))
const doneTasks = computed(() => tasks.value.filter((task) => task.status === 'DONE'))

const visibleTasks = computed(() => {
  const source = resolveViewTasks()
  return source
})

const currentView = computed(() => views.value.find((item) => item.key === activeView.value) || views.value[0])
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
const hasListFilters = computed(() => Boolean(listFilters.status || listFilters.priority))
const hasServerQuery = computed(() => Boolean(listFilters.status || listFilters.priority || query.value.trim()))

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
    taskPage.total += 1
    taskPage.pages = Math.max(1, Math.ceil(taskPage.total / taskPage.size))
    taskForm.title = ''
    taskForm.description = ''
    taskForm.priority = 'MEDIUM'
    taskForm.dueDate = ''
    taskForm.dueTime = ''
    isDuePanelOpen.value = false
    isComposerOpen.value = false
    await refreshTaskStats()
    await refreshTaskReminders()
  } catch (error) {
    errorMessage.value = error.message || '创建任务失败。'
  } finally {
    isTaskSubmitting.value = false
  }
}

async function refreshTasks() {
  const result = await listTasks({
    status: listFilters.status,
    priority: listFilters.priority,
    keyword: query.value.trim(),
    page: taskPage.page,
    size: taskPage.size
  })

  if (Array.isArray(result)) {
    tasks.value = result
    taskPage.page = 1
    taskPage.size = result.length || 10
    taskPage.total = result.length
    taskPage.pages = 1
  } else {
    tasks.value = result.records || []
    taskPage.page = result.page || 1
    taskPage.size = result.size || taskPage.size
    taskPage.total = result.total || 0
    taskPage.pages = result.pages || 1
  }

  if (selectedTask.value) {
    const latest = tasks.value.find((task) => task.id === selectedTask.value.id)
    selectedTask.value = latest || null
  }

  await refreshTaskStats()
  await refreshTaskReminders()
}

async function refreshTaskStats() {
  try {
    Object.assign(taskStats, await getTaskStats())
  } catch {
    Object.assign(taskStats, {
      total: taskPage.total,
      todo: activeTasks.value.filter((task) => task.status === 'TODO').length,
      inProgress: inProgressTasks.value.length,
      done: doneTasks.value.length,
      highPriority: importantTasks.value.length,
      dueToday: todayTasks.value.length,
      overdue: 0
    })
  }
}

async function refreshTaskReminders() {
  try {
    const result = await getTaskReminders(60)
    reminders.value = Array.isArray(result) ? result : []
  } catch {
    reminders.value = []
  }
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
  reminders.value = []
  Object.assign(taskStats, {
    total: 0,
    todo: 0,
    inProgress: 0,
    done: 0,
    highPriority: 0,
    dueToday: 0,
    overdue: 0
  })
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

  if (activeView.value === 'progress') {
    return inProgressTasks.value
  }

  if (activeView.value === 'done') {
    return doneTasks.value
  }

  return tasks.value
}

async function selectView(key) {
  activeView.value = key
  taskPage.page = 1
  query.value = ''

  if (key === 'important') {
    listFilters.status = ''
    listFilters.priority = 'HIGH'
  } else if (key === 'progress') {
    listFilters.status = 'IN_PROGRESS'
    listFilters.priority = ''
  } else if (key === 'done') {
    listFilters.status = 'DONE'
    listFilters.priority = ''
  } else {
    listFilters.status = ''
    listFilters.priority = ''
  }

  await refreshTasks()
}

async function setListStatus(status) {
  listFilters.status = status
  activeView.value = 'all'
  taskPage.page = 1
  await refreshTasks()
  isFilterOpen.value = false
}

async function setListPriority(priority) {
  listFilters.priority = priority
  activeView.value = 'all'
  taskPage.page = 1
  await refreshTasks()
  isFilterOpen.value = false
}

async function clearListFilters() {
  listFilters.status = ''
  listFilters.priority = ''
  query.value = ''
  activeView.value = 'all'
  taskPage.page = 1
  await refreshTasks()
  isFilterOpen.value = false
}

async function applyKeywordSearch() {
  activeView.value = 'all'
  taskPage.page = 1
  await refreshTasks()
}

async function changePage(page) {
  if (page < 1 || page > taskPage.pages || page === taskPage.page) {
    return
  }

  taskPage.page = page
  await refreshTasks()
}

function changePageSize(size) {
  if (taskPage.size === size) {
    return
  }

  taskPage.size = size
  taskPage.page = 1
  return refreshTasks()
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
    return '未安排'
  }

  return formatTaskDateTime(value)
}

function formatTaskDateTime(value) {
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

function formatFullDateTime(value) {
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

function parseLocalDateTime(value) {
  if (!value) {
    return null
  }

  const date = new Date(String(value))

  return Number.isNaN(date.getTime()) ? null : date
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

function setEditDueToday() {
  editForm.dueDate = todayKey.value
  editForm.dueTime = editForm.dueTime || '18:00'
}

function setEditDueTomorrow() {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  editForm.dueDate = toLocalDateKey(tomorrow)
  editForm.dueTime = editForm.dueTime || '18:00'
}

function updateEditDatePart(part, value) {
  const current = splitDateParts(editForm.dueDate)
  const fallback = splitDateParts(todayKey.value)
  const next = {
    year: current.year || fallback.year,
    month: current.month || fallback.month,
    day: current.day || fallback.day,
    [part]: value
  }

  if (!next.year || !next.month || !next.day) {
    editForm.dueDate = ''
    return
  }

  editForm.dueDate = `${String(next.year).padStart(4, '0')}-${String(next.month).padStart(2, '0')}-${String(next.day).padStart(2, '0')}`
}

function updateEditTimePart(part, value) {
  const current = splitTimeParts(editForm.dueTime)
  const next = {
    hour: current.hour || '18',
    minute: current.minute || '00',
    [part]: value
  }

  editForm.dueTime = `${String(next.hour).padStart(2, '0')}:${String(next.minute).padStart(2, '0')}`
}

function splitDateParts(value) {
  const [year = '', month = '', day = ''] = value ? value.split('-') : []

  return { year, month, day }
}

function splitTimeParts(value) {
  const [hour = '', minute = ''] = value ? value.split(':') : []

  return { hour, minute }
}

async function openTaskDetail(task) {
  selectedTask.value = task
  isReminderOpen.value = false
  detailError.value = ''
  fillEditForm(task)
  isDetailLoading.value = true

  try {
    const latest = await getTask(task.id)
    selectedTask.value = latest
    fillEditForm(latest)
  } catch (error) {
    detailError.value = error.message || '读取任务详情失败。'
  } finally {
    isDetailLoading.value = false
  }
}

async function handleUpdateTask() {
  if (!selectedTask.value) {
    return
  }

  detailError.value = ''

  if (!editForm.title.trim()) {
    detailError.value = '任务标题不能为空。'
    return
  }

  isDetailSaving.value = true

  try {
    const payload = {
      title: editForm.title.trim(),
      description: editForm.description.trim(),
      priority: editForm.priority
    }

    if (editForm.dueDate) {
      payload.dueAt = `${editForm.dueDate}T${editForm.dueTime || '23:59'}`
    }

    const updated = await updateTask(selectedTask.value.id, payload)
    selectedTask.value = updated
    fillEditForm(updated)
    upsertTask(updated)
    await refreshTaskStats()
    await refreshTaskReminders()
  } catch (error) {
    detailError.value = error.message || '更新任务失败。'
  } finally {
    isDetailSaving.value = false
  }
}

async function handleDeleteTask() {
  if (!selectedTask.value) {
    return
  }

  detailError.value = ''

  try {
    const taskId = selectedTask.value.id
    await deleteTask(taskId)
    tasks.value = tasks.value.filter((task) => task.id !== taskId)
    closeTaskDetail()
    await refreshTaskStats()
    await refreshTaskReminders()
  } catch (error) {
    detailError.value = error.message || '删除任务失败。'
  }
}

async function handleStatusChange(task, status) {
  if (!task || task.status === status) {
    return
  }

  errorMessage.value = ''
  detailError.value = ''

  try {
    const updated = await updateTaskStatus(task.id, { status })
    upsertTask(updated)

    if (selectedTask.value?.id === updated.id) {
      selectedTask.value = updated
      fillEditForm(updated)
    }

    await refreshTaskStats()
    await refreshTaskReminders()

  } catch (error) {
    const message = error.message || '更新任务状态失败。'

    if (selectedTask.value?.id === task.id) {
      detailError.value = message
    } else {
      errorMessage.value = message
    }
  }
}

function toggleTaskDone(task) {
  const nextStatus = task.status === 'DONE' ? 'TODO' : 'DONE'

  return handleStatusChange(task, nextStatus)
}

function fillEditForm(task) {
  editForm.title = task?.title || ''
  editForm.description = task?.description || ''
  editForm.priority = task?.priority || 'MEDIUM'

  const due = splitDueAt(task?.dueAt)
  editForm.dueDate = due.date
  editForm.dueTime = due.time
}

function splitDueAt(value) {
  if (!value) {
    return { date: '', time: '' }
  }

  const [date, time = ''] = String(value).split('T')

  return {
    date,
    time: time.slice(0, 5)
  }
}

function upsertTask(task) {
  const index = tasks.value.findIndex((item) => item.id === task.id)

  if (index === -1) {
    tasks.value = [task, ...tasks.value]
    return
  }

  tasks.value = [task, ...tasks.value.filter((item) => item.id !== task.id)]
}

function closeTaskDetail() {
  selectedTask.value = null
  detailError.value = ''
}

function openComposer() {
  isComposerOpen.value = true
  isDuePanelOpen.value = false
  isFilterOpen.value = false
  isReminderOpen.value = false
}

function closeComposer() {
  isComposerOpen.value = false
  isDuePanelOpen.value = false
}

function isTaskOverdue(task) {
  const due = parseLocalDateTime(task?.dueAt)

  return Boolean(due && task.status !== 'DONE' && due.getTime() < Date.now())
}

function statusText(status) {
  const option = statusOptions.find((item) => item.value === status)

  return option?.label || status || '待办'
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

  <main v-else class="todo-app" :class="{ 'has-detail': selectedTask }">
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
          @click="selectView(view.key)"
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
        <div class="board-title">
          <p class="date-line">{{ new Date().toLocaleDateString('zh-CN', { weekday: 'long', month: 'long', day: 'numeric' }) }}</p>
          <h1>{{ currentView.label }}</h1>
        </div>

        <div class="board-actions">
          <div class="search-box search-box-compact">
            <Search :size="17" />
            <input v-model="query" type="search" placeholder="搜索任务" @keyup.enter="applyKeywordSearch" />
            <button type="button" aria-label="搜索任务" @click="applyKeywordSearch">搜索</button>
          </div>

          <div class="reminder-menu">
            <button
              class="icon-tool"
              type="button"
              aria-label="查看即将到期任务"
              title="未来 60 分钟"
              @click="isReminderOpen = !isReminderOpen; isFilterOpen = false"
            >
              <Bell :size="17" />
              <b v-if="reminders.length">{{ reminders.length }}</b>
            </button>

            <div v-if="isReminderOpen" class="reminder-popover">
              <div class="reminder-heading">
                <div>
                  <span>即将到期</span>
                  <strong>未来 60 分钟</strong>
                </div>
                <Bell :size="17" />
              </div>

              <button
                v-for="reminder in reminders"
                :key="reminder.id"
                class="reminder-item"
                type="button"
                @click="openTaskDetail(reminder)"
              >
                <span>{{ reminder.title }}</span>
                <time>{{ formatDueAt(reminder.dueAt) }}</time>
              </button>

              <p v-if="!reminders.length" class="reminder-empty">这段时间很从容，没有临近截止的任务。</p>
            </div>
          </div>

          <div class="filter-menu">
            <button
              class="tool-button"
              type="button"
              :class="{ active: isFilterOpen }"
              @click="isFilterOpen = !isFilterOpen; isReminderOpen = false"
            >
              <SlidersHorizontal :size="17" />
              <span>筛选</span>
              <b v-if="hasListFilters">{{ Number(Boolean(listFilters.status)) + Number(Boolean(listFilters.priority)) }}</b>
            </button>

            <div v-if="isFilterOpen" class="filter-popover">
              <div class="filter-popover-heading">
                <strong>筛选任务</strong>
                <button v-if="hasServerQuery" type="button" @click="clearListFilters">清除</button>
              </div>

              <div class="filter-section">
                <span>状态</span>
                <div class="filter-group">
                  <button type="button" :class="{ active: !listFilters.status }" @click="setListStatus('')">全部</button>
                  <button
                    v-for="option in statusOptions"
                    :key="option.value"
                    type="button"
                    :class="{ active: listFilters.status === option.value }"
                    @click="setListStatus(option.value)"
                  >
                    {{ option.label }}
                  </button>
                </div>
              </div>

              <div class="filter-section priority-filter-section">
                <span>优先级</span>
                <div class="filter-group">
                  <button type="button" :class="{ active: !listFilters.priority }" @click="setListPriority('')">全部</button>
                  <button
                    v-for="option in priorityOptions"
                    :key="option.value"
                    type="button"
                    :class="{ active: listFilters.priority === option.value }"
                    @click="setListPriority(option.value)"
                  >
                    {{ option.label }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <button class="primary-button create-trigger" type="button" @click="openComposer">
            <Plus :size="17" />
            <span>新建任务</span>
          </button>
        </div>
      </header>

      <div class="board-summary" aria-label="任务概览">
        <span><b>{{ taskStats.total }}</b> 全部</span>
        <span><b>{{ taskStats.todo }}</b> 待办</span>
        <span><b>{{ taskStats.dueToday }}</b> 今天截止</span>
        <span v-if="taskStats.overdue" class="summary-overdue"><b>{{ taskStats.overdue }}</b> 已逾期</span>
      </div>

      <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>
      <div class="task-list">
        <article
          v-for="task in visibleTasks"
          :key="task.id"
          class="task-item"
          :class="{ selected: selectedTask?.id === task.id, done: task.status === 'DONE' }"
          @click="openTaskDetail(task)"
        >
          <button
            class="task-check"
            type="button"
            :class="{ done: task.status === 'DONE' }"
            :aria-label="task.status === 'DONE' ? '恢复任务' : '完成任务'"
            @click.stop="toggleTaskDone(task)"
          >
            <Check v-if="task.status === 'DONE'" :size="14" />
          </button>
          <div class="task-content">
            <div class="task-title-row">
              <h2>{{ task.title }}</h2>
              <span class="task-due" :class="{ overdue: isTaskOverdue(task) }">
                <CalendarDays :size="14" />
                {{ formatDueAt(task.dueAt) }}
              </span>
            </div>
          </div>
        </article>

        <section v-if="visibleTasks.length === 0" class="empty-panel">
          <ListTodo :size="34" />
          <h2>这里还没有任务</h2>
          <p>使用右上角的新建任务开始规划。</p>
        </section>
      </div>

      <div class="pagination-bar">
        <div class="page-info">
          <span>共 {{ taskPage.total }} 条</span>
          <strong>{{ taskPage.page }} / {{ taskPage.pages || 1 }}</strong>
        </div>

        <div class="page-actions">
          <button class="page-arrow" type="button" :disabled="taskPage.page <= 1" @click="changePage(taskPage.page - 1)">
            <ChevronLeft :size="17" />
            <span>上一页</span>
          </button>
          <button class="page-arrow" type="button" :disabled="taskPage.page >= taskPage.pages" @click="changePage(taskPage.page + 1)">
            <span>下一页</span>
            <ChevronRight :size="17" />
          </button>
        </div>

        <div class="page-size-segment" aria-label="每页数量">
          <button
            v-for="size in [10, 20, 50]"
            :key="size"
            type="button"
            :class="{ active: taskPage.size === size }"
            @click="changePageSize(size)"
          >
            {{ size }}
          </button>
          <span>条/页</span>
        </div>
      </div>

      <div v-if="isComposerOpen" class="composer-overlay" @click.self="closeComposer">
        <form class="task-composer" @submit.prevent="handleCreateTask">
          <div class="composer-heading">
            <div>
              <p>新建任务</p>
              <span>把要做的事写下来，细节可以稍后补充。</span>
            </div>
            <button type="button" class="icon-button" aria-label="关闭新建任务" @click="closeComposer">
              <X :size="18" />
            </button>
          </div>

          <div class="composer-main">
            <Plus :size="20" />
            <input v-model="taskForm.title" type="text" maxlength="100" placeholder="任务标题" autofocus />
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
              {{ isTaskSubmitting ? '创建中...' : '创建任务' }}
            </button>
          </div>
        </form>
      </div>
    </section>

    <aside v-if="selectedTask" class="detail-panel">
      <header class="detail-header">
        <div>
          <p>任务详情</p>
          <h2>{{ selectedTask.title }}</h2>
        </div>
        <button type="button" class="icon-button" aria-label="关闭详情" @click="closeTaskDetail">
          <X :size="18" />
        </button>
      </header>

      <p v-if="detailError" class="notice error">{{ detailError }}</p>

      <form class="detail-form" @submit.prevent="handleUpdateTask">
        <label class="field">
          <span>标题</span>
          <input v-model="editForm.title" type="text" maxlength="100" />
        </label>

        <label class="field">
          <span>描述</span>
          <input v-model="editForm.description" type="text" maxlength="100" placeholder="补充任务说明" />
        </label>

        <div class="detail-section">
          <span class="detail-label">状态</span>
          <div class="status-segment" aria-label="编辑任务状态">
            <button
              v-for="option in statusOptions"
              :key="option.value"
              type="button"
              :class="{ active: selectedTask.status === option.value }"
              @click="handleStatusChange(selectedTask, option.value)"
            >
              <Circle v-if="option.value === 'TODO'" :size="15" />
              <RefreshCw v-else-if="option.value === 'IN_PROGRESS'" :size="15" />
              <Check v-else :size="15" />
              <span>{{ option.label }}</span>
            </button>
          </div>
        </div>

        <div class="detail-section">
          <span class="detail-label">优先级</span>
          <div class="priority-segment" aria-label="编辑优先级">
            <button
              v-for="option in priorityOptions"
              :key="option.value"
              type="button"
              :class="[{ active: editForm.priority === option.value }, option.tone]"
              @click="editForm.priority = option.value"
            >
              {{ option.label }}
            </button>
          </div>
        </div>

        <div class="detail-section">
          <span class="detail-label">截止时间</span>
          <div class="due-editor">
            <div class="due-editor-preview">
              <CalendarDays :size="18" />
              <div>
                <strong>{{ formatDateLabel(editForm.dueDate) }}</strong>
                <span>{{ formatTimeLabel(editForm.dueTime) }}</span>
              </div>
            </div>

            <div class="due-editor-actions">
              <button type="button" @click="setEditDueToday">今天傍晚</button>
              <button type="button" @click="setEditDueTomorrow">明天傍晚</button>
            </div>

            <div class="date-part-grid" aria-label="编辑截止日期">
              <label>
                <span>年</span>
                <input
                  :value="editDateParts.year"
                  inputmode="numeric"
                  maxlength="4"
                  placeholder="2026"
                  @input="updateEditDatePart('year', $event.target.value)"
                />
              </label>
              <label>
                <span>月</span>
                <input
                  :value="editDateParts.month"
                  inputmode="numeric"
                  maxlength="2"
                  placeholder="07"
                  @input="updateEditDatePart('month', $event.target.value)"
                />
              </label>
              <label>
                <span>日</span>
                <input
                  :value="editDateParts.day"
                  inputmode="numeric"
                  maxlength="2"
                  placeholder="08"
                  @input="updateEditDatePart('day', $event.target.value)"
                />
              </label>
            </div>

            <div class="time-part-grid" aria-label="编辑截止时间">
              <label>
                <span>时</span>
                <input
                  :value="editTimeParts.hour"
                  inputmode="numeric"
                  maxlength="2"
                  placeholder="18"
                  @input="updateEditTimePart('hour', $event.target.value)"
                />
              </label>
              <i>:</i>
              <label>
                <span>分</span>
                <input
                  :value="editTimeParts.minute"
                  inputmode="numeric"
                  maxlength="2"
                  placeholder="00"
                  @input="updateEditTimePart('minute', $event.target.value)"
                />
              </label>
            </div>
          </div>
        </div>

        <div class="detail-meta">
          <div class="meta-tile">
            <Inbox :size="17" />
            <span>状态</span>
            <strong>{{ statusText(selectedTask.status) }}</strong>
          </div>
          <div class="meta-tile">
            <CalendarDays :size="17" />
            <span>创建</span>
            <strong>{{ formatFullDateTime(selectedTask.createdAt) }}</strong>
          </div>
          <div class="meta-tile">
            <RefreshCw :size="17" />
            <span>更新</span>
            <strong>{{ formatFullDateTime(selectedTask.updatedAt) }}</strong>
          </div>
        </div>

        <button class="primary-button detail-save" type="submit" :disabled="isDetailSaving || isDetailLoading">
          <Save :size="17" />
          <span>{{ isDetailSaving ? '保存中...' : '保存修改' }}</span>
        </button>

        <button class="danger-button" type="button" @click="handleDeleteTask">
          <Trash2 :size="17" />
          <span>删除任务</span>
        </button>
      </form>
    </aside>
  </main>
</template>
