<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import AiAdvisor from './components/ai/AiAdvisor.vue'
import AuthView from './components/auth/AuthView.vue'
import ConfirmationDialog from './components/common/ConfirmationDialog.vue'
import GroupComposer from './components/groups/GroupComposer.vue'
import GroupWorkspace from './components/groups/GroupWorkspace.vue'
import InvitationCenter from './components/groups/InvitationCenter.vue'
import AppSidebar from './components/layout/AppSidebar.vue'
import TaskWorkspace from './components/tasks/TaskWorkspace.vue'
import TaskComposer from './components/tasks/TaskComposer.vue'
import { useResizablePanel } from './composables/useResizablePanel'
import {
  AlignLeft,
  BatteryMedium,
  CalendarDays,
  Check,
  ChevronDown,
  Circle,
  Clock3,
  Flag,
  GripVertical,
  History,
  ListChecks,
  ListTodo,
  LogOut,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  Sparkles,
  Star,
  Trash2,
  UsersRound,
  WandSparkles,
  X
} from '@lucide/vue'
import {
  acceptGroupInvitation,
  createGroup,
  createGroupInvitation,
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
  listPendingGroupInvitations,
  listTasks,
  listGroupMembers,
  listGroups,
  leaveGroup,
  loginUser,
  rejectGroupInvitation,
  registerUser,
  updateTaskStep,
  updateTask,
  updateTaskStatus
} from './services/api'
import { parseAdvice } from './utils/content'
import {
  END_OF_DAY_TIME,
  formatDateKey,
  formatDateLabel,
  formatFullDateTime,
  formatShortDate,
  formatTaskDateTime,
  formatTimeLabel,
  getNextWeekEndDate,
  parseLocalDateTime,
  resolveDuePartValues,
  resolveDuePresetDate,
  splitDateParts,
  splitTimeParts,
  toLocalDateKey
} from './utils/dateTime'
import { groupRoleLabel } from './utils/groups'
import { priorityText, statusText } from './utils/tasks'

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
const groupLeaveCandidate = ref(null)
const isTaskDeleting = ref(false)
const isGroupLeaving = ref(false)
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
const isGroupInviteOpen = ref(false)
const isGroupInviteSubmitting = ref(false)
const invitationAccount = ref('')
const invitationError = ref('')
const invitationMessage = ref('')
const pendingInvitations = ref([])
const isInvitationCenterOpen = ref(false)
const isInvitationListLoading = ref(false)
const invitationListError = ref('')
const acceptingInvitationId = ref(null)
const rejectingInvitationId = ref(null)
const rejectConfirmationId = ref(null)
const invitationActionMessage = ref('')
const acceptedInvitationGroup = ref(null)
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
const detailPropertiesRef = ref(null)
const groupInvitePanelRef = ref(null)
const invitationCenterCloseRef = ref(null)
const aiMessageInputRef = ref(null)
const deleteCancelButtonRef = ref(null)
const TASK_ORDER_STORAGE_PREFIX = 'aiTodoTaskOrder'
const TASK_STEP_BATCH_SIZE = 10
const {
  width: detailPanelWidth,
  bounds: detailPanelBounds,
  isResizing: isDetailResizing,
  startResize: startDetailResize,
  resetWidth: resetDetailPanelWidth,
  handleResizeKeydown: handleDetailResizeKeydown
} = useResizablePanel()
const isTaskDragging = ref(false)
const draggingTaskId = ref(null)
let searchTimer
let aiCopyTimer
let taskStepStatsRequestId = 0
let createStepDraftId = 0
let groupDetailRequestId = 0
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
const canInviteGroupMember = computed(() => selectedGroup.value?.currentUserRole === 'OWNER')
const isInvitationValid = computed(() => {
  const accountLength = invitationAccount.value.trim().length
  return accountLength > 0 && accountLength <= 100
})
const pendingInvitationCount = computed(() => pendingInvitations.value.length)

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
const isGroupLeaveConfirmOpen = computed(() => Boolean(groupLeaveCandidate.value))
const isConfirmationDialogOpen = computed(() => (
  Boolean(deleteCandidate.value) || isLogoutConfirmOpen.value || isGroupLeaveConfirmOpen.value
))
const isConfirmationPending = computed(() => isTaskDeleting.value || isGroupLeaving.value)
const confirmationDialogContent = computed(() => {
  if (isLogoutConfirmOpen.value) {
    return {
      kind: 'logout',
      eyebrow: 'SIGN OUT',
      title: '退出当前账号？',
      description: '退出后将清除本机的登录状态，你的任务数据仍会保留在账户中。',
      warning: '重新登录后可继续管理任务',
      confirmLabel: '退出登录',
      busyLabel: '退出登录',
      icon: LogOut,
      warningIcon: LogOut
    }
  }

  if (isGroupLeaveConfirmOpen.value) {
    return {
      kind: 'leave-group',
      eyebrow: 'LEAVE WORKSPACE',
      title: '退出这个工作组？',
      description: `退出后，你将无法继续访问“${groupLeaveCandidate.value?.name || '该工作组'}”的成员信息。`,
      warning: '再次加入需要负责人重新邀请',
      confirmLabel: '确认退出',
      busyLabel: '正在退出',
      icon: LogOut,
      warningIcon: UsersRound
    }
  }

  return {
    kind: 'delete-task',
    eyebrow: 'DELETE TASK',
    title: '删除这个任务？',
    description: `“${deleteCandidate.value?.title || '该任务'}”将从任务列表中永久移除。`,
    warning: '此操作无法撤销',
    confirmLabel: '确认删除',
    busyLabel: '正在删除',
    icon: Trash2,
    warningIcon: History
  }
})
const isInvitationOperationPending = computed(() => Boolean(
  acceptingInvitationId.value || rejectingInvitationId.value
))
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

  const token = localStorage.getItem('aiTodoToken')

  if (!token) {
    isBooting.value = false
    return
  }

  try {
    user.value = await getCurrentUser()
    await Promise.all([refreshTasks(), refreshGroups(), refreshPendingInvitations()])
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
  if (event.key !== 'Escape') {
    return
  }

  if (isConfirmationDialogOpen.value) {
    closeConfirmationDialog()
    return
  }

  if (isInvitationCenterOpen.value && rejectConfirmationId.value && !isInvitationOperationPending.value) {
    rejectConfirmationId.value = null
    return
  }

  if (isInvitationCenterOpen.value && !isInvitationOperationPending.value) {
    closeInvitationCenter()
    return
  }

  if (isAiAdvisorOpen.value) {
    closeAiAdvisor()
    return
  }

  if (isGroupInviteOpen.value && !isGroupInviteSubmitting.value) {
    closeGroupInvite()
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
    isAiAdvisorOpen.value || isConfirmationDialogOpen.value || isInvitationCenterOpen.value
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

  if (isFilterOpen.value && filterMenuRef.value && !filterMenuRef.value.isInsideFilter(event.target)) {
    isFilterOpen.value = false
  }

  if (isReminderOpen.value && filterMenuRef.value && !filterMenuRef.value.isInsideReminder(event.target)) {
    isReminderOpen.value = false
  }

  if (expandedDetailSection.value && detailPropertiesRef.value && !path.includes(detailPropertiesRef.value)) {
    expandedDetailSection.value = null
    stepDeleteCandidateId.value = null
  }


  if (isGroupInviteOpen.value && groupInvitePanelRef.value && !groupInvitePanelRef.value.contains(event.target)) {
    closeGroupInvite()
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
      await Promise.all([refreshTasks(), refreshGroups(), refreshPendingInvitations()])
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

async function refreshPendingInvitations(showLoading = false) {
  if (showLoading) {
    isInvitationListLoading.value = true
  }

  invitationListError.value = ''

  try {
    const result = await listPendingGroupInvitations()
    pendingInvitations.value = Array.isArray(result) ? result : []
  } catch (error) {
    invitationListError.value = error.message || '工作组邀请加载失败，请稍后重试。'
  } finally {
    isInvitationListLoading.value = false
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
  resetGroupInvitation()
  isInvitationCenterOpen.value = false
  pendingInvitations.value = []
  invitationListError.value = ''
  acceptingInvitationId.value = null
  rejectingInvitationId.value = null
  rejectConfirmationId.value = null
  invitationActionMessage.value = ''
  acceptedInvitationGroup.value = null
  groupLeaveCandidate.value = null
  isGroupLeaving.value = false
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

  if (isGroupLeaveConfirmOpen.value) {
    return confirmLeaveGroup()
  }

  return confirmDeleteTask()
}

async function selectView(key) {
  groupDetailRequestId += 1
  isGroupDetailLoading.value = false
  selectedGroup.value = null
  groupMembers.value = []
  groupDetailError.value = ''
  resetGroupInvitation()
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
  if (isConfirmationPending.value) {
    return
  }

  deleteCandidate.value = null
  isLogoutConfirmOpen.value = false
  groupLeaveCandidate.value = null
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

async function openInvitationCenter() {
  acceptedInvitationGroup.value = null
  rejectConfirmationId.value = null
  invitationActionMessage.value = ''
  isInvitationCenterOpen.value = true
  isSidebarOpen.value = false
  isFilterOpen.value = false
  isReminderOpen.value = false
  syncBodyModalState()

  await nextTick()
  invitationCenterCloseRef.value?.focus()
  await refreshPendingInvitations(true)
}

function closeInvitationCenter() {
  if (isInvitationOperationPending.value) {
    return
  }

  isInvitationCenterOpen.value = false
  acceptedInvitationGroup.value = null
  rejectConfirmationId.value = null
  invitationActionMessage.value = ''
  syncBodyModalState()
}

async function handleAcceptInvitation(invitation) {
  if (!invitation?.id || isInvitationOperationPending.value) {
    return
  }

  invitationListError.value = ''
  invitationActionMessage.value = ''
  rejectConfirmationId.value = null
  acceptingInvitationId.value = invitation.id

  try {
    const acceptedGroup = normalizeGroup(await acceptGroupInvitation(invitation.id))

    pendingInvitations.value = pendingInvitations.value.filter(
      (item) => String(item.id) !== String(invitation.id)
    )
    groups.value = [
      acceptedGroup,
      ...groups.value.filter((group) => String(group.id) !== String(acceptedGroup.id))
    ]
    acceptedInvitationGroup.value = acceptedGroup
  } catch (error) {
    invitationListError.value = error.message || '接受邀请失败，请稍后重试。'
  } finally {
    acceptingInvitationId.value = null
  }
}

function requestInvitationRejection(invitationId) {
  if (!invitationId || isInvitationOperationPending.value) {
    return
  }

  rejectConfirmationId.value = invitationId
  invitationListError.value = ''
  invitationActionMessage.value = ''
}

function cancelInvitationRejection() {
  if (rejectingInvitationId.value) {
    return
  }

  rejectConfirmationId.value = null
}

async function handleRejectInvitation(invitation) {
  if (!invitation?.id || isInvitationOperationPending.value) {
    return
  }

  invitationListError.value = ''
  invitationActionMessage.value = ''
  rejectingInvitationId.value = invitation.id

  try {
    await rejectGroupInvitation(invitation.id)
    pendingInvitations.value = pendingInvitations.value.filter(
      (item) => String(item.id) !== String(invitation.id)
    )
    rejectConfirmationId.value = null
    invitationActionMessage.value = `已拒绝“${invitation.groupName || '该工作组'}”的邀请。`
  } catch (error) {
    invitationListError.value = error.message || '拒绝邀请失败，请稍后重试。'
  } finally {
    rejectingInvitationId.value = null
  }
}

async function openAcceptedInvitationGroup() {
  const group = acceptedInvitationGroup.value

  if (!group) {
    return
  }

  closeInvitationCenter()
  await selectGroup(group)
}

function resetGroupInvitation() {
  isGroupInviteOpen.value = false
  invitationAccount.value = ''
  invitationError.value = ''
  invitationMessage.value = ''
}

function toggleGroupInvite() {
  if (!canInviteGroupMember.value || isGroupInviteSubmitting.value) {
    return
  }

  if (isGroupInviteOpen.value) {
    closeGroupInvite()
    return
  }

  invitationAccount.value = ''
  invitationError.value = ''
  invitationMessage.value = ''
  isGroupInviteOpen.value = true
}

function closeGroupInvite() {
  if (isGroupInviteSubmitting.value) {
    return
  }

  resetGroupInvitation()
}

async function handleCreateInvitation() {
  invitationError.value = ''
  invitationMessage.value = ''

  if (!selectedGroup.value || !canInviteGroupMember.value || !isInvitationValid.value) {
    invitationError.value = '请输入不超过 100 个字符的用户名或邮箱。'
    return
  }

  isGroupInviteSubmitting.value = true

  try {
    const invitation = await createGroupInvitation(selectedGroup.value.id, {
      account: invitationAccount.value.trim()
    })

    invitationAccount.value = ''
    invitationMessage.value = `已向 ${invitation.inviteeName || '该用户'} 发送邀请，等待对方处理。`
  } catch (error) {
    invitationError.value = error.message || '邀请发送失败，请稍后重试。'
  } finally {
    isGroupInviteSubmitting.value = false
  }
}

async function openGroupLeaveConfirm() {
  if (!selectedGroup.value || selectedGroup.value.currentUserRole !== 'MEMBER') {
    return
  }

  closeGroupInvite()
  groupLeaveCandidate.value = {
    id: selectedGroup.value.id,
    name: selectedGroup.value.name
  }
  deleteDialogError.value = ''
  syncBodyModalState()

  await nextTick()
  deleteCancelButtonRef.value?.focus()
}

async function confirmLeaveGroup() {
  const group = groupLeaveCandidate.value

  if (!group || isGroupLeaving.value) {
    return
  }

  deleteDialogError.value = ''
  isGroupLeaving.value = true

  try {
    await leaveGroup(group.id)
  } catch (error) {
    deleteDialogError.value = error.message || '退出工作组失败，请稍后重试。'
    isGroupLeaving.value = false
    return
  }

  groups.value = groups.value.filter((item) => String(item.id) !== String(group.id))
  selectedGroup.value = null
  groupMembers.value = []
  groupLeaveCandidate.value = null
  isGroupLeaving.value = false
  syncBodyModalState()
  await selectView('all')
}

async function selectGroup(group) {
  const normalizedGroup = normalizeGroup(group)
  const requestId = ++groupDetailRequestId

  resetGroupInvitation()

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

</script>

<template>
  <main v-if="isBooting" class="loading-screen">
    <div class="loader"></div>
  </main>

  <AuthView
    v-else-if="!user"
    v-model:account="authForm.account"
    v-model:username="authForm.username"
    v-model:email="authForm.email"
    v-model:password="authForm.password"
    :is-login="isLogin"
    :is-submitting="isAuthSubmitting"
    :is-valid="isAuthValid"
    :error-message="errorMessage"
    :success-message="successMessage"
    @mode-change="switchAuthMode"
    @submit="handleAuthSubmit"
  />

  <main
    v-else
    class="todo-app"
    :class="{ 'has-detail': selectedTask || isComposerOpen || isGroupComposerOpen, 'is-detail-resizing': isDetailResizing }"
    :style="{ '--detail-panel-width': `${detailPanelWidth}px` }"
  >
    <AppSidebar
      :open="isSidebarOpen"
      :user="user"
      :views="views"
      :active-view="activeView"
      :selected-group="selectedGroup"
      :groups="groups"
      :is-group-list-loading="isGroupListLoading"
      :group-list-error="groupListError"
      :pending-invitation-count="pendingInvitationCount"
      @close="isSidebarOpen = false"
      @select-view="selectView"
      @select-group="selectGroup"
      @open-invitations="openInvitationCenter"
      @create-group="openGroupComposer"
      @retry-groups="refreshGroups"
      @logout="openLogoutConfirm"
    />

    <InvitationCenter
      ref="invitationCenterCloseRef"
      :open="isInvitationCenterOpen"
      :invitations="pendingInvitations"
      :pending-count="pendingInvitationCount"
      :is-loading="isInvitationListLoading"
      :error-message="invitationListError"
      :accepted-group="acceptedInvitationGroup"
      :action-message="invitationActionMessage"
      :operation-pending="isInvitationOperationPending"
      :accepting-id="acceptingInvitationId"
      :rejecting-id="rejectingInvitationId"
      :reject-confirmation-id="rejectConfirmationId"
      @close="closeInvitationCenter"
      @open-accepted="openAcceptedInvitationGroup"
      @retry="refreshPendingInvitations(true)"
      @cancel-rejection="cancelInvitationRejection"
      @request-rejection="requestInvitationRejection"
      @reject="handleRejectInvitation"
      @accept="handleAcceptInvitation"
    />

    <section class="task-board" :class="{ 'group-board': selectedGroup }">
      <GroupWorkspace
        v-if="selectedGroup"
        ref="groupInvitePanelRef"
        v-model:invitation-account="invitationAccount"
        :group="selectedGroup"
        :members="groupMembers"
        :role-label="selectedGroupRole"
        :pending-invitation-count="pendingInvitationCount"
        :detail-error="groupDetailError"
        :is-loading="isGroupDetailLoading"
        :can-invite="canInviteGroupMember"
        :is-invite-open="isGroupInviteOpen"
        :is-invite-submitting="isGroupInviteSubmitting"
        :is-invitation-valid="isInvitationValid"
        :invitation-error="invitationError"
        :invitation-message="invitationMessage"
        @open-sidebar="isSidebarOpen = true"
        @create-group="openGroupComposer"
        @leave="openGroupLeaveConfirm"
        @retry="selectGroup(selectedGroup)"
        @toggle-invite="toggleGroupInvite"
        @close-invite="closeGroupInvite"
        @submit-invite="handleCreateInvitation"
        @clear-invitation-feedback="invitationError = ''; invitationMessage = ''"
      />

      <TaskWorkspace
        v-else
        ref="filterMenuRef"
        v-model:query="query"
        v-model:filter-open="isFilterOpen"
        v-model:reminder-open="isReminderOpen"
        v-model:completed-group-open="isCompletedGroupOpen"
        :pending-invitation-count="pendingInvitationCount"
        :current-view="currentView"
        :reminders="reminders"
        :has-list-filters="hasListFilters"
        :has-server-query="hasServerQuery"
        :filters="listFilters"
        :status-options="statusOptions"
        :priority-options="priorityOptions"
        :stats="currentViewStats"
        :error-message="errorMessage"
        :is-loading="isTaskListLoading"
        :tasks="visibleTasks"
        :task-groups="visibleTaskGroups"
        :selected-task="selectedTask"
        :dragging-task-id="draggingTaskId"
        :empty-state="emptyState"
        :page="taskPage"
        :get-step-stats="getTaskStepStats"
        :get-step-label="taskStepListLabel"
        @open-sidebar="isSidebarOpen = true"
        @clear-search="clearSearch"
        @open-task="handleTaskItemClick"
        @clear-filters="clearListFilters"
        @set-status="setListStatus"
        @set-priority="setListPriority"
        @open-ai="openAiAdvisor"
        @create-task="openComposer"
        @retry="refreshTasks"
        @drag-start="handleTaskDragStart"
        @drag-end="handleTaskDragEnd"
        @toggle-task="toggleTaskDone"
        @move-task="moveTaskWithKeyboard"
        @empty-action="handleEmptyAction"
        @change-page="changePage"
        @change-page-size="changePageSize"
      />

      <AiAdvisor
        ref="aiMessageInputRef"
        v-model:message="aiMessage"
        :open="isAiAdvisorOpen"
        :prompt-options="aiPromptOptions"
        :unfinished-count="unfinishedTaskCount"
        :stats="taskStats"
        :is-submitting="isAiSubmitting"
        :is-valid="isAiMessageValid"
        :error-message="aiError"
        :advice="aiAdvice"
        :advice-blocks="aiAdviceBlocks"
        :copied="aiCopied"
        @close="closeAiAdvisor"
        @apply-prompt="applyAiPrompt"
        @submit="handleAiAdviceSubmit"
        @copy="copyAiAdvice"
      />

      <ConfirmationDialog
        ref="deleteCancelButtonRef"
        :open="isConfirmationDialogOpen"
        :content="confirmationDialogContent"
        :pending="isConfirmationPending"
        :error-message="deleteDialogError"
        @close="closeConfirmationDialog"
        @confirm="confirmDialogAction"
        @keydown="handleDeleteDialogKeydown"
      />
    </section>

    <GroupComposer
      v-model:name="groupForm.name"
      v-model:description="groupForm.description"
      :open="isGroupComposerOpen"
      :submitting="isGroupSubmitting"
      :valid="isGroupFormValid"
      :error-message="groupFormError"
      :panel-width="detailPanelWidth"
      :panel-bounds="detailPanelBounds"
      :resizing="isDetailResizing"
      @close="closeGroupComposer"
      @submit="handleCreateGroup"
      @clear-error="groupFormError = ''"
      @resize-start="startDetailResize"
      @resize-reset="resetDetailPanelWidth"
      @resize-keydown="handleDetailResizeKeydown"
    />

    <TaskComposer
      v-model:title="taskForm.title"
      v-model:description="taskForm.description"
      v-model:status="taskForm.status"
      v-model:priority="taskForm.priority"
      v-model:step-draft="createStepDraft"
      :open="isComposerOpen"
      :view-label="currentView.label"
      :submitting="isTaskSubmitting"
      :valid="isTaskValid"
      :error-message="composerError"
      :status-options="statusOptions"
      :priority-options="priorityOptions"
      :due-label="createDueLabel"
      :has-due-value="hasCreateDueValue"
      :due-presets="duePresetOptions"
      :is-due-preset-active="isCreateDuePresetActive"
      :custom-due-open="isCreateCustomDueOpen"
      :date-parts="createDateParts"
      :time-parts="createTimeParts"
      :step-drafts="createStepDrafts"
      :panel-width="detailPanelWidth"
      :panel-bounds="detailPanelBounds"
      :resizing="isDetailResizing"
      v-on:close="closeComposer"
      v-on:submit="handleCreateTask"
      v-on:clear-error="composerError = ''"
      v-on:clear-due="clearCreateDue"
      v-on:apply-due-preset="applyCreateDuePreset"
      v-on:toggle-custom-due="toggleCreateCustomDue"
      v-on:update-date-part="updateCreateDatePart"
      v-on:update-time-part="updateCreateTimePart"
      v-on:add-step="addCreateStep"
      v-on:remove-step="removeCreateStep"
      v-on:resize-start="startDetailResize"
      v-on:resize-reset="resetDetailPanelWidth"
      v-on:resize-keydown="handleDetailResizeKeydown"
    />

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
