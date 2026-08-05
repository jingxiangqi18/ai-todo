<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import VueDraggable from 'vuedraggable'
import {
  AlignLeft,
  BatteryMedium,
  Bell,
  BrainCircuit,
  Building2,
  CalendarDays,
  Check,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Circle,
  Clock3,
  Copy,
  Crown,
  Flag,
  FolderPlus,
  GripVertical,
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
  UsersRound,
  WandSparkles,
  X,
  UserRound
} from '@lucide/vue'
import {
  createGroup,
  createTaskStep,
  createTaskStepsBatch,
  createTask,
  deleteTaskStep,
  deleteTask,
  generateTaskStepDrafts,
  getCurrentUser,
  getGroup,
  getTask,
  getTaskAdvice,
  getTaskReminders,
  getTaskStats,
  listTaskSteps,
  listTasks,
  listGroupMembers,
  listGroups,
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
const isCompletedGroupOpen = ref(true)
const isComposerOpen = ref(false)
const isFilterOpen = ref(false)
const isReminderOpen = ref(false)
const isSidebarOpen = ref(false)
const isAiAdvisorOpen = ref(false)
const deleteCandidate = ref(null)
const isLogoutConfirmOpen = ref(false)
const isTaskDeleting = ref(false)
const deleteDialogError = ref('')
const selectedTask = ref(null)
const expandedDetailSection = ref(null)
const taskSteps = ref([])
const taskStepStatsById = reactive(new Map())
const stepDraft = ref('')
const isAiStepDraftOpen = ref(false)
const isAiStepDraftLoading = ref(false)
const isAiStepDraftSaving = ref(false)
const aiStepInstruction = ref('')
const aiStepDrafts = ref([])
const aiStepDraftError = ref('')
const aiStepDraftMessage = ref('')
const editingStepId = ref(null)
const editingStepTitle = ref('')
const stepDeleteCandidateId = ref(null)
const reminders = ref([])
const allTasksSnapshot = ref([])
const groups = ref([])
const selectedGroup = ref(null)
const groupMembers = ref([])
const isGroupListLoading = ref(false)
const isGroupDetailLoading = ref(false)
const isGroupComposerOpen = ref(false)
const isGroupSubmitting = ref(false)
const groupListError = ref('')
const groupDetailError = ref('')
const groupFormError = ref('')
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
const detailPropertiesRef = ref(null)
const aiMessageInputRef = ref(null)
const deleteCancelButtonRef = ref(null)
const DETAIL_PANEL_WIDTH_KEY = 'aiTodoDetailPanelWidth'
const TASK_ORDER_STORAGE_PREFIX = 'aiTodoTaskOrder'
const TASK_STEP_BATCH_SIZE = 10
const END_OF_DAY_TIME = '23:59'
const DETAIL_PANEL_DEFAULT_WIDTH = 520
const DETAIL_PANEL_MIN_WIDTH = 360
const DETAIL_PANEL_MAX_WIDTH = 760
const detailPanelWidth = ref(readStoredDetailPanelWidth())
const detailPanelBounds = reactive({
  min: DETAIL_PANEL_MIN_WIDTH,
  max: DETAIL_PANEL_MAX_WIDTH
})
const isDetailResizing = ref(false)
const isTaskDragging = ref(false)
const draggingTaskId = ref(null)
let searchTimer
let aiCopyTimer
let taskStepStatsRequestId = 0
let createStepDraftId = 0
let groupDetailRequestId = 0
let detailResizeStartX = 0
let detailResizeStartWidth = 0
let suppressTaskClickUntil = 0

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

const groupForm = reactive({
  name: '',
  description: ''
})

const taskForm = reactive({
  title: '',
  description: '',
  status: 'TODO',
  priority: 'MEDIUM',
  dueDate: '',
  dueTime: ''
})

const createDueParts = reactive({
  year: '',
  month: '',
  day: '',
  hour: '',
  minute: ''
})
const composerError = ref('')
const createStepDraft = ref('')
const createStepDrafts = ref([])
const isCreateCustomDueOpen = ref(false)

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
const isEditCustomDueOpen = ref(false)

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
const createDateParts = computed(() => ({
  year: createDueParts.year,
  month: createDueParts.month,
  day: createDueParts.day
}))
const createTimeParts = computed(() => ({
  hour: createDueParts.hour,
  minute: createDueParts.minute
}))
const hasCreateDueValue = computed(() => Object.values(createDueParts).some(Boolean))
const createDueLabel = computed(() => {
  const due = resolveCreateDueValues()

  if (due.error) {
    return '日期或时间填写中'
  }

  if (!due.date) {
    return '未设置截止时间'
  }

  return `${formatDateLabel(due.date)} · ${formatTimeLabel(due.time)}`
})
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
const selectedAiStepDrafts = computed(() => aiStepDrafts.value.filter((draft) => draft.selected))
const areAllAiStepDraftsSelected = computed(() => (
  aiStepDrafts.value.length > 0 && selectedAiStepDrafts.value.length === aiStepDrafts.value.length
))
const canGenerateAiStepDraft = computed(() => (
  !isAiStepDraftLoading.value && aiStepInstruction.value.length <= 500
))
const unfinishedTaskCount = computed(() => Math.max(0, taskStats.total - taskStats.done))
const isAiMessageValid = computed(() => {
  const message = aiMessage.value.trim()

  return message.length > 0 && message.length <= 1000
})
const aiAdviceBlocks = computed(() => parseAdvice(aiAdvice.value))
const isGroupFormValid = computed(() => {
  const nameLength = groupForm.name.trim().length
  return nameLength > 0 && nameLength <= 100 && groupForm.description.length <= 500
})
const selectedGroupRole = computed(() => groupRoleLabel(selectedGroup.value?.currentUserRole))

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

const aiStepInstructionPresets = [
  '拆成 3 到 5 个最关键步骤',
  '优先给出可以立即开始的步骤',
  '每一步尽量控制在 30 分钟内'
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

const activeTaskSnapshot = computed(() => allTasksSnapshot.value.filter((task) => task.status !== 'DONE'))
const todayTaskSnapshot = computed(() => activeTaskSnapshot.value.filter(isTaskDueToday))
const plannedTaskSnapshot = computed(() => activeTaskSnapshot.value.filter((task) => Boolean(task.dueAt)))
const importantTaskSnapshot = computed(() => activeTaskSnapshot.value.filter((task) => task.priority === 'HIGH'))
const inProgressTaskSnapshot = computed(() => allTasksSnapshot.value.filter((task) => task.status === 'IN_PROGRESS'))
const doneTaskSnapshot = computed(() => allTasksSnapshot.value.filter((task) => task.status === 'DONE'))

const views = computed(() => [
  { key: 'all', label: '全部任务', icon: ListTodo, count: allTasksSnapshot.value.length },
  { key: 'today', label: '我的一天', icon: Sparkles, count: todayTaskSnapshot.value.length },
  { key: 'planned', label: '计划内', icon: CalendarDays, count: plannedTaskSnapshot.value.length },
  { key: 'important', label: '重要', icon: Star, count: importantTaskSnapshot.value.length },
  { key: 'progress', label: '进行中', icon: RefreshCw, count: inProgressTaskSnapshot.value.length },
  { key: 'done', label: '已完成', icon: Check, count: doneTaskSnapshot.value.length }
])

const todayKey = computed(() => toLocalDateKey(new Date()))
const duePresetOptions = computed(() => [
  {
    value: 'today',
    label: '今天完成',
    meta: '今天 · 23:59'
  },
  {
    value: 'tomorrow',
    label: '明天完成',
    meta: '明天 · 23:59'
  },
  {
    value: 'next-week',
    label: '下周内',
    meta: `${formatShortDate(getNextWeekEndDate())} · 23:59`
  }
])
const currentViewTaskSnapshot = computed(() => filterTasksForCurrentView(allTasksSnapshot.value))
const currentViewStats = computed(() => createTaskStats(currentViewTaskSnapshot.value))
const visibleTasks = computed(() => tasks.value)
const isConfirmationDialogOpen = computed(() => Boolean(deleteCandidate.value) || isLogoutConfirmOpen.value)
const shouldSeparateCompletedTasks = computed(() => (
  activeView.value === 'all' && listFilters.status !== 'DONE'
))
const visibleTaskGroups = computed(() => {
  if (!visibleTasks.value.length) {
    return []
  }

  if (!shouldSeparateCompletedTasks.value) {
    return [{
      key: 'primary',
      completed: false,
      tasks: visibleTasks.value
    }]
  }

  const pendingTasks = visibleTasks.value.filter((task) => task.status !== 'DONE')
  const completedTasks = visibleTasks.value.filter((task) => task.status === 'DONE')
  const groups = []

  if (pendingTasks.length) {
    groups.push({
      key: 'pending',
      completed: false,
      tasks: pendingTasks
    })
  }

  if (completedTasks.length) {
    groups.push({
      key: 'completed',
      completed: true,
      tasks: completedTasks
    })
  }

  return groups
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
  window.addEventListener('resize', updateDetailPanelBounds)
  updateDetailPanelBounds()

  const token = localStorage.getItem('aiTodoToken')

  if (!token) {
    isBooting.value = false
    return
  }

  try {
    user.value = await getCurrentUser()
    await Promise.all([refreshTasks(), refreshGroups()])
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
  window.removeEventListener('resize', updateDetailPanelBounds)
  finishDetailResize()
  window.clearTimeout(searchTimer)
  window.clearTimeout(aiCopyTimer)
  document.body.classList.remove('modal-open')
})

function readStoredDetailPanelWidth() {
  try {
    const storedWidth = Number(localStorage.getItem(DETAIL_PANEL_WIDTH_KEY))

    return Number.isFinite(storedWidth) && storedWidth > 0
      ? storedWidth
      : DETAIL_PANEL_DEFAULT_WIDTH
  } catch {
    return DETAIL_PANEL_DEFAULT_WIDTH
  }
}

function updateDetailPanelBounds() {
  const viewportWidth = window.innerWidth
  const sidebarWidth = viewportWidth > 1240 ? 240 : viewportWidth > 900 ? 82 : 0
  const minimumBoardWidth = viewportWidth > 1240 ? 500 : 0
  const availableWidth = viewportWidth - sidebarWidth - minimumBoardWidth
  const maximumWidth = Math.min(DETAIL_PANEL_MAX_WIDTH, Math.max(DETAIL_PANEL_MIN_WIDTH, availableWidth))

  detailPanelBounds.min = Math.min(DETAIL_PANEL_MIN_WIDTH, maximumWidth)
  detailPanelBounds.max = maximumWidth

  if (viewportWidth > 680) {
    detailPanelWidth.value = clampDetailPanelWidth(detailPanelWidth.value)
  }
}

function clampDetailPanelWidth(width) {
  return Math.min(detailPanelBounds.max, Math.max(detailPanelBounds.min, Math.round(width)))
}

function persistDetailPanelWidth() {
  try {
    localStorage.setItem(DETAIL_PANEL_WIDTH_KEY, String(detailPanelWidth.value))
  } catch {
    // The resize remains available even when browser storage is disabled.
  }
}

function startDetailResize(event) {
  if (window.innerWidth <= 680 || (event.pointerType === 'mouse' && event.button !== 0)) {
    return
  }

  event.preventDefault()
  updateDetailPanelBounds()
  detailResizeStartX = event.clientX
  detailResizeStartWidth = detailPanelWidth.value
  isDetailResizing.value = true
  document.body.classList.add('detail-resizing')
  window.addEventListener('pointermove', handleDetailResize)
  window.addEventListener('pointerup', finishDetailResize)
  window.addEventListener('pointercancel', finishDetailResize)
}

function handleDetailResize(event) {
  if (!isDetailResizing.value) {
    return
  }

  detailPanelWidth.value = clampDetailPanelWidth(
    detailResizeStartWidth + detailResizeStartX - event.clientX
  )
}

function finishDetailResize() {
  if (isDetailResizing.value) {
    persistDetailPanelWidth()
  }

  isDetailResizing.value = false
  document.body.classList.remove('detail-resizing')
  window.removeEventListener('pointermove', handleDetailResize)
  window.removeEventListener('pointerup', finishDetailResize)
  window.removeEventListener('pointercancel', finishDetailResize)
}

function resetDetailPanelWidth() {
  updateDetailPanelBounds()
  detailPanelWidth.value = clampDetailPanelWidth(DETAIL_PANEL_DEFAULT_WIDTH)
  persistDetailPanelWidth()
}

function handleDetailResizeKeydown(event) {
  const resizeStep = event.shiftKey ? 64 : 24
  let nextWidth = detailPanelWidth.value

  if (event.key === 'ArrowLeft') {
    nextWidth += resizeStep
  } else if (event.key === 'ArrowRight') {
    nextWidth -= resizeStep
  } else if (event.key === 'Home') {
    nextWidth = detailPanelBounds.min
  } else if (event.key === 'End') {
    nextWidth = detailPanelBounds.max
  } else {
    return
  }

  event.preventDefault()
  updateDetailPanelBounds()
  detailPanelWidth.value = clampDetailPanelWidth(nextWidth)
  persistDetailPanelWidth()
}

function handleDocumentKeydown(event) {
  if (event.key !== 'Escape') {
    return
  }

  if (isConfirmationDialogOpen.value) {
    closeConfirmationDialog()
    return
  }

  if (isAiAdvisorOpen.value) {
    closeAiAdvisor()
    return
  }

  if (isComposerOpen.value && !isTaskSubmitting.value) {
    closeComposer()
    return
  }

  if (isGroupComposerOpen.value && !isGroupSubmitting.value) {
    closeGroupComposer()
    return
  }

  if (selectedTask.value) {
    closeTaskDetail()
  }
}

function syncBodyModalState() {
  document.body.classList.toggle(
    'modal-open',
    isAiAdvisorOpen.value || isConfirmationDialogOpen.value
  )
}

function handleDeleteDialogKeydown(event) {
  if (event.key !== 'Tab') {
    return
  }

  const buttons = [...event.currentTarget.querySelectorAll('button:not(:disabled)')]

  if (!buttons.length) {
    return
  }

  const firstButton = buttons[0]
  const lastButton = buttons[buttons.length - 1]

  if (event.shiftKey && document.activeElement === firstButton) {
    event.preventDefault()
    lastButton.focus()
  } else if (!event.shiftKey && document.activeElement === lastButton) {
    event.preventDefault()
    firstButton.focus()
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
      await Promise.all([refreshTasks(), refreshGroups()])
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
  composerError.value = ''
  errorMessage.value = ''
  successMessage.value = ''

  if (!isTaskValid.value) {
    composerError.value = '任务标题不能为空，且不能超过 100 个字符。'
    return
  }

  const due = resolveCreateDueValues()

  if (due.error) {
    composerError.value = due.error
    isCreateCustomDueOpen.value = true
    return
  }

  const pendingStepTitle = createStepDraft.value.trim()

  if (pendingStepTitle.length > 100) {
    composerError.value = '执行步骤不能超过 100 个字符。'
    return
  }

  if (pendingStepTitle && createStepDrafts.value.some((step) => (
    step.title.toLocaleLowerCase() === pendingStepTitle.toLocaleLowerCase()
  ))) {
    composerError.value = '这个执行步骤已经添加过了。'
    return
  }

  const requestedStatus = taskForm.status
  const stepTitles = [
    ...createStepDrafts.value.map((step) => step.title),
    ...(pendingStepTitle ? [pendingStepTitle] : [])
  ]

  isTaskSubmitting.value = true

  try {
    const created = await createTask({
      title: taskForm.title.trim(),
      description: taskForm.description.trim() || null,
      priority: taskForm.priority,
      dueAt: due.date ? `${due.date}T${due.time}` : null
    })

    const statusRequest = requestedStatus !== 'TODO'
      ? updateTaskStatus(created.id, { status: requestedStatus })
      : null
    const [statusResult, stepBatchResult] = await Promise.all([
      statusRequest
        ? Promise.allSettled([statusRequest]).then(([result]) => result)
        : Promise.resolve(null),
      createTaskStepBatches(created.id, stepTitles)
    ])
    const createdSteps = stepBatchResult.createdSteps
    const failedStatus = statusResult?.status === 'rejected'
    const failedSteps = Boolean(stepBatchResult.error)

    syncTaskStepStats(created.id, createdSteps)
    resetTaskForm()
    isComposerOpen.value = false
    taskPage.page = 1
    await refreshTasks()

    if (failedStatus || failedSteps) {
      const failures = [
        ...(failedStatus ? [`状态未保存：${statusResult.reason?.message || '请稍后重试'}`] : []),
        ...(failedSteps
          ? [`还有 ${stepBatchResult.unsavedStepCount} 个执行步骤未保存：${stepBatchResult.error.message || '请稍后重试'}`]
          : [])
      ]
      errorMessage.value = `任务已创建，但${failures.join('；')}。请打开任务后补充。`
    }
  } catch (error) {
    composerError.value = error.message || '创建任务失败。'
  } finally {
    isTaskSubmitting.value = false
  }
}

async function createTaskStepBatches(taskId, titles) {
  const createdSteps = []

  for (let index = 0; index < titles.length; index += TASK_STEP_BATCH_SIZE) {
    const batchTitles = titles.slice(index, index + TASK_STEP_BATCH_SIZE)

    try {
      const result = await createTaskStepsBatch(taskId, { titles: batchTitles })
      createdSteps.push(...(Array.isArray(result) ? result : []))
    } catch (error) {
      return {
        createdSteps,
        error,
        unsavedStepCount: titles.length - createdSteps.length
      }
    }
  }

  return {
    createdSteps,
    error: null,
    unsavedStepCount: 0
  }
}

async function fetchAllTaskRecords() {
  const pageSize = 50
  const firstPage = await listTasks({ page: 1, size: pageSize })

  if (Array.isArray(firstPage)) {
    return firstPage
  }

  const records = [...(firstPage.records || [])]
  const pages = Math.max(1, Number(firstPage.pages) || 1)

  for (let page = 2; page <= pages; page += 1) {
    const result = await listTasks({ page, size: pageSize })
    records.push(...(Array.isArray(result) ? result : result.records || []))
  }

  return records
}

function getTaskOrderStorageKey() {
  const identity = user.value?.id || user.value?.email || user.value?.username

  return identity ? `${TASK_ORDER_STORAGE_PREFIX}:${identity}` : ''
}

function readStoredTaskOrder() {
  const storageKey = getTaskOrderStorageKey()

  if (!storageKey) {
    return []
  }

  try {
    const value = JSON.parse(localStorage.getItem(storageKey) || '[]')

    return Array.isArray(value) ? value.map(String) : []
  } catch {
    return []
  }
}

function applyStoredTaskOrder(source) {
  const storedOrder = readStoredTaskOrder()

  if (!storedOrder.length) {
    return source
  }

  const taskById = new Map(source.map((task) => [String(task.id), task]))
  const orderedIds = new Set(storedOrder)
  const newTasks = source.filter((task) => !orderedIds.has(String(task.id)))
  const orderedTasks = storedOrder
    .map((taskId) => taskById.get(taskId))
    .filter(Boolean)

  return [...newTasks, ...orderedTasks]
}

function persistTaskOrder() {
  const storageKey = getTaskOrderStorageKey()

  if (!storageKey) {
    return
  }

  try {
    localStorage.setItem(
      storageKey,
      JSON.stringify(allTasksSnapshot.value.map((task) => String(task.id)))
    )
  } catch {
    // Dragging still works for the current session when storage is unavailable.
  }
}

function commitVisibleTaskOrder(orderedTasks) {
  const orderedIds = new Set(orderedTasks.map((task) => String(task.id)))
  let visibleIndex = 0

  allTasksSnapshot.value = allTasksSnapshot.value.map((task) => (
    orderedIds.has(String(task.id)) ? orderedTasks[visibleIndex++] : task
  ))
  persistTaskOrder()
  paginateCurrentView()
}

function handleTaskDragStart(event) {
  isTaskDragging.value = true
  draggingTaskId.value = event.item?.dataset.taskId || null
}

function handleTaskDragEnd(orderedTasks) {
  commitVisibleTaskOrder(orderedTasks)
  isTaskDragging.value = false
  draggingTaskId.value = null
  suppressTaskClickUntil = Date.now() + 220
}

function moveTaskWithKeyboard(task, orderedTasks, direction) {
  const currentIndex = orderedTasks.findIndex((item) => item.id === task.id)
  const targetIndex = currentIndex + direction

  if (currentIndex < 0 || targetIndex < 0 || targetIndex >= orderedTasks.length) {
    return
  }

  const reorderedTasks = [...orderedTasks]
  const [movedTask] = reorderedTasks.splice(currentIndex, 1)
  reorderedTasks.splice(targetIndex, 0, movedTask)
  commitVisibleTaskOrder(reorderedTasks)
}

function handleTaskItemClick(task) {
  if (isTaskDragging.value || Date.now() < suppressTaskClickUntil) {
    return
  }

  openTaskDetail(task)
}

function filterTasksForCurrentView(source) {
  let filtered = [...source]

  if (activeView.value === 'today') {
    filtered = filtered.filter((task) => task.status !== 'DONE' && isTaskDueToday(task))
  } else if (activeView.value === 'planned') {
    filtered = filtered.filter((task) => task.status !== 'DONE' && Boolean(task.dueAt))
  } else if (activeView.value === 'important') {
    filtered = filtered.filter((task) => task.status !== 'DONE' && task.priority === 'HIGH')
  } else if (activeView.value === 'progress') {
    filtered = filtered.filter((task) => task.status === 'IN_PROGRESS')
  } else if (activeView.value === 'done') {
    filtered = filtered.filter((task) => task.status === 'DONE')
  }

  if (listFilters.status) {
    filtered = filtered.filter((task) => task.status === listFilters.status)
  }

  if (listFilters.priority) {
    filtered = filtered.filter((task) => task.priority === listFilters.priority)
  }

  const keyword = query.value.trim().toLocaleLowerCase()

  if (keyword) {
    filtered = filtered.filter((task) => (
      task.title?.toLocaleLowerCase().includes(keyword) ||
      task.description?.toLocaleLowerCase().includes(keyword)
    ))
  }

  return filtered
}

function paginateCurrentView() {
  const filtered = filterTasksForCurrentView(allTasksSnapshot.value)
  const pages = Math.max(1, Math.ceil(filtered.length / taskPage.size))

  taskPage.page = Math.min(Math.max(1, taskPage.page), pages)
  taskPage.total = filtered.length
  taskPage.pages = pages

  const start = (taskPage.page - 1) * taskPage.size
  tasks.value = filtered.slice(start, start + taskPage.size)
}

function removeOrphanedTaskStepStats() {
  const taskIds = new Set(allTasksSnapshot.value.map((task) => task.id))

  for (const taskId of taskStepStatsById.keys()) {
    if (!taskIds.has(taskId)) {
      taskStepStatsById.delete(taskId)
    }
  }
}

function isTaskDueToday(task) {
  return formatDateKey(task?.dueAt) === todayKey.value
}

function createTaskStats(source) {
  const list = Array.isArray(source) ? source : []
  const now = Date.now()

  return {
    total: list.length,
    todo: list.filter((task) => task.status === 'TODO').length,
    inProgress: list.filter((task) => task.status === 'IN_PROGRESS').length,
    done: list.filter((task) => task.status === 'DONE').length,
    highPriority: list.filter((task) => task.priority === 'HIGH').length,
    dueToday: list.filter(isTaskDueToday).length,
    overdue: list.filter((task) => {
      const due = parseLocalDateTime(task.dueAt)
      return task.status !== 'DONE' && Boolean(due && due.getTime() < now)
    }).length
  }
}

async function refreshTasks() {
  isTaskListLoading.value = true
  errorMessage.value = ''

  try {
    allTasksSnapshot.value = applyStoredTaskOrder(await fetchAllTaskRecords())
    paginateCurrentView()

    void refreshTaskStepStats(tasks.value)
    removeOrphanedTaskStepStats()

    if (selectedTask.value) {
      const latest = tasks.value.find((task) => task.id === selectedTask.value.id)
      selectedTask.value = latest || null
    }

    await Promise.all([
      refreshTaskStats(),
      refreshTaskReminders()
    ])
  } catch (error) {
    errorMessage.value = error.message || '任务加载失败，请稍后重试。'
  } finally {
    isTaskListLoading.value = false
  }
}

function normalizeGroup(group) {
  if (!group || typeof group !== 'object') {
    return group
  }

  const responseRole = group.currentUserRole || group.currentUsserRole || group.role
  const inferredRole = String(group.ownerId) === String(user.value?.id) ? 'OWNER' : 'MEMBER'

  return {
    ...group,
    currentUserRole: responseRole || inferredRole
  }
}

async function refreshGroups() {
  isGroupListLoading.value = true
  groupListError.value = ''

  try {
    const result = await listGroups()
    groups.value = Array.isArray(result) ? result.map(normalizeGroup) : []

    if (selectedGroup.value) {
      const current = groups.value.find((group) => String(group.id) === String(selectedGroup.value.id))
      selectedGroup.value = current || selectedGroup.value
    }
  } catch (error) {
    groupListError.value = error.message || '工作组加载失败，请稍后重试。'
  } finally {
    isGroupListLoading.value = false
  }
}

async function refreshTaskStats() {
  try {
    Object.assign(taskStats, await getTaskStats())
  } catch {
    Object.assign(taskStats, createTaskStats(allTasksSnapshot.value))
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
  groupDetailRequestId += 1
  localStorage.removeItem('aiTodoToken')
  user.value = null
  tasks.value = []
  allTasksSnapshot.value = []
  taskStepStatsById.clear()
  reminders.value = []
  groups.value = []
  selectedGroup.value = null
  groupMembers.value = []
  groupListError.value = ''
  groupDetailError.value = ''
  isGroupComposerOpen.value = false
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

async function openLogoutConfirm() {
  isLogoutConfirmOpen.value = true
  isSidebarOpen.value = false
  deleteDialogError.value = ''
  syncBodyModalState()

  await nextTick()
  deleteCancelButtonRef.value?.focus()
}

function confirmLogout() {
  isLogoutConfirmOpen.value = false
  syncBodyModalState()
  logout()
}

function confirmDialogAction() {
  if (isLogoutConfirmOpen.value) {
    confirmLogout()
    return
  }

  return confirmDeleteTask()
}

async function selectView(key) {
  groupDetailRequestId += 1
  isGroupDetailLoading.value = false
  selectedGroup.value = null
  groupMembers.value = []
  groupDetailError.value = ''
  closeGroupComposer()
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

function formatShortDate(date) {
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

function getNextWeekEndDate() {
  const nextWeekEnd = new Date()
  const currentDay = nextWeekEnd.getDay()
  const daysUntilNextWeekEnd = currentDay === 0 ? 7 : 14 - currentDay
  nextWeekEnd.setDate(nextWeekEnd.getDate() + daysUntilNextWeekEnd)
  return nextWeekEnd
}

function resolveDuePresetDate(preset) {
  const date = new Date()

  if (preset === 'tomorrow') {
    date.setDate(date.getDate() + 1)
  } else if (preset === 'next-week') {
    return getNextWeekEndDate()
  }

  return date
}

function applyCreateDuePreset(preset) {
  setCreateDueParts(toLocalDateKey(resolveDuePresetDate(preset)), END_OF_DAY_TIME)
  isCreateCustomDueOpen.value = false
}

function isCreateDuePresetActive(preset) {
  const due = resolveCreateDueValues()
  return !due.error
    && due.date === toLocalDateKey(resolveDuePresetDate(preset))
    && due.time === END_OF_DAY_TIME
}

function clearCreateDue() {
  taskForm.dueDate = ''
  taskForm.dueTime = ''
  setCreateDueParts('', '')
  composerError.value = ''
}

function toggleCreateCustomDue() {
  isCreateCustomDueOpen.value = !isCreateCustomDueOpen.value

  if (isCreateCustomDueOpen.value) {
    nextTick(() => {
      document.querySelector('.create-due-section .due-custom-fields')?.scrollIntoView({
        behavior: 'smooth',
        block: 'nearest'
      })
    })
  }
}

function updateCreateDatePart(part, value) {
  const maxLength = part === 'year' ? 4 : 2
  createDueParts[part] = String(value).replace(/\D/g, '').slice(0, maxLength)
  composerError.value = ''
}

function updateCreateTimePart(part, value) {
  createDueParts[part] = String(value).replace(/\D/g, '').slice(0, 2)
  composerError.value = ''
}

function setCreateDueParts(date, time) {
  const dateParts = splitDateParts(date)
  const timeParts = splitTimeParts(time)

  Object.assign(createDueParts, {
    year: dateParts.year,
    month: dateParts.month,
    day: dateParts.day,
    hour: timeParts.hour,
    minute: timeParts.minute
  })
  taskForm.dueDate = date
  taskForm.dueTime = time
  composerError.value = ''
}

function resolveCreateDueValues() {
  return resolveDuePartValues(createDueParts)
}

function applyEditDuePreset(preset) {
  setEditDueParts(toLocalDateKey(resolveDuePresetDate(preset)), END_OF_DAY_TIME)
  isEditCustomDueOpen.value = false
}

function isEditDuePresetActive(preset) {
  const due = resolveEditDueValues()
  return !due.error
    && due.date === toLocalDateKey(resolveDuePresetDate(preset))
    && due.time === END_OF_DAY_TIME
}

function toggleEditCustomDue() {
  isEditCustomDueOpen.value = !isEditCustomDueOpen.value

  if (isEditCustomDueOpen.value) {
    nextTick(() => {
      detailPropertiesRef.value?.querySelector('.due-custom-fields')?.scrollIntoView({
        behavior: 'smooth',
        block: 'nearest'
      })
    })
  }
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
  return resolveDuePartValues(editDueParts)
}

function resolveDuePartValues(parts) {
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
  if (isComposerOpen.value) {
    closeComposer()
  }
  selectedTask.value = task
  expandedDetailSection.value = null
  isEditCustomDueOpen.value = false
  taskSteps.value = []
  stepDraft.value = ''
  resetAiStepDraft()
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
    isEditCustomDueOpen.value = true
    nextTick(() => {
      detailPropertiesRef.value?.querySelector('.due-item')?.scrollIntoView({
        behavior: 'smooth',
        block: 'nearest'
      })
    })
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
    await refreshTasks()
  } catch (error) {
    detailError.value = error.message || '更新任务失败。'
  } finally {
    isDetailSaving.value = false
  }
}

async function openDeleteConfirm() {
  const task = selectedTask.value

  if (!task) {
    return
  }

  deleteCandidate.value = {
    id: task.id,
    title: task.title
  }
  deleteDialogError.value = ''
  syncBodyModalState()

  await nextTick()
  deleteCancelButtonRef.value?.focus()
}

function closeConfirmationDialog() {
  if (isTaskDeleting.value) {
    return
  }

  deleteCandidate.value = null
  isLogoutConfirmOpen.value = false
  deleteDialogError.value = ''
  syncBodyModalState()
}

async function confirmDeleteTask() {
  const task = deleteCandidate.value

  if (!task || isTaskDeleting.value) {
    return
  }

  deleteDialogError.value = ''
  isTaskDeleting.value = true

  try {
    await deleteTask(task.id)
  } catch (error) {
    deleteDialogError.value = error.message || '删除任务失败，请稍后重试。'
    isTaskDeleting.value = false
    return
  }

  taskStepStatsById.delete(task.id)
  isTaskDeleting.value = false
  deleteCandidate.value = null
  syncBodyModalState()
  closeTaskDetail()
  await refreshTasks()
}

async function handleStatusChange(task, status) {
  if (!task || task.status === status) {
    return
  }

  errorMessage.value = ''
  detailError.value = ''

  try {
    const updated = await updateTaskStatus(task.id, { status })

    if (selectedTask.value?.id === updated.id) {
      selectedTask.value = updated
      fillEditForm(updated)
    }

    await refreshTasks()

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

function closeTaskDetail() {
  selectedTask.value = null
  expandedDetailSection.value = null
  isEditCustomDueOpen.value = false
  taskSteps.value = []
  stepDraft.value = ''
  resetAiStepDraft()
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

function resetAiStepDraft() {
  isAiStepDraftOpen.value = false
  isAiStepDraftLoading.value = false
  isAiStepDraftSaving.value = false
  aiStepInstruction.value = ''
  aiStepDrafts.value = []
  aiStepDraftError.value = ''
  aiStepDraftMessage.value = ''
}

function toggleAiStepDraftPanel() {
  isAiStepDraftOpen.value = !isAiStepDraftOpen.value
  aiStepDraftError.value = ''
  aiStepDraftMessage.value = ''
}

function applyAiStepInstructionPreset(instruction) {
  aiStepInstruction.value = instruction
  aiStepDraftError.value = ''
}

async function handleGenerateAiStepDrafts() {
  const taskId = selectedTask.value?.id
  const instruction = aiStepInstruction.value.trim()

  aiStepDraftError.value = ''
  aiStepDraftMessage.value = ''

  if (!taskId || isAiStepDraftLoading.value) {
    return
  }

  if (instruction.length > 500) {
    aiStepDraftError.value = '任务拆解要求不能超过 500 个字符。'
    return
  }

  isAiStepDraftLoading.value = true

  try {
    const result = await generateTaskStepDrafts(taskId, {
      instruction: instruction || null
    })
    const steps = Array.isArray(result?.steps) ? result.steps : []

    if (!steps.length) {
      throw new Error('AI 服务没有返回可用的步骤草稿。')
    }

    aiStepDrafts.value = steps.map((title, index) => ({
      id: `${Date.now()}-${index}`,
      title: String(title).trim(),
      selected: true
    }))
  } catch (error) {
    aiStepDrafts.value = []
    aiStepDraftError.value = error.message || '生成步骤草稿失败。'
  } finally {
    isAiStepDraftLoading.value = false
  }
}

function toggleAiStepDraft(draft) {
  draft.selected = !draft.selected
  aiStepDraftError.value = ''
  aiStepDraftMessage.value = ''
}

function toggleAllAiStepDrafts() {
  const nextSelected = !areAllAiStepDraftsSelected.value

  aiStepDrafts.value.forEach((draft) => {
    draft.selected = nextSelected
  })
}

function removeAiStepDraft(draftId) {
  aiStepDrafts.value = aiStepDrafts.value.filter((draft) => draft.id !== draftId)
  aiStepDraftError.value = ''
  aiStepDraftMessage.value = ''
}

async function handleSaveAiStepDrafts() {
  const taskId = selectedTask.value?.id

  aiStepDraftError.value = ''
  aiStepDraftMessage.value = ''

  if (!taskId || isAiStepDraftSaving.value) {
    return
  }

  if (!selectedAiStepDrafts.value.length) {
    aiStepDraftError.value = '请至少选择一个需要保存的步骤。'
    return
  }

  const knownTitles = new Set(taskSteps.value.map((step) => step.title.trim().toLocaleLowerCase()))
  const duplicateIds = new Set()
  const candidates = []

  for (const draft of selectedAiStepDrafts.value) {
    const title = draft.title.trim()
    const normalizedTitle = title.toLocaleLowerCase()

    if (!title || title.length > 100) {
      aiStepDraftError.value = '步骤标题不能为空，且不能超过 100 个字符。'
      return
    }

    if (knownTitles.has(normalizedTitle)) {
      duplicateIds.add(draft.id)
      continue
    }

    knownTitles.add(normalizedTitle)
    candidates.push({ ...draft, title })
  }

  if (!candidates.length) {
    aiStepDrafts.value.forEach((draft) => {
      if (duplicateIds.has(draft.id)) {
        draft.selected = false
      }
    })
    aiStepDraftError.value = '选中的草稿与现有步骤重复，请调整后再保存。'
    return
  }

  isAiStepDraftSaving.value = true

  try {
    const batchResult = await createTaskStepBatches(taskId, candidates.map((draft) => draft.title))
    const createdSteps = batchResult.createdSteps
    const savedDraftIds = new Set(
      candidates.slice(0, createdSteps.length).map((draft) => draft.id)
    )

    if (createdSteps.length && selectedTask.value?.id === taskId) {
      taskSteps.value = [...taskSteps.value, ...createdSteps]
      syncTaskStepStats(taskId, taskSteps.value)
    } else if (createdSteps.length) {
      void refreshTaskStepStats([{ id: taskId }])
    }

    aiStepDrafts.value = aiStepDrafts.value
      .filter((draft) => !savedDraftIds.has(draft.id))
      .map((draft) => duplicateIds.has(draft.id) ? { ...draft, selected: false } : draft)

    if (createdSteps.length) {
      const duplicateText = duplicateIds.size ? `，另有 ${duplicateIds.size} 条重复草稿已取消选择` : ''
      aiStepDraftMessage.value = `已加入 ${createdSteps.length} 个执行步骤${duplicateText}。`
    }

    if (batchResult.error) {
      aiStepDraftError.value = `还有 ${batchResult.unsavedStepCount} 个步骤未保存：${batchResult.error.message || '请重试。'}`
    }
  } catch (error) {
    aiStepDrafts.value = aiStepDrafts.value
      .map((draft) => duplicateIds.has(draft.id) ? { ...draft, selected: false } : draft)
    aiStepDraftError.value = error.message || '批量保存步骤失败，请重试。'
  } finally {
    isAiStepDraftSaving.value = false
  }
}

async function openAiAdvisor() {
  isAiAdvisorOpen.value = true
  if (isComposerOpen.value) {
    closeComposer()
  }
  isFilterOpen.value = false
  isReminderOpen.value = false
  isSidebarOpen.value = false
  aiError.value = ''
  syncBodyModalState()

  await nextTick()
  aiMessageInputRef.value?.focus()
}

function closeAiAdvisor() {
  isAiAdvisorOpen.value = false
  aiCopied.value = false
  syncBodyModalState()
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

function groupRoleLabel(role) {
  const labels = {
    OWNER: '负责人',
    MEMBER: '成员'
  }

  return labels[role] || '成员'
}

function groupInitial(name) {
  return String(name || '组').trim().slice(0, 1).toLocaleUpperCase() || '组'
}

function formatGroupDate(value) {
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

async function selectGroup(group) {
  const normalizedGroup = normalizeGroup(group)
  const requestId = ++groupDetailRequestId

  if (selectedTask.value) {
    closeTaskDetail()
  }

  if (isComposerOpen.value) {
    closeComposer()
  }

  if (isGroupComposerOpen.value) {
    closeGroupComposer()
  }

  selectedGroup.value = normalizedGroup
  groupMembers.value = []
  groupDetailError.value = ''
  isGroupDetailLoading.value = true
  isSidebarOpen.value = false
  isFilterOpen.value = false
  isReminderOpen.value = false

  const [detailResult, membersResult] = await Promise.allSettled([
    getGroup(normalizedGroup.id),
    listGroupMembers(normalizedGroup.id)
  ])

  if (requestId !== groupDetailRequestId) {
    return
  }

  if (detailResult.status === 'fulfilled') {
    const detail = normalizeGroup(detailResult.value)
    selectedGroup.value = detail
    groups.value = [
      detail,
      ...groups.value.filter((item) => String(item.id) !== String(detail.id))
    ]
  } else {
    groupDetailError.value = detailResult.reason?.message || '工作组详情加载失败。'
  }

  if (membersResult.status === 'fulfilled') {
    groupMembers.value = Array.isArray(membersResult.value) ? membersResult.value : []
  } else {
    const message = membersResult.reason?.message || '成员列表加载失败。'
    groupDetailError.value = groupDetailError.value
      ? `${groupDetailError.value}；${message}`
      : message
  }

  isGroupDetailLoading.value = false
}

function resetGroupForm() {
  groupForm.name = ''
  groupForm.description = ''
  groupFormError.value = ''
}

function openGroupComposer() {
  if (selectedTask.value) {
    closeTaskDetail()
  }

  if (isComposerOpen.value) {
    closeComposer()
  }

  resetGroupForm()
  isGroupComposerOpen.value = true
  isSidebarOpen.value = false
}

function closeGroupComposer() {
  if (isGroupSubmitting.value) {
    return
  }

  isGroupComposerOpen.value = false
  resetGroupForm()
}

async function handleCreateGroup() {
  groupFormError.value = ''

  if (!isGroupFormValid.value) {
    groupFormError.value = '请填写 1 到 100 个字符的工作组名称，并将描述控制在 500 个字符内。'
    return
  }

  isGroupSubmitting.value = true

  try {
    const created = normalizeGroup(await createGroup({
      name: groupForm.name.trim(),
      description: groupForm.description.trim() || null
    }))

    groups.value = [
      created,
      ...groups.value.filter((group) => String(group.id) !== String(created.id))
    ]
    isGroupComposerOpen.value = false
    resetGroupForm()
    await selectGroup(created)
  } catch (error) {
    groupFormError.value = error.message || '工作组创建失败，请稍后重试。'
  } finally {
    isGroupSubmitting.value = false
  }
}

function openComposer() {
  groupDetailRequestId += 1
  isGroupDetailLoading.value = false
  selectedGroup.value = null
  groupMembers.value = []
  closeGroupComposer()
  if (selectedTask.value) {
    closeTaskDetail()
  }

  resetTaskForm()

  if (activeView.value === 'today') {
    setCreateDueParts(todayKey.value, '23:59')
  }

  isComposerOpen.value = true
  isFilterOpen.value = false
  isReminderOpen.value = false
  isSidebarOpen.value = false
}

function closeComposer() {
  if (isTaskSubmitting.value) {
    return
  }

  isComposerOpen.value = false
  resetTaskForm()
}

function resetTaskForm() {
  taskForm.title = ''
  taskForm.description = ''
  taskForm.status = 'TODO'
  taskForm.priority = 'MEDIUM'
  taskForm.dueDate = ''
  taskForm.dueTime = ''
  setCreateDueParts('', '')
  createStepDraft.value = ''
  createStepDrafts.value = []
  isCreateCustomDueOpen.value = false
  composerError.value = ''
}

function addCreateStep() {
  const title = createStepDraft.value.trim()
  composerError.value = ''

  if (!title) {
    return
  }

  if (title.length > 100) {
    composerError.value = '执行步骤不能超过 100 个字符。'
    return
  }

  const isDuplicate = createStepDrafts.value.some((step) => (
    step.title.toLocaleLowerCase() === title.toLocaleLowerCase()
  ))

  if (isDuplicate) {
    composerError.value = '这个执行步骤已经添加过了。'
    return
  }

  createStepDraftId += 1
  createStepDrafts.value.push({ id: createStepDraftId, title })
  createStepDraft.value = ''
}

function removeCreateStep(stepId) {
  createStepDrafts.value = createStepDrafts.value.filter((step) => step.id !== stepId)
  composerError.value = ''
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

  <main
    v-else
    class="todo-app"
    :class="{ 'has-detail': selectedTask || isComposerOpen || isGroupComposerOpen, 'is-detail-resizing': isDetailResizing }"
    :style="{ '--detail-panel-width': `${detailPanelWidth}px` }"
  >
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

      <div class="sidebar-scroll-area">
        <nav class="nav-list" aria-label="任务视图">
          <button
            v-for="view in views"
            :key="view.key"
            type="button"
            :class="{ active: !selectedGroup && activeView === view.key }"
            :aria-current="!selectedGroup && activeView === view.key ? 'page' : undefined"
            @click="selectView(view.key)"
          >
            <component :is="view.icon" :size="18" />
            <span>{{ view.label }}</span>
            <em>{{ view.count }}</em>
          </button>
        </nav>

        <section class="sidebar-groups" aria-labelledby="sidebar-groups-title">
          <header class="sidebar-group-heading">
            <span id="sidebar-groups-title">工作组</span>
            <button type="button" aria-label="创建工作组" title="创建工作组" @click="openGroupComposer">
              <FolderPlus :size="15" />
            </button>
          </header>

          <div v-if="isGroupListLoading" class="group-nav-loading" aria-label="正在加载工作组">
            <span></span>
            <span></span>
          </div>

          <div v-else-if="groups.length" class="group-nav-list">
            <button
              v-for="group in groups"
              :key="group.id"
              type="button"
              class="group-nav-item"
              :class="{ active: String(selectedGroup?.id) === String(group.id) }"
              :aria-current="String(selectedGroup?.id) === String(group.id) ? 'page' : undefined"
              :title="group.name"
              @click="selectGroup(group)"
            >
              <span class="group-nav-avatar">{{ groupInitial(group.name) }}</span>
              <span class="group-nav-copy">{{ group.name }}</span>
              <Crown v-if="group.currentUserRole === 'OWNER'" class="group-nav-role" :size="13" />
              <UsersRound v-else class="group-nav-role" :size="13" />
            </button>
          </div>

          <button v-else-if="!groupListError" class="group-nav-empty" type="button" @click="openGroupComposer">
            <Plus :size="14" />
            <span>创建工作组</span>
          </button>

          <div v-if="groupListError" class="group-nav-error">
            <span>{{ groupListError }}</span>
            <button type="button" aria-label="重新加载工作组" title="重试" @click="refreshGroups">
              <RefreshCw :size="13" />
            </button>
          </div>
        </section>
      </div>

      <div class="sidebar-footer">
        <button class="ghost-button" type="button" @click="openLogoutConfirm">
          <LogOut :size="17" />
          <span>退出登录</span>
        </button>
      </div>
    </aside>

    <section class="task-board" :class="{ 'group-board': selectedGroup }">
      <template v-if="selectedGroup">
        <header class="board-header group-board-header">
          <div class="board-title">
            <button class="mobile-menu-button" type="button" aria-label="打开导航" @click="isSidebarOpen = true">
              <Menu :size="20" />
            </button>
            <p class="date-line">协作工作组</p>
            <h1>{{ selectedGroup.name }}</h1>
          </div>

          <div class="board-primary-actions group-primary-actions">
            <button class="primary-button group-create-trigger" type="button" @click="openGroupComposer">
              <FolderPlus :size="17" />
              <span>新建工作组</span>
            </button>
          </div>
        </header>

        <div class="board-summary group-summary" aria-label="工作组概览">
          <span><b>{{ groupMembers.length }}</b> 位成员</span>
          <span class="group-role-summary">
            <Crown v-if="selectedGroup.currentUserRole === 'OWNER'" :size="13" />
            <UsersRound v-else :size="13" />
            {{ selectedGroupRole }}
          </span>
          <span>创建于 {{ formatGroupDate(selectedGroup.createdAt) }}</span>
        </div>

        <div v-if="groupDetailError" class="notice error list-error" role="alert">
          <span>{{ groupDetailError }}</span>
          <button type="button" @click="selectGroup(selectedGroup)">重试</button>
        </div>

        <div v-if="isGroupDetailLoading" class="group-workspace group-workspace-loading" aria-label="正在加载工作组">
          <div class="group-profile-skeleton"><span></span><i></i><i></i></div>
          <div class="group-member-skeleton" v-for="index in 3" :key="index"><span></span><i></i></div>
        </div>

        <section v-else class="group-workspace">
          <header class="group-profile-band">
            <span class="group-profile-mark">{{ groupInitial(selectedGroup.name) }}</span>
            <div class="group-profile-copy">
              <span>WORKSPACE</span>
              <h2>{{ selectedGroup.name }}</h2>
              <p>{{ selectedGroup.description || '这个工作组暂时没有填写描述。' }}</p>
            </div>
            <span class="group-role-badge" :class="{ owner: selectedGroup.currentUserRole === 'OWNER' }">
              <Crown v-if="selectedGroup.currentUserRole === 'OWNER'" :size="14" />
              <UsersRound v-else :size="14" />
              {{ selectedGroupRole }}
            </span>
          </header>

          <div class="group-workspace-divider"></div>

          <section class="group-members-section">
            <header>
              <div>
                <span>成员</span>
                <strong>{{ groupMembers.length }}</strong>
              </div>
              <small>按加入时间排列</small>
            </header>

            <div v-if="groupMembers.length" class="group-member-list">
              <article v-for="member in groupMembers" :key="member.userId" class="group-member-row">
                <span class="group-member-avatar">{{ groupInitial(member.username) }}</span>
                <div>
                  <strong>{{ member.username }}</strong>
                  <small>{{ String(member.userId) === String(selectedGroup.ownerId) ? '工作组创建者' : '工作组成员' }}</small>
                </div>
                <span class="group-member-role" :class="{ owner: member.role === 'OWNER' }">
                  <Crown v-if="member.role === 'OWNER'" :size="13" />
                  <UsersRound v-else :size="13" />
                  {{ groupRoleLabel(member.role) }}
                </span>
                <time>{{ formatGroupDate(member.joinedAt) }}加入</time>
              </article>
            </div>

            <div v-else class="group-members-empty">
              <UsersRound :size="28" />
              <span>暂无成员信息</span>
            </div>
          </section>
        </section>
      </template>

      <template v-else>
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
        <span><b>{{ currentViewStats.total }}</b> 当前视图</span>
        <span><b>{{ currentViewStats.todo }}</b> 待办</span>
        <span><b>{{ currentViewStats.dueToday }}</b> 今天截止</span>
        <span v-if="currentViewStats.overdue" class="summary-overdue"><b>{{ currentViewStats.overdue }}</b> 已逾期</span>
      </div>

      <div v-if="errorMessage" class="notice error list-error" role="alert">
        <span>{{ errorMessage }}</span>
        <button type="button" @click="refreshTasks">重试</button>
      </div>
      <div v-if="isTaskListLoading" class="task-list">
        <template v-for="index in 5" :key="index">
          <div class="task-skeleton" aria-hidden="true">
            <span></span>
            <div><i></i><i></i></div>
            <em></em>
          </div>
        </template>
      </div>

      <div v-else-if="visibleTasks.length" class="task-groups">
        <section
          v-for="group in visibleTaskGroups"
          :key="group.key"
          class="task-group"
          :class="{ 'completed-task-group': group.completed }"
        >
          <button
            v-if="group.completed"
            class="completed-group-toggle"
            type="button"
            :aria-expanded="isCompletedGroupOpen"
            @click="isCompletedGroupOpen = !isCompletedGroupOpen"
          >
            <ChevronRight class="completed-group-chevron" :class="{ open: isCompletedGroupOpen }" :size="17" />
            <span>已完成</span>
            <b>{{ group.tasks.length }}</b>
          </button>

          <Transition name="completed-list">
            <VueDraggable
              v-show="!group.completed || isCompletedGroupOpen"
              :list="group.tasks"
              item-key="id"
              tag="div"
              class="task-list"
              :class="{ 'completed-task-list': group.completed }"
              :group="{ name: group.completed ? 'completed-tasks' : 'active-tasks', pull: false, put: false }"
              handle=".task-drag-handle"
              ghost-class="task-drag-ghost"
              chosen-class="task-drag-chosen"
              drag-class="task-drag-active"
              :animation="190"
              :delay="150"
              :delay-on-touch-only="true"
              :touch-start-threshold="4"
              @start="handleTaskDragStart"
              @end="handleTaskDragEnd(group.tasks)"
            >
              <template #header>
                <div v-if="!group.completed" class="task-list-columns" aria-hidden="true">
                  <span></span>
                  <span>任务</span>
                  <span>执行进度</span>
                  <span>截止时间</span>
                </div>
              </template>

              <template #item="{ element: task }">
                <article
                  class="task-item"
                  :class="[
                    `status-${task.status}`,
                    {
                      selected: selectedTask?.id === task.id,
                      done: task.status === 'DONE',
                      dragging: String(draggingTaskId) === String(task.id)
                    }
                  ]"
                  :data-task-id="task.id"
                  tabindex="0"
                  @click="handleTaskItemClick(task)"
                  @keydown.enter="handleTaskItemClick(task)"
                >
                  <div class="task-leading-actions" @click.stop>
                    <button
                      class="task-check"
                      type="button"
                      :class="{ done: task.status === 'DONE' }"
                      :aria-label="task.status === 'DONE' ? '恢复任务' : '完成任务'"
                      @click="toggleTaskDone(task)"
                    >
                      <Check v-if="task.status === 'DONE'" :size="14" />
                    </button>
                    <button
                      class="task-drag-handle"
                      type="button"
                      :aria-label="`调整任务“${task.title}”的顺序`"
                      title="拖动排序；也可使用上下方向键"
                      @keydown.up.stop.prevent="moveTaskWithKeyboard(task, group.tasks, -1)"
                      @keydown.down.stop.prevent="moveTaskWithKeyboard(task, group.tasks, 1)"
                    >
                      <GripVertical :size="15" />
                    </button>
                  </div>
                  <div class="task-content">
                    <div class="task-title-row">
                      <h2>{{ task.title }}</h2>
                      <span
                        class="task-priority-mark"
                        :class="`priority-${task.priority || 'MEDIUM'}`"
                        :title="`${priorityText(task.priority)}优先级`"
                        :aria-label="`${priorityText(task.priority)}优先级`"
                      >
                        <Flag :size="11" />
                        <span>{{ priorityText(task.priority) }}</span>
                      </span>
                      <span v-if="task.status !== 'DONE'" class="task-status-mark" :class="`status-${task.status}`">
                        <Circle v-if="task.status === 'TODO'" :size="10" />
                        <RefreshCw v-else :size="10" />
                        {{ statusText(task.status) }}
                      </span>
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
              </template>
            </VueDraggable>
          </Transition>
        </section>
      </div>

      <div v-else class="task-list">
        <section class="empty-panel">
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
      </template>

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

      <Teleport to="body">
        <Transition name="delete-dialog">
          <div v-if="isConfirmationDialogOpen" class="delete-dialog-overlay" @click.self="closeConfirmationDialog">
            <section
              class="delete-dialog"
              :class="{ 'logout-dialog': isLogoutConfirmOpen }"
              role="alertdialog"
              aria-modal="true"
              aria-labelledby="delete-dialog-title"
              aria-describedby="delete-dialog-description"
              @keydown="handleDeleteDialogKeydown"
            >
              <header class="delete-dialog-header">
                <span class="delete-dialog-mark">
                  <LogOut v-if="isLogoutConfirmOpen" :size="20" />
                  <Trash2 v-else :size="20" />
                </span>
                <span>{{ isLogoutConfirmOpen ? 'SIGN OUT' : 'DELETE TASK' }}</span>
                <button
                  class="icon-button delete-dialog-close"
                  type="button"
                  :aria-label="isLogoutConfirmOpen ? '关闭退出确认' : '关闭删除确认'"
                  :disabled="isTaskDeleting"
                  @click="closeConfirmationDialog"
                >
                  <X :size="18" />
                </button>
              </header>

              <div class="delete-dialog-copy">
                <h2 id="delete-dialog-title">{{ isLogoutConfirmOpen ? '退出当前账号？' : '删除这个任务？' }}</h2>
                <p id="delete-dialog-description">
                  <template v-if="isLogoutConfirmOpen">
                    退出后将清除本机的登录状态，你的任务数据仍会保留在账户中。
                  </template>
                  <template v-else>
                    “{{ deleteCandidate.title }}”将从任务列表中永久移除。
                  </template>
                </p>
              </div>

              <div class="delete-dialog-warning">
                <LogOut v-if="isLogoutConfirmOpen" :size="16" />
                <History v-else :size="16" />
                <span>{{ isLogoutConfirmOpen ? '重新登录后可继续管理任务' : '此操作无法撤销' }}</span>
              </div>

              <p v-if="!isLogoutConfirmOpen && deleteDialogError" class="delete-dialog-error" role="alert">
                {{ deleteDialogError }}
              </p>

              <footer class="delete-dialog-actions">
                <button
                  ref="deleteCancelButtonRef"
                  class="delete-cancel-button"
                  type="button"
                  :disabled="isTaskDeleting"
                  @click="closeConfirmationDialog"
                >
                  取消
                </button>
                <button
                  class="delete-confirm-button"
                  :class="{ 'logout-confirm-button': isLogoutConfirmOpen }"
                  type="button"
                  :disabled="isTaskDeleting"
                  @click="confirmDialogAction"
                >
                  <RefreshCw v-if="isTaskDeleting" class="spin-icon" :size="16" />
                  <LogOut v-else-if="isLogoutConfirmOpen" :size="16" />
                  <Trash2 v-else :size="16" />
                  <span>{{ isTaskDeleting ? '正在删除' : isLogoutConfirmOpen ? '退出登录' : '确认删除' }}</span>
                </button>
              </footer>
            </section>
          </div>
        </Transition>
      </Teleport>
    </section>

    <aside v-if="isGroupComposerOpen" class="detail-panel create-panel group-create-panel">
      <div
        class="detail-resize-handle"
        role="separator"
        aria-label="调整工作组创建栏宽度"
        aria-orientation="vertical"
        :aria-valuemin="detailPanelBounds.min"
        :aria-valuemax="detailPanelBounds.max"
        :aria-valuenow="detailPanelWidth"
        tabindex="0"
        title="拖动调整创建栏宽度，双击恢复默认"
        @pointerdown="startDetailResize"
        @dblclick="resetDetailPanelWidth"
        @keydown="handleDetailResizeKeydown"
      >
        <GripVertical :size="15" />
        <span v-if="isDetailResizing">{{ detailPanelWidth }} px</span>
      </div>

      <div class="detail-panel-scroll create-panel-scroll">
        <header class="detail-header create-panel-header">
          <div class="detail-heading-copy">
            <p>创建工作组</p>
            <span>新的协作空间</span>
          </div>
          <button
            type="button"
            class="icon-button"
            aria-label="关闭创建工作组"
            :disabled="isGroupSubmitting"
            @click="closeGroupComposer"
          >
            <X :size="18" />
          </button>
        </header>

        <form class="create-detail-form group-create-form" @submit.prevent="handleCreateGroup">
          <div class="group-create-intro">
            <span><Building2 :size="22" /></span>
            <div>
              <strong>建立工作组</strong>
              <p>名称和描述会展示给工作组成员。</p>
            </div>
          </div>

          <label class="create-title-field group-name-field">
            <span>工作组名称</span>
            <input
              v-focus
              v-model="groupForm.name"
              type="text"
              maxlength="100"
              placeholder="例如：课程项目组"
              @input="groupFormError = ''"
            />
            <small>{{ groupForm.name.length }} / 100</small>
          </label>

          <label class="group-description-field">
            <span>工作组描述</span>
            <textarea
              v-model="groupForm.description"
              maxlength="500"
              rows="7"
              placeholder="记录工作组的目标、范围或协作约定"
              @input="groupFormError = ''"
            ></textarea>
            <small>{{ groupForm.description.length }} / 500</small>
          </label>

          <p v-if="groupFormError" class="notice error create-panel-error" role="alert">{{ groupFormError }}</p>

          <footer class="create-panel-actions">
            <button type="button" :disabled="isGroupSubmitting" @click="closeGroupComposer">取消</button>
            <button class="primary-button" type="submit" :disabled="isGroupSubmitting || !isGroupFormValid">
              <RefreshCw v-if="isGroupSubmitting" class="spin-icon" :size="16" />
              <FolderPlus v-else :size="17" />
              <span>{{ isGroupSubmitting ? '创建中...' : '创建工作组' }}</span>
            </button>
          </footer>
        </form>
      </div>
    </aside>

    <aside v-if="isComposerOpen" class="detail-panel create-panel">
      <div
        class="detail-resize-handle"
        role="separator"
        aria-label="调整新建任务栏宽度"
        aria-orientation="vertical"
        :aria-valuemin="detailPanelBounds.min"
        :aria-valuemax="detailPanelBounds.max"
        :aria-valuenow="detailPanelWidth"
        tabindex="0"
        title="拖动调整创建栏宽度，双击恢复默认"
        @pointerdown="startDetailResize"
        @dblclick="resetDetailPanelWidth"
        @keydown="handleDetailResizeKeydown"
      >
        <GripVertical :size="15" />
        <span v-if="isDetailResizing">{{ detailPanelWidth }} px</span>
      </div>

      <div class="detail-panel-scroll create-panel-scroll">
        <header class="detail-header create-panel-header">
          <div class="detail-heading-copy">
            <p>创建任务</p>
            <span>{{ currentView.label }}</span>
          </div>
          <button
            type="button"
            class="icon-button"
            aria-label="关闭新建任务"
            :disabled="isTaskSubmitting"
            @click="closeComposer"
          >
            <X :size="18" />
          </button>
        </header>

        <form class="create-detail-form" @submit.prevent="handleCreateTask">
          <label class="create-title-field">
            <span>任务标题</span>
            <input
              v-focus
              v-model="taskForm.title"
              type="text"
              maxlength="100"
              placeholder="准备做什么？"
              @input="composerError = ''"
            />
            <small>{{ taskForm.title.length }} / 100</small>
          </label>

          <section class="create-form-section create-properties-section">
            <div class="create-section-heading">
              <div>
                <span class="create-section-icon status-icon"><Circle :size="16" /></span>
                <strong>状态</strong>
              </div>
            </div>
            <div class="create-choice-grid status-choice-grid" aria-label="任务状态">
              <button
                v-for="option in statusOptions"
                :key="option.value"
                type="button"
                :class="[`status-${option.value}`, { active: taskForm.status === option.value }]"
                @click="taskForm.status = option.value"
              >
                <Circle v-if="option.value === 'TODO'" :size="15" />
                <RefreshCw v-else-if="option.value === 'IN_PROGRESS'" :size="15" />
                <Check v-else :size="15" />
                <span>{{ option.label }}</span>
              </button>
            </div>

            <div class="create-section-heading priority-create-heading">
              <div>
                <span class="create-section-icon priority-icon"><Flag :size="16" /></span>
                <strong>优先级</strong>
              </div>
            </div>
            <div class="create-choice-grid priority-choice-grid" aria-label="任务优先级">
              <button
                v-for="option in priorityOptions"
                :key="option.value"
                type="button"
                :class="[option.tone, { active: taskForm.priority === option.value }]"
                @click="taskForm.priority = option.value"
              >
                <Flag :size="14" />
                <span>{{ option.label }}</span>
              </button>
            </div>
          </section>

          <section class="create-form-section">
            <div class="create-section-heading">
              <div>
                <span class="create-section-icon description-icon"><AlignLeft :size="16" /></span>
                <strong>描述</strong>
              </div>
              <small>{{ taskForm.description.length }} / 100</small>
            </div>
            <textarea
              v-model="taskForm.description"
              maxlength="100"
              rows="4"
              placeholder="补充任务背景、目标或注意事项"
              @input="composerError = ''"
            ></textarea>
          </section>

          <section class="create-form-section create-due-section">
            <div class="create-section-heading">
              <div>
                <span class="create-section-icon due-icon"><CalendarDays :size="16" /></span>
                <span>
                  <strong>截止时间</strong>
                  <small>{{ createDueLabel }}</small>
                </span>
              </div>
              <button v-if="hasCreateDueValue" type="button" @click="clearCreateDue">
                清除
              </button>
            </div>

            <div class="create-due-presets">
              <button
                v-for="preset in duePresetOptions"
                :key="preset.value"
                type="button"
                :class="{ active: isCreateDuePresetActive(preset.value) }"
                @click="applyCreateDuePreset(preset.value)"
              >
                <strong>{{ preset.label }}</strong>
                <small>{{ preset.meta }}</small>
              </button>
            </div>

            <button
              class="due-custom-toggle"
              type="button"
              :class="{ open: isCreateCustomDueOpen }"
              :aria-expanded="isCreateCustomDueOpen"
              @click="toggleCreateCustomDue"
            >
              <span><Clock3 :size="15" /> 自定义日期与时间</span>
              <ChevronDown :size="15" />
            </button>

            <Transition name="property-reveal">
              <div v-if="isCreateCustomDueOpen" class="due-custom-fields">
                <div class="date-part-grid create-date-grid" aria-label="新任务截止日期">
                  <label>
                    <span>年</span>
                    <input
                      :value="createDateParts.year"
                      inputmode="numeric"
                      maxlength="4"
                      placeholder="2026"
                      @input="updateCreateDatePart('year', $event.target.value)"
                    />
                  </label>
                  <label>
                    <span>月</span>
                    <input
                      :value="createDateParts.month"
                      inputmode="numeric"
                      maxlength="2"
                      placeholder="07"
                      @input="updateCreateDatePart('month', $event.target.value)"
                    />
                  </label>
                  <label>
                    <span>日</span>
                    <input
                      :value="createDateParts.day"
                      inputmode="numeric"
                      maxlength="2"
                      placeholder="31"
                      @input="updateCreateDatePart('day', $event.target.value)"
                    />
                  </label>
                </div>

                <div class="time-part-grid create-time-grid" aria-label="新任务截止时间">
                  <label>
                    <span>时</span>
                    <input
                      :value="createTimeParts.hour"
                      inputmode="numeric"
                      maxlength="2"
                      placeholder="23"
                      @input="updateCreateTimePart('hour', $event.target.value)"
                    />
                  </label>
                  <i>:</i>
                  <label>
                    <span>分</span>
                    <input
                      :value="createTimeParts.minute"
                      inputmode="numeric"
                      maxlength="2"
                      placeholder="59"
                      @input="updateCreateTimePart('minute', $event.target.value)"
                    />
                  </label>
                </div>
              </div>
            </Transition>
          </section>

          <section class="create-form-section create-steps-section">
            <div class="create-section-heading">
              <div>
                <span class="create-section-icon steps-icon"><ListChecks :size="17" /></span>
                <span>
                  <strong>执行步骤</strong>
                  <small>{{ createStepDrafts.length ? `${createStepDrafts.length} 个步骤` : '尚未添加' }}</small>
                </span>
              </div>
            </div>

            <TransitionGroup v-if="createStepDrafts.length" name="create-step" tag="div" class="create-step-list">
              <div v-for="(step, index) in createStepDrafts" :key="step.id" class="create-step-row">
                <span>{{ index + 1 }}</span>
                <p>{{ step.title }}</p>
                <button type="button" :aria-label="`移除步骤 ${step.title}`" title="移除" @click="removeCreateStep(step.id)">
                  <X :size="14" />
                </button>
              </div>
            </TransitionGroup>

            <div class="create-step-composer">
              <Plus :size="16" />
              <input
                v-model="createStepDraft"
                type="text"
                maxlength="100"
                placeholder="添加一个执行步骤"
                @input="composerError = ''"
                @keydown.enter.prevent="addCreateStep"
              />
              <button
                type="button"
                :disabled="!createStepDraft.trim()"
                aria-label="添加执行步骤"
                @click="addCreateStep"
              >
                <Plus :size="15" />
              </button>
            </div>
          </section>

          <p v-if="composerError" class="notice error create-panel-error" role="alert">{{ composerError }}</p>

          <footer class="create-panel-actions">
            <button type="button" :disabled="isTaskSubmitting" @click="closeComposer">取消</button>
            <button class="primary-button" type="submit" :disabled="isTaskSubmitting || !isTaskValid">
              <RefreshCw v-if="isTaskSubmitting" class="spin-icon" :size="16" />
              <Plus v-else :size="17" />
              <span>{{ isTaskSubmitting ? '创建中...' : '创建任务' }}</span>
            </button>
          </footer>
        </form>
      </div>
    </aside>

    <aside v-if="selectedTask" class="detail-panel">
      <div
        class="detail-resize-handle"
        role="separator"
        aria-label="调整任务详情宽度"
        aria-orientation="vertical"
        :aria-valuemin="detailPanelBounds.min"
        :aria-valuemax="detailPanelBounds.max"
        :aria-valuenow="detailPanelWidth"
        tabindex="0"
        title="拖动调整详情宽度，双击恢复默认"
        @pointerdown="startDetailResize"
        @dblclick="resetDetailPanelWidth"
        @keydown="handleDetailResizeKeydown"
      >
        <GripVertical :size="15" />
        <span v-if="isDetailResizing">{{ detailPanelWidth }} px</span>
      </div>

      <div class="detail-panel-scroll">
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
              <div class="steps-heading-actions">
                <span v-if="taskSteps.length" class="step-count">{{ taskStepProgress }}%</span>
                <button
                  class="ai-step-trigger"
                  type="button"
                  title="AI 拆解任务"
                  :class="{ active: isAiStepDraftOpen }"
                  :aria-expanded="isAiStepDraftOpen"
                  @click="toggleAiStepDraftPanel"
                >
                  <Sparkles :size="14" />
                  <span>AI 拆解</span>
                </button>
              </div>
            </div>

            <Transition name="property-reveal">
              <section v-if="isAiStepDraftOpen" class="ai-step-draft-panel" aria-label="AI 步骤草稿">
                <header class="ai-step-draft-header">
                  <div>
                    <small>AI STEP DRAFTS</small>
                    <strong>生成执行步骤</strong>
                  </div>
                  <button type="button" aria-label="关闭 AI 步骤草稿" title="关闭" @click="isAiStepDraftOpen = false">
                    <X :size="15" />
                  </button>
                </header>

                <form class="ai-step-draft-form" @submit.prevent="handleGenerateAiStepDrafts">
                  <div class="ai-step-presets" aria-label="拆解要求快捷选项">
                    <button
                      v-for="preset in aiStepInstructionPresets"
                      :key="preset"
                      type="button"
                      :class="{ active: aiStepInstruction === preset }"
                      @click="applyAiStepInstructionPreset(preset)"
                    >
                      {{ preset }}
                    </button>
                  </div>

                  <label class="ai-step-instruction">
                    <textarea
                      v-model="aiStepInstruction"
                      maxlength="500"
                      rows="2"
                      placeholder="补充拆解要求（可选）"
                      @input="aiStepDraftError = ''; aiStepDraftMessage = ''"
                    ></textarea>
                    <span :class="{ over: aiStepInstruction.length > 500 }">{{ aiStepInstruction.length }} / 500</span>
                  </label>

                  <button
                    class="ai-step-generate"
                    type="submit"
                    :class="{ loading: isAiStepDraftLoading }"
                    :disabled="!canGenerateAiStepDraft"
                  >
                    <RefreshCw v-if="isAiStepDraftLoading" :size="14" />
                    <WandSparkles v-else :size="14" />
                    <span>{{ isAiStepDraftLoading ? '生成中...' : aiStepDrafts.length ? '重新生成' : '生成草稿' }}</span>
                  </button>
                </form>

                <div v-if="isAiStepDraftLoading" class="ai-step-draft-loading" role="status">
                  <span v-for="index in 3" :key="index"></span>
                </div>

                <div v-else-if="aiStepDrafts.length" class="ai-step-draft-list">
                  <div v-for="(draft, index) in aiStepDrafts" :key="draft.id" class="ai-step-draft-row">
                    <button
                      class="ai-step-draft-check"
                      type="button"
                      :class="{ selected: draft.selected }"
                      :aria-label="draft.selected ? `取消选择步骤 ${index + 1}` : `选择步骤 ${index + 1}`"
                      @click="toggleAiStepDraft(draft)"
                    >
                      <Check v-if="draft.selected" :size="12" />
                      <span v-else>{{ index + 1 }}</span>
                    </button>
                    <input
                      v-model="draft.title"
                      type="text"
                      maxlength="100"
                      :aria-label="`步骤草稿 ${index + 1}`"
                      @input="aiStepDraftError = ''; aiStepDraftMessage = ''"
                    />
                    <button type="button" aria-label="移除草稿" title="移除" @click="removeAiStepDraft(draft.id)">
                      <X :size="14" />
                    </button>
                  </div>
                </div>

                <p v-if="aiStepDraftError" class="ai-step-feedback error" role="alert">{{ aiStepDraftError }}</p>
                <p v-if="aiStepDraftMessage" class="ai-step-feedback success" role="status">{{ aiStepDraftMessage }}</p>

                <footer v-if="aiStepDrafts.length" class="ai-step-draft-footer">
                  <button type="button" class="ai-step-select-all" @click="toggleAllAiStepDrafts">
                    <Check :size="13" />
                    <span>{{ areAllAiStepDraftsSelected ? '取消全选' : '选择全部' }}</span>
                  </button>
                  <button
                    type="button"
                    class="ai-step-save"
                    :disabled="isAiStepDraftSaving || !selectedAiStepDrafts.length"
                    @click="handleSaveAiStepDrafts"
                  >
                    <Plus :size="14" />
                    <span>{{ isAiStepDraftSaving ? '添加中...' : `批量添加 ${selectedAiStepDrafts.length} 项` }}</span>
                  </button>
                </footer>
              </section>
            </Transition>

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
                  <button
                    v-for="preset in duePresetOptions"
                    :key="preset.value"
                    type="button"
                    :class="{ active: isEditDuePresetActive(preset.value) }"
                    @click="applyEditDuePreset(preset.value)"
                  >
                    <strong>{{ preset.label }}</strong>
                    <small>{{ preset.meta }}</small>
                  </button>
                </div>

                <button
                  class="due-custom-toggle"
                  type="button"
                  :class="{ open: isEditCustomDueOpen }"
                  :aria-expanded="isEditCustomDueOpen"
                  @click="toggleEditCustomDue"
                >
                  <span><Clock3 :size="15" /> 自定义日期与时间</span>
                  <ChevronDown :size="15" />
                </button>

                <Transition name="property-reveal">
                  <div v-if="isEditCustomDueOpen" class="due-custom-fields">
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
                          placeholder="23"
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
                          placeholder="59"
                          @input="updateEditTimePart('minute', $event.target.value)"
                        />
                      </label>
                    </div>
                  </div>
                </Transition>
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

          <button class="danger-icon-button" type="button" aria-label="删除任务" title="删除任务" @click="openDeleteConfirm">
            <Trash2 :size="17" />
          </button>
        </div>
        </form>
      </div>
    </aside>
  </main>
</template>
