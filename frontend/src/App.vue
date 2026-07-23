<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import {
  AlignLeft,
  BatteryMedium,
  Bell,
  BrainCircuit,
  CalendarDays,
  Check,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Circle,
  Clock3,
  Copy,
  Flag,
  History,
  ListChecks,
  ListTodo,
  LogOut,
  Menu,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  Search,
  SendHorizontal,
  SlidersHorizontal,
  Sparkles,
  Star,
  Trash2,
  WandSparkles,
  X,
  UserRound
} from '@lucide/vue'
import {
  createTaskStep,
  createTask,
  deleteTaskStep,
  deleteTask,
  getCurrentUser,
  getTask,
  getTaskAdvice,
  getTaskReminders,
  getTaskStats,
  listTaskSteps,
  listTasks,
  loginUser,
  registerUser,
  updateTaskStep,
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
const isTaskListLoading = ref(false)
const isDuePanelOpen = ref(false)
const isComposerOpen = ref(false)
const isFilterOpen = ref(false)
const isReminderOpen = ref(false)
const isSidebarOpen = ref(false)
const isAiAdvisorOpen = ref(false)
const selectedTask = ref(null)
const expandedDetailSection = ref(null)
const taskSteps = ref([])
const taskStepStatsById = reactive(new Map())
const stepDraft = ref('')
const editingStepId = ref(null)
const editingStepTitle = ref('')
const stepDeleteCandidateId = ref(null)
const reminders = ref([])
const detailError = ref('')
const isDetailLoading = ref(false)
const isDetailSaving = ref(false)
const isStepListLoading = ref(false)
const isStepSubmitting = ref(false)
const isAiSubmitting = ref(false)
const aiMessage = ref('')
const aiAdvice = ref('')
const aiError = ref('')
const aiCopied = ref(false)
const stepPendingIds = reactive(new Set())
const filterMenuRef = ref(null)
const reminderMenuRef = ref(null)
const dueMenuRef = ref(null)
const detailPropertiesRef = ref(null)
const aiMessageInputRef = ref(null)
let searchTimer
let aiCopyTimer
let taskStepStatsRequestId = 0

const vFocus = {
  mounted(element) {
    element.focus()
    element.select()
  }
}

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

const editDueParts = reactive({
  year: '',
  month: '',
  day: '',
  hour: '',
  minute: ''
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

const editDateParts = computed(() => ({
  year: editDueParts.year,
  month: editDueParts.month,
  day: editDueParts.day
}))
const editTimeParts = computed(() => ({
  hour: editDueParts.hour,
  minute: editDueParts.minute
}))
const editDescriptionSummary = computed(() => editForm.description.trim() || '添加任务描述')
const editDueLabel = computed(() => {
  const due = resolveEditDueValues()

  if (due.error) {
    return '日期或时间填写中'
  }

  if (!due.date) {
    return '未设置'
  }

  return `${formatDateLabel(due.date)} · ${formatTimeLabel(due.time)}`
})
const completedStepCount = computed(() => taskSteps.value.filter((step) => step.completed).length)
const taskStepSummary = computed(() => {
  if (isStepListLoading.value) {
    return '正在读取步骤'
  }

  if (!taskSteps.value.length) {
    return '添加执行步骤'
  }

  return `${completedStepCount.value} / ${taskSteps.value.length} 已完成`
})
const taskStepProgress = computed(() => {
  if (!taskSteps.value.length) {
    return 0
  }

  return Math.round((completedStepCount.value / taskSteps.value.length) * 100)
})
const unfinishedTaskCount = computed(() => Math.max(0, taskStats.total - taskStats.done))
const isAiMessageValid = computed(() => {
  const message = aiMessage.value.trim()

  return message.length > 0 && message.length <= 1000
})
const aiAdviceBlocks = computed(() => parseAdvice(aiAdvice.value))

const aiPromptOptions = [
  {
    label: '只有 30 分钟',
    prompt: '我现在只有 30 分钟，请根据任务情况告诉我最适合先完成什么。',
    icon: Clock3
  },
  {
    label: '精力比较一般',
    prompt: '我现在精力比较一般，请安排一些容易推进、又不会耽误重要进度的任务。',
    icon: BatteryMedium
  },
  {
    label: '安排今天剩余时间',
    prompt: '请综合截止时间、优先级和当前进度，帮我安排今天剩余时间要处理的任务。',
    icon: WandSparkles
  }
]

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
const emptyState = computed(() => {
  if (query.value.trim()) {
    return {
      title: '没有找到匹配任务',
      description: `没有与“${query.value.trim()}”相关的结果。`,
      action: '清除搜索'
    }
  }

  if (hasListFilters.value) {
    return {
      title: '当前筛选下没有任务',
      description: '调整状态或优先级筛选后再看看。',
      action: '清除筛选'
    }
  }

  return {
    title: `${currentView.value.label}还没有内容`,
    description: '创建一个任务，把下一步安排清楚。',
    action: '创建第一个任务'
  }
})

watch(query, () => {
  window.clearTimeout(searchTimer)

  if (!user.value) {
    return
  }

  searchTimer = window.setTimeout(() => {
    applyKeywordSearch()
  }, 320)
})

watch(aiMessage, () => {
  if (aiError.value) {
    aiError.value = ''
  }
})

onMounted(async () => {
  document.addEventListener('pointerdown', handleDocumentPointerDown)
  document.addEventListener('keydown', handleDocumentKeydown)

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

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', handleDocumentPointerDown)
  document.removeEventListener('keydown', handleDocumentKeydown)
  window.clearTimeout(searchTimer)
  window.clearTimeout(aiCopyTimer)
  document.body.classList.remove('modal-open')
})

function handleDocumentKeydown(event) {
  if (event.key === 'Escape' && isAiAdvisorOpen.value) {
    closeAiAdvisor()
  }
}

function handleDocumentPointerDown(event) {
  const path = event.composedPath()

  if (isFilterOpen.value && filterMenuRef.value && !path.includes(filterMenuRef.value)) {
    isFilterOpen.value = false
  }

  if (isReminderOpen.value && reminderMenuRef.value && !path.includes(reminderMenuRef.value)) {
    isReminderOpen.value = false
  }

  if (isDuePanelOpen.value && dueMenuRef.value && !path.includes(dueMenuRef.value)) {
    isDuePanelOpen.value = false
  }

  if (expandedDetailSection.value && detailPropertiesRef.value && !path.includes(detailPropertiesRef.value)) {
    expandedDetailSection.value = null
    stepDeleteCandidateId.value = null
  }
}

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
    syncTaskStepStats(created.id, [])
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
  isTaskListLoading.value = true
  errorMessage.value = ''

  try {
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

    void refreshTaskStepStats(tasks.value)

    if (selectedTask.value) {
      const latest = tasks.value.find((task) => task.id === selectedTask.value.id)
      selectedTask.value = latest || null
    }

    await refreshTaskStats()
    await refreshTaskReminders()
  } catch (error) {
    errorMessage.value = error.message || '任务加载失败，请稍后重试。'
  } finally {
    isTaskListLoading.value = false
  }
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
  closeAiAdvisor()
  localStorage.removeItem('aiTodoToken')
  user.value = null
  tasks.value = []
  taskStepStatsById.clear()
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
  isSidebarOpen.value = false
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

function clearSearch() {
  query.value = ''
}

function handleEmptyAction() {
  if (query.value.trim()) {
    clearSearch()
    return
  }

  if (hasListFilters.value) {
    clearListFilters()
    return
  }

  openComposer()
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

function getTaskStepStats(taskId) {
  return taskStepStatsById.get(taskId) || {
    total: 0,
    completed: 0,
    progress: 0,
    loading: true,
    error: false
  }
}

function taskStepListLabel(taskId) {
  const stats = getTaskStepStats(taskId)

  if (stats.loading) {
    return '正在读取步骤'
  }

  if (stats.error) {
    return '步骤暂不可用'
  }

  return `${stats.completed} / ${stats.total} 个步骤`
}

function createTaskStepStats(steps, requestId = ++taskStepStatsRequestId) {
  const list = Array.isArray(steps) ? steps : []
  const completed = list.filter((step) => step.completed).length
  const total = list.length

  return {
    total,
    completed,
    progress: total ? Math.round((completed / total) * 100) : 0,
    loading: false,
    error: false,
    requestId
  }
}

function syncTaskStepStats(taskId, steps) {
  if (taskId == null) {
    return
  }

  taskStepStatsById.set(taskId, createTaskStepStats(steps))
}

async function refreshTaskStepStats(taskList) {
  const pendingTasks = Array.isArray(taskList) ? [...taskList] : []

  async function loadTaskStepStats(task) {
    const requestId = ++taskStepStatsRequestId
    const current = taskStepStatsById.get(task.id)

    taskStepStatsById.set(task.id, {
      total: current?.total || 0,
      completed: current?.completed || 0,
      progress: current?.progress || 0,
      loading: !current,
      error: false,
      requestId
    })

    try {
      const steps = await listTaskSteps(task.id)

      if (taskStepStatsById.get(task.id)?.requestId === requestId) {
        taskStepStatsById.set(task.id, createTaskStepStats(steps, requestId))
      }
    } catch {
      if (taskStepStatsById.get(task.id)?.requestId === requestId) {
        taskStepStatsById.set(task.id, {
          total: current?.total || 0,
          completed: current?.completed || 0,
          progress: current?.progress || 0,
          loading: false,
          error: true,
          requestId
        })
      }
    }
  }

  const workerCount = Math.min(6, pendingTasks.length)
  const workers = Array.from({ length: workerCount }, async () => {
    while (pendingTasks.length) {
      await loadTaskStepStats(pendingTasks.shift())
    }
  })

  await Promise.allSettled(workers)
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
  setEditDueParts(todayKey.value, resolveEditDueValues().time || '18:00')
}

function setEditDueTomorrow() {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  setEditDueParts(toLocalDateKey(tomorrow), resolveEditDueValues().time || '18:00')
}

function updateEditDatePart(part, value) {
  const maxLength = part === 'year' ? 4 : 2
  editDueParts[part] = String(value).replace(/\D/g, '').slice(0, maxLength)
}

function updateEditTimePart(part, value) {
  editDueParts[part] = String(value).replace(/\D/g, '').slice(0, 2)
}

function setEditDueParts(date, time) {
  const dateParts = splitDateParts(date)
  const timeParts = splitTimeParts(time)

  Object.assign(editDueParts, {
    year: dateParts.year,
    month: dateParts.month,
    day: dateParts.day,
    hour: timeParts.hour,
    minute: timeParts.minute
  })
}

function resolveEditDueValues() {
  const { year, month, day, hour, minute } = editDueParts
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

function splitDateParts(value) {
  const [year = '', month = '', day = ''] = value ? value.split('-') : []

  return { year, month, day }
}

function splitTimeParts(value) {
  const [hour = '', minute = ''] = value ? value.split(':') : []

  return { hour, minute }
}

async function openTaskDetail(task) {
  const taskId = task.id
  selectedTask.value = task
  expandedDetailSection.value = null
  taskSteps.value = []
  stepDraft.value = ''
  stepPendingIds.clear()
  resetStepEditor()
  isReminderOpen.value = false
  detailError.value = ''
  fillEditForm(task)
  isDetailLoading.value = true
  isStepListLoading.value = true

  const [taskResult, stepsResult] = await Promise.allSettled([
    getTask(taskId),
    listTaskSteps(taskId)
  ])

  if (selectedTask.value?.id !== taskId) {
    return
  }

  if (taskResult.status === 'fulfilled') {
    selectedTask.value = taskResult.value
    fillEditForm(taskResult.value)
  } else {
    detailError.value = taskResult.reason?.message || '读取任务详情失败。'
  }

  if (stepsResult.status === 'fulfilled') {
    taskSteps.value = Array.isArray(stepsResult.value) ? stepsResult.value : []
    syncTaskStepStats(taskId, taskSteps.value)
  } else {
    detailError.value = stepsResult.reason?.message || detailError.value || '读取任务步骤失败。'
  }

  if (selectedTask.value?.id === taskId) {
    isDetailLoading.value = false
    isStepListLoading.value = false
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

  const due = resolveEditDueValues()

  if (due.error) {
    detailError.value = due.error
    expandedDetailSection.value = 'due'
    return
  }

  isDetailSaving.value = true

  try {
    const payload = {
      title: editForm.title.trim(),
      description: editForm.description.trim(),
      priority: editForm.priority
    }

    if (due.date) {
      payload.dueAt = `${due.date}T${due.time}`
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

  if (!window.confirm(`确定删除“${selectedTask.value.title}”吗？此操作无法撤销。`)) {
    return
  }

  detailError.value = ''

  try {
    const taskId = selectedTask.value.id
    await deleteTask(taskId)
    tasks.value = tasks.value.filter((task) => task.id !== taskId)
    taskStepStatsById.delete(taskId)
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
  setEditDueParts(due.date, due.time)
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
  expandedDetailSection.value = null
  taskSteps.value = []
  stepDraft.value = ''
  stepPendingIds.clear()
  resetStepEditor()
  isDetailLoading.value = false
  isStepListLoading.value = false
  detailError.value = ''
}

function toggleDetailSection(section) {
  expandedDetailSection.value = expandedDetailSection.value === section ? null : section
  stepDeleteCandidateId.value = null
}

async function selectDetailStatus(status) {
  await handleStatusChange(selectedTask.value, status)
  expandedDetailSection.value = null
}

function selectDetailPriority(priority) {
  editForm.priority = priority
  expandedDetailSection.value = null
}

function resetStepEditor() {
  editingStepId.value = null
  editingStepTitle.value = ''
  stepDeleteCandidateId.value = null
}

function replaceTaskStep(updatedStep) {
  taskSteps.value = taskSteps.value.map((step) => (step.id === updatedStep.id ? updatedStep : step))
  syncTaskStepStats(selectedTask.value?.id, taskSteps.value)
}

async function handleCreateStep() {
  const taskId = selectedTask.value?.id
  const title = stepDraft.value.trim()

  detailError.value = ''

  if (!taskId || isStepSubmitting.value) {
    return
  }

  if (!title || title.length > 100) {
    detailError.value = '步骤标题不能为空，且不能超过 100 个字符。'
    return
  }

  isStepSubmitting.value = true

  try {
    const created = await createTaskStep(taskId, { title })

    if (selectedTask.value?.id === taskId) {
      taskSteps.value = [...taskSteps.value, created]
      syncTaskStepStats(taskId, taskSteps.value)
      stepDraft.value = ''
    }
  } catch (error) {
    detailError.value = error.message || '创建任务步骤失败。'
  } finally {
    isStepSubmitting.value = false
  }
}

async function toggleTaskStep(step) {
  const taskId = selectedTask.value?.id

  if (!taskId || stepPendingIds.has(step.id)) {
    return
  }

  const previous = { ...step }
  const optimistic = { ...step, completed: !step.completed }
  detailError.value = ''
  stepPendingIds.add(step.id)
  replaceTaskStep(optimistic)

  try {
    const updated = await updateTaskStep(taskId, step.id, { completed: optimistic.completed })

    if (selectedTask.value?.id === taskId) {
      replaceTaskStep(updated)
    }
  } catch (error) {
    if (selectedTask.value?.id === taskId) {
      replaceTaskStep(previous)
      detailError.value = error.message || '更新任务步骤失败。'
    }
  } finally {
    stepPendingIds.delete(step.id)
  }
}

function startStepEditing(step) {
  editingStepId.value = step.id
  editingStepTitle.value = step.title
  stepDeleteCandidateId.value = null
}

async function saveStepTitle(step) {
  const taskId = selectedTask.value?.id
  const title = editingStepTitle.value.trim()

  detailError.value = ''

  if (!taskId || stepPendingIds.has(step.id)) {
    return
  }

  if (!title) {
    detailError.value = '步骤标题不能为空。'
    return
  }

  if (title === step.title) {
    resetStepEditor()
    return
  }

  stepPendingIds.add(step.id)

  try {
    const updated = await updateTaskStep(taskId, step.id, { title })

    if (selectedTask.value?.id === taskId) {
      replaceTaskStep(updated)
      resetStepEditor()
    }
  } catch (error) {
    detailError.value = error.message || '修改步骤标题失败。'
  } finally {
    stepPendingIds.delete(step.id)
  }
}

async function handleDeleteStep(step) {
  const taskId = selectedTask.value?.id

  if (!taskId || stepPendingIds.has(step.id)) {
    return
  }

  detailError.value = ''
  stepPendingIds.add(step.id)

  try {
    await deleteTaskStep(taskId, step.id)

    if (selectedTask.value?.id === taskId) {
      taskSteps.value = taskSteps.value.filter((item) => item.id !== step.id)
      syncTaskStepStats(taskId, taskSteps.value)
      resetStepEditor()
    }
  } catch (error) {
    detailError.value = error.message || '删除任务步骤失败。'
  } finally {
    stepPendingIds.delete(step.id)
  }
}

async function openAiAdvisor() {
  isAiAdvisorOpen.value = true
  isComposerOpen.value = false
  isDuePanelOpen.value = false
  isFilterOpen.value = false
  isReminderOpen.value = false
  isSidebarOpen.value = false
  aiError.value = ''
  document.body.classList.add('modal-open')

  await nextTick()
  aiMessageInputRef.value?.focus()
}

function closeAiAdvisor() {
  isAiAdvisorOpen.value = false
  aiCopied.value = false
  document.body.classList.remove('modal-open')
}

function applyAiPrompt(prompt) {
  aiMessage.value = prompt
  aiError.value = ''
  nextTick(() => aiMessageInputRef.value?.focus())
}

async function handleAiAdviceSubmit() {
  const message = aiMessage.value.trim()

  aiError.value = ''
  aiCopied.value = false

  if (!message) {
    aiError.value = '请先写下你现在的时间、精力或安排目标。'
    return
  }

  if (message.length > 1000) {
    aiError.value = '咨询内容不能超过 1000 个字符。'
    return
  }

  isAiSubmitting.value = true

  try {
    const result = await getTaskAdvice({ message })
    const advice = result?.advice?.trim()

    if (!advice) {
      throw new Error('AI 暂时没有返回可用的规划建议。')
    }

    aiAdvice.value = advice
  } catch (error) {
    aiError.value = error.message || '获取 AI 规划建议失败。'
  } finally {
    isAiSubmitting.value = false
  }
}

async function copyAiAdvice() {
  if (!aiAdvice.value) {
    return
  }

  try {
    await navigator.clipboard.writeText(aiAdvice.value)
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = aiAdvice.value
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    textarea.remove()
  }

  aiCopied.value = true
  window.clearTimeout(aiCopyTimer)
  aiCopyTimer = window.setTimeout(() => {
    aiCopied.value = false
  }, 1800)
}

function parseAdvice(content) {
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
        <div class="brand-lockup">
          <span>AT</span>
          <p class="brand">AI Todo</p>
        </div>
        <div class="auth-visual-copy">
          <p>PERSONAL FOCUS SYSTEM</p>
          <h1>让今天的重点<br />清晰可见</h1>
          <span>安静地收拢任务，把注意力留给真正重要的事。</span>
        </div>
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
    <button
      v-if="isSidebarOpen"
      class="sidebar-backdrop"
      type="button"
      aria-label="关闭导航"
      @click="isSidebarOpen = false"
    ></button>

    <aside class="sidebar" :class="{ open: isSidebarOpen }">
      <div class="sidebar-brand">
        <span>AT</span>
        <strong>AI Todo</strong>
        <button class="mobile-sidebar-close" type="button" aria-label="关闭导航" @click="isSidebarOpen = false">
          <X :size="18" />
        </button>
      </div>

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
          :aria-current="activeView === view.key ? 'page' : undefined"
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
          <button class="mobile-menu-button" type="button" aria-label="打开导航" @click="isSidebarOpen = true">
            <Menu :size="20" />
          </button>
          <p class="date-line">{{ new Date().toLocaleDateString('zh-CN', { weekday: 'long', month: 'long', day: 'numeric' }) }}</p>
          <h1>{{ currentView.label }}</h1>
        </div>

        <div class="board-tools">
          <div class="search-box search-box-compact">
            <Search :size="17" />
            <input v-model="query" type="search" placeholder="搜索标题或描述" aria-label="搜索任务" />
            <button v-if="query" type="button" aria-label="清除搜索" title="清除搜索" @click="clearSearch">
              <X :size="15" />
            </button>
          </div>

          <div ref="reminderMenuRef" class="reminder-menu">
            <button
              class="tool-button reminder-trigger"
              type="button"
              aria-label="查看即将到期任务"
              @click="isReminderOpen = !isReminderOpen; isFilterOpen = false"
            >
              <Bell :size="17" />
              <span>未来 60 分钟</span>
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

          <div ref="filterMenuRef" class="filter-menu">
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

        </div>

        <div class="board-primary-actions">
          <button class="tool-button ai-trigger" type="button" @click="openAiAdvisor">
            <WandSparkles :size="17" />
            <span>AI 规划</span>
          </button>

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

      <div v-if="errorMessage" class="notice error list-error" role="alert">
        <span>{{ errorMessage }}</span>
        <button type="button" @click="refreshTasks">重试</button>
      </div>
      <div class="task-list">
        <div v-if="!isTaskListLoading && visibleTasks.length" class="task-list-columns" aria-hidden="true">
          <span></span>
          <span>任务</span>
          <span>执行进度</span>
          <span>截止时间</span>
        </div>

        <template v-if="isTaskListLoading">
          <div v-for="index in 5" :key="index" class="task-skeleton" aria-hidden="true">
            <span></span>
            <div><i></i><i></i></div>
            <em></em>
          </div>
        </template>

        <article
          v-for="task in isTaskListLoading ? [] : visibleTasks"
          :key="task.id"
          class="task-item"
          :class="{ selected: selectedTask?.id === task.id, done: task.status === 'DONE' }"
          tabindex="0"
          @click="openTaskDetail(task)"
          @keydown.enter="openTaskDetail(task)"
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
            </div>
          </div>
          <div
            class="task-step-summary"
            :class="{
              loading: getTaskStepStats(task.id).loading,
              unavailable: getTaskStepStats(task.id).error,
              complete: getTaskStepStats(task.id).progress === 100
            }"
          >
            <div class="task-step-copy">
              <ListChecks :size="13" />
              <span>{{ taskStepListLabel(task.id) }}</span>
              <b v-if="!getTaskStepStats(task.id).loading && !getTaskStepStats(task.id).error">
                {{ getTaskStepStats(task.id).progress }}%
              </b>
            </div>
            <div
              class="task-step-track"
              role="progressbar"
              :aria-label="`${task.title}的步骤完成进度`"
              :aria-valuenow="getTaskStepStats(task.id).progress"
              aria-valuemin="0"
              aria-valuemax="100"
            >
              <span :style="{ width: `${getTaskStepStats(task.id).progress}%` }"></span>
            </div>
          </div>
          <div class="task-meta">
            <span v-if="task.dueAt" class="task-due" :class="{ overdue: isTaskOverdue(task) }">
              <CalendarDays :size="14" />
              {{ formatDueAt(task.dueAt) }}
            </span>
          </div>
        </article>

        <section v-if="!isTaskListLoading && visibleTasks.length === 0" class="empty-panel">
          <ListTodo :size="34" />
          <h2>{{ emptyState.title }}</h2>
          <p>{{ emptyState.description }}</p>
          <button type="button" @click="handleEmptyAction">{{ emptyState.action }}</button>
        </section>
      </div>

      <div v-if="taskPage.pages > 1 || taskPage.total > 10" class="pagination-bar">
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

        <label class="page-size-select">
          <span class="sr-only">每页显示数量</span>
          <select :value="taskPage.size" aria-label="每页显示数量" @change="changePageSize(Number($event.target.value))">
            <option v-for="size in [10, 20, 50]" :key="size" :value="size">{{ size }} 条 / 页</option>
          </select>
          <ChevronDown :size="15" />
        </label>
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
            <div ref="dueMenuRef" class="due-menu" :class="{ open: isDuePanelOpen }">
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

      <Transition name="ai-overlay">
        <div v-if="isAiAdvisorOpen" class="ai-advisor-overlay" @click.self="closeAiAdvisor">
          <section class="ai-advisor" role="dialog" aria-modal="true" aria-labelledby="ai-advisor-title">
            <div class="ai-decoration" aria-hidden="true">
              <span></span>
              <span></span>
              <span></span>
            </div>

            <header class="ai-advisor-header">
              <div class="ai-heading-mark"><BrainCircuit :size="22" /></div>
              <div class="ai-heading-copy">
                <span>AI FOCUS STUDIO</span>
                <h2 id="ai-advisor-title">现在，先做什么？</h2>
              </div>
              <button class="icon-button ai-close" type="button" aria-label="关闭 AI 规划" @click="closeAiAdvisor">
                <X :size="19" />
              </button>
            </header>

            <div class="ai-context-strip" aria-label="当前任务上下文">
              <span><b>{{ unfinishedTaskCount }}</b> 未完成</span>
              <span><b>{{ taskStats.inProgress }}</b> 进行中</span>
              <span><b>{{ taskStats.highPriority }}</b> 高优先级</span>
              <span><b>{{ taskStats.dueToday }}</b> 今天截止</span>
            </div>

            <div class="ai-workspace">
              <form class="ai-prompt-panel" @submit.prevent="handleAiAdviceSubmit">
                <div class="ai-section-heading">
                  <span>01</span>
                  <div>
                    <small>CONTEXT</small>
                    <h3>补充你的当前状态</h3>
                  </div>
                </div>

                <div class="ai-prompt-options" aria-label="快捷咨询条件">
                  <button
                    v-for="option in aiPromptOptions"
                    :key="option.label"
                    type="button"
                    :class="{ active: aiMessage === option.prompt }"
                    @click="applyAiPrompt(option.prompt)"
                  >
                    <component :is="option.icon" :size="15" />
                    <span>{{ option.label }}</span>
                  </button>
                </div>

                <label class="ai-message-field">
                  <span class="sr-only">咨询内容</span>
                  <textarea
                    ref="aiMessageInputRef"
                    v-model="aiMessage"
                    maxlength="1000"
                    rows="7"
                    placeholder="例如：我现在有 40 分钟，精力一般，希望先推进最紧急的任务。"
                    @keydown.ctrl.enter.prevent="handleAiAdviceSubmit"
                    @keydown.meta.enter.prevent="handleAiAdviceSubmit"
                  ></textarea>
                </label>

                <p v-if="aiError" class="notice error ai-error" role="alert">{{ aiError }}</p>

                <div class="ai-prompt-footer">
                  <span :class="{ over: aiMessage.length > 1000 }">{{ aiMessage.length }} / 1000</span>
                  <button class="ai-submit" type="submit" :disabled="isAiSubmitting || !isAiMessageValid">
                    <SendHorizontal :size="16" />
                    <span>{{ isAiSubmitting ? '正在规划' : aiAdvice ? '重新规划' : '生成安排' }}</span>
                  </button>
                </div>
              </form>

              <section class="ai-response-panel" aria-live="polite">
                <div class="ai-section-heading response-heading">
                  <span>02</span>
                  <div>
                    <small>FOCUS PLAN</small>
                    <h3>本次安排</h3>
                  </div>
                  <button
                    v-if="aiAdvice && !isAiSubmitting"
                    class="ai-copy"
                    type="button"
                    :aria-label="aiCopied ? '已复制建议' : '复制建议'"
                    :title="aiCopied ? '已复制' : '复制建议'"
                    @click="copyAiAdvice"
                  >
                    <Check v-if="aiCopied" :size="15" />
                    <Copy v-else :size="15" />
                  </button>
                </div>

                <div v-if="isAiSubmitting" class="ai-thinking" role="status">
                  <div class="thinking-lines" aria-hidden="true">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                  <p>正在结合任务进度与截止时间...</p>
                </div>

                <article v-else-if="aiAdvice" class="ai-advice-content">
                  <template v-for="(block, index) in aiAdviceBlocks" :key="`${block.type}-${index}`">
                    <h4 v-if="block.type === 'heading'">{{ block.text }}</h4>
                    <ol v-else-if="block.type === 'ordered'">
                      <li v-for="item in block.items" :key="item">{{ item }}</li>
                    </ol>
                    <ul v-else-if="block.type === 'unordered'">
                      <li v-for="item in block.items" :key="item">{{ item }}</li>
                    </ul>
                    <p v-else>{{ block.text }}</p>
                  </template>
                </article>

                <div v-else class="ai-waiting">
                  <BrainCircuit :size="34" />
                  <strong>等待本次规划</strong>
                  <span>把此刻的时间和精力写下来。</span>
                </div>
              </section>
            </div>
          </section>
        </div>
      </Transition>
    </section>

    <aside v-if="selectedTask" class="detail-panel">
      <header class="detail-header">
        <div class="detail-heading-copy">
          <p>任务详情</p>
          <span>整理任务的关键信息</span>
        </div>
        <button type="button" class="icon-button" aria-label="关闭详情" @click="closeTaskDetail">
          <X :size="18" />
        </button>
      </header>

      <p v-if="detailError" class="notice error">{{ detailError }}</p>

      <form class="detail-form" @submit.prevent="handleUpdateTask">
        <label class="detail-title-field">
          <span>标题</span>
          <input v-model="editForm.title" type="text" maxlength="100" />
        </label>

        <div ref="detailPropertiesRef" class="property-stack">
          <section class="property-item description-item" :class="{ expanded: expandedDetailSection === 'description' }">
            <button class="property-trigger" type="button" @click="toggleDetailSection('description')">
              <span class="property-icon description-icon"><AlignLeft :size="16" /></span>
              <span class="property-copy">
                <small>描述</small>
                <strong>{{ editDescriptionSummary }}</strong>
              </span>
              <ChevronDown class="property-chevron" :size="17" />
            </button>

            <Transition name="property-reveal">
              <div v-if="expandedDetailSection === 'description'" class="property-editor description-editor">
                <textarea v-model="editForm.description" maxlength="100" rows="4" placeholder="补充任务说明"></textarea>
                <span>{{ editForm.description.length }} / 100</span>
              </div>
            </Transition>
          </section>

          <section class="property-item steps-item expanded steps-always-open">
            <div class="property-trigger steps-heading">
              <span class="property-icon steps-icon"><ListChecks :size="17" /></span>
              <span class="property-copy">
                <small>执行步骤</small>
                <strong>{{ taskStepSummary }}</strong>
              </span>
              <span v-if="taskSteps.length" class="step-count">{{ taskStepProgress }}%</span>
            </div>

            <div class="property-editor steps-editor">
                <div v-if="taskSteps.length" class="steps-progress" aria-hidden="true">
                  <span :style="{ width: `${taskStepProgress}%` }"></span>
                </div>

                <div v-if="isStepListLoading" class="steps-loading" role="status">
                  <RefreshCw :size="15" />
                  <span>正在读取步骤</span>
                </div>

                <div v-else class="steps-list">
                  <div
                    v-for="step in taskSteps"
                    :key="step.id"
                    class="step-row"
                    :class="{ completed: step.completed, pending: stepPendingIds.has(step.id) }"
                  >
                    <button
                      class="step-check"
                      type="button"
                      :class="{ completed: step.completed }"
                      :aria-label="step.completed ? '恢复步骤' : '完成步骤'"
                      :disabled="stepPendingIds.has(step.id)"
                      @click="toggleTaskStep(step)"
                    >
                      <Check v-if="step.completed" :size="13" />
                    </button>

                    <template v-if="editingStepId === step.id">
                      <input
                        v-focus
                        v-model="editingStepTitle"
                        class="step-edit-input"
                        type="text"
                        maxlength="100"
                        aria-label="编辑步骤标题"
                        @keydown.enter.prevent="saveStepTitle(step)"
                        @keydown.esc.prevent="resetStepEditor"
                      />
                      <div class="step-actions editing-actions">
                        <button type="button" aria-label="保存步骤标题" title="保存" @click="saveStepTitle(step)">
                          <Check :size="14" />
                        </button>
                        <button type="button" aria-label="取消编辑" title="取消" @click="resetStepEditor">
                          <X :size="14" />
                        </button>
                      </div>
                    </template>

                    <template v-else>
                      <span class="step-title" @dblclick="startStepEditing(step)">{{ step.title }}</span>
                      <div class="step-actions">
                        <template v-if="stepDeleteCandidateId === step.id">
                          <button
                            class="confirm-step-delete"
                            type="button"
                            aria-label="确认删除步骤"
                            title="确认删除"
                            @click="handleDeleteStep(step)"
                          >
                            <Check :size="14" />
                          </button>
                          <button type="button" aria-label="取消删除" title="取消" @click="stepDeleteCandidateId = null">
                            <X :size="14" />
                          </button>
                        </template>
                        <template v-else>
                          <button type="button" aria-label="编辑步骤" title="编辑步骤" @click="startStepEditing(step)">
                            <Pencil :size="14" />
                          </button>
                          <button
                            type="button"
                            aria-label="删除步骤"
                            title="删除步骤"
                            @click="stepDeleteCandidateId = step.id"
                          >
                            <Trash2 :size="14" />
                          </button>
                        </template>
                      </div>
                    </template>
                  </div>

                  <p v-if="!taskSteps.length" class="steps-empty">还没有执行步骤</p>
                </div>

                <div class="step-composer">
                  <Plus :size="15" />
                  <input
                    v-model="stepDraft"
                    type="text"
                    maxlength="100"
                    placeholder="添加下一步"
                    aria-label="添加任务步骤"
                    @keydown.enter.prevent="handleCreateStep"
                  />
                  <button
                    type="button"
                    aria-label="添加步骤"
                    title="添加步骤"
                    :disabled="isStepSubmitting || !stepDraft.trim()"
                    @click="handleCreateStep"
                  >
                    <Plus :size="15" />
                  </button>
                </div>
            </div>
          </section>

          <section class="property-item status-item" :class="{ expanded: expandedDetailSection === 'status' }">
            <button class="property-trigger" type="button" @click="toggleDetailSection('status')">
              <span class="property-icon status-icon" :class="`status-${selectedTask.status}`">
                <Circle v-if="selectedTask.status === 'TODO'" :size="16" />
                <RefreshCw v-else-if="selectedTask.status === 'IN_PROGRESS'" :size="16" />
                <Check v-else :size="16" />
              </span>
              <span class="property-copy">
                <small>状态</small>
                <strong>{{ statusText(selectedTask.status) }}</strong>
              </span>
              <ChevronDown class="property-chevron" :size="17" />
            </button>

            <Transition name="property-reveal">
              <div v-if="expandedDetailSection === 'status'" class="property-editor status-option-grid">
                <button
                  v-for="option in statusOptions"
                  :key="option.value"
                  type="button"
                  :class="{ active: selectedTask.status === option.value }"
                  @click="selectDetailStatus(option.value)"
                >
                  <Circle v-if="option.value === 'TODO'" :size="16" />
                  <RefreshCw v-else-if="option.value === 'IN_PROGRESS'" :size="16" />
                  <Check v-else :size="16" />
                  <span>{{ option.label }}</span>
                </button>
              </div>
            </Transition>
          </section>

          <section class="property-item priority-item" :class="{ expanded: expandedDetailSection === 'priority' }">
            <button class="property-trigger" type="button" @click="toggleDetailSection('priority')">
              <span class="property-icon priority-icon" :class="`priority-${editForm.priority}`">
                <Flag :size="16" />
              </span>
              <span class="property-copy">
                <small>优先级</small>
                <strong>{{ priorityText(editForm.priority) }}优先级</strong>
              </span>
              <ChevronDown class="property-chevron" :size="17" />
            </button>

            <Transition name="property-reveal">
              <div v-if="expandedDetailSection === 'priority'" class="property-editor priority-option-grid">
                <button
                  v-for="option in priorityOptions"
                  :key="option.value"
                  type="button"
                  :class="[{ active: editForm.priority === option.value }, option.tone]"
                  @click="selectDetailPriority(option.value)"
                >
                  <Flag :size="15" />
                  <span>{{ option.label }}优先级</span>
                </button>
              </div>
            </Transition>
          </section>

          <section class="property-item due-item" :class="{ expanded: expandedDetailSection === 'due' }">
            <button class="property-trigger" type="button" @click="toggleDetailSection('due')">
              <span class="property-icon due-icon"><CalendarDays :size="16" /></span>
              <span class="property-copy">
                <small>截止时间</small>
                <strong>{{ editDueLabel }}</strong>
              </span>
              <ChevronDown class="property-chevron" :size="17" />
            </button>

            <Transition name="property-reveal">
              <div v-if="expandedDetailSection === 'due'" class="property-editor compact-due-editor">
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
            </Transition>
          </section>

          <section class="property-item activity-item" :class="{ expanded: expandedDetailSection === 'activity' }">
            <button class="property-trigger" type="button" @click="toggleDetailSection('activity')">
              <span class="property-icon activity-icon"><History :size="16" /></span>
              <span class="property-copy">
                <small>活动信息</small>
                <strong>创建与最近更新</strong>
              </span>
              <ChevronDown class="property-chevron" :size="17" />
            </button>

            <Transition name="property-reveal">
              <div v-if="expandedDetailSection === 'activity'" class="property-editor activity-list activity-inline-list">
                <div>
                  <CalendarDays :size="15" />
                  <span>创建</span>
                  <strong>{{ formatFullDateTime(selectedTask.createdAt) }}</strong>
                </div>
                <div>
                  <RefreshCw :size="15" />
                  <span>更新</span>
                  <strong>{{ formatFullDateTime(selectedTask.updatedAt) }}</strong>
                </div>
              </div>
            </Transition>
          </section>
        </div>

        <div class="detail-actions">
          <button class="primary-button detail-save" type="submit" :disabled="isDetailSaving || isDetailLoading">
            <Save :size="17" />
            <span>{{ isDetailSaving ? '保存中...' : '保存修改' }}</span>
          </button>

          <button class="danger-icon-button" type="button" aria-label="删除任务" title="删除任务" @click="handleDeleteTask">
            <Trash2 :size="17" />
          </button>
        </div>
      </form>
    </aside>
  </main>
</template>
