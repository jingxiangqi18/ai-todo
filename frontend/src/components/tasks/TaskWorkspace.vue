<script setup>
import { ref } from 'vue'
import VueDraggable from 'vuedraggable'
import {
  Bell,
  CalendarDays,
  Check,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Circle,
  Flag,
  GripVertical,
  ListChecks,
  ListTodo,
  Menu,
  Plus,
  RefreshCw,
  Search,
  SlidersHorizontal,
  WandSparkles,
  X
} from '@lucide/vue'
import { formatDueAt } from '../../utils/dateTime'
import { isTaskOverdue, priorityText, statusText } from '../../utils/tasks'

defineProps({
  pendingInvitationCount: {
    type: Number,
    default: 0
  },
  currentView: {
    type: Object,
    required: true
  },
  reminders: {
    type: Array,
    default: () => []
  },
  hasListFilters: Boolean,
  hasServerQuery: Boolean,
  filters: {
    type: Object,
    required: true
  },
  statusOptions: {
    type: Array,
    default: () => []
  },
  priorityOptions: {
    type: Array,
    default: () => []
  },
  stats: {
    type: Object,
    required: true
  },
  errorMessage: {
    type: String,
    default: ''
  },
  isLoading: Boolean,
  tasks: {
    type: Array,
    default: () => []
  },
  taskGroups: {
    type: Array,
    default: () => []
  },
  selectedTask: {
    type: Object,
    default: null
  },
  draggingTaskId: {
    type: [String, Number],
    default: null
  },
  emptyState: {
    type: Object,
    required: true
  },
  page: {
    type: Object,
    required: true
  },
  getStepStats: {
    type: Function,
    required: true
  },
  getStepLabel: {
    type: Function,
    required: true
  }
})

const emit = defineEmits([
  'open-sidebar',
  'clear-search',
  'open-task',
  'clear-filters',
  'set-status',
  'set-priority',
  'open-ai',
  'create-task',
  'retry',
  'drag-start',
  'drag-end',
  'toggle-task',
  'move-task',
  'empty-action',
  'change-page',
  'change-page-size'
])

const query = defineModel('query', { type: String, default: '' })
const filterOpen = defineModel('filterOpen', { type: Boolean, default: false })
const reminderOpen = defineModel('reminderOpen', { type: Boolean, default: false })
const completedGroupOpen = defineModel('completedGroupOpen', { type: Boolean, default: true })
const filterMenuRef = ref(null)
const reminderMenuRef = ref(null)

defineExpose({
  isInsideFilter: (target) => Boolean(filterMenuRef.value?.contains(target)),
  isInsideReminder: (target) => Boolean(reminderMenuRef.value?.contains(target))
})
</script>

<template>
  <header class="board-header">
    <div class="board-title">
      <button class="mobile-menu-button" type="button" aria-label="打开导航" @click="emit('open-sidebar')">
        <Menu :size="20" />
        <b v-if="pendingInvitationCount">{{ pendingInvitationCount > 99 ? '99+' : pendingInvitationCount }}</b>
      </button>
      <p class="date-line">{{ new Date().toLocaleDateString('zh-CN', { weekday: 'long', month: 'long', day: 'numeric' }) }}</p>
      <h1>{{ currentView.label }}</h1>
    </div>

    <div class="board-tools">
      <div class="search-box search-box-compact">
        <Search :size="17" />
        <input v-model="query" type="search" placeholder="搜索标题或描述" aria-label="搜索任务" />
        <button v-if="query" type="button" aria-label="清除搜索" title="清除搜索" @click="emit('clear-search')">
          <X :size="15" />
        </button>
      </div>

      <div ref="reminderMenuRef" class="reminder-menu">
        <button
          class="tool-button reminder-trigger"
          type="button"
          aria-label="查看即将到期任务"
          @click="reminderOpen = !reminderOpen; filterOpen = false"
        >
          <Bell :size="17" />
          <span>未来 60 分钟</span>
          <b v-if="reminders.length">{{ reminders.length }}</b>
        </button>

        <div v-if="reminderOpen" class="reminder-popover">
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
            @click="emit('open-task', reminder)"
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
          :class="{ active: filterOpen }"
          @click="filterOpen = !filterOpen; reminderOpen = false"
        >
          <SlidersHorizontal :size="17" />
          <span>筛选</span>
          <b v-if="hasListFilters">{{ Number(Boolean(filters.status)) + Number(Boolean(filters.priority)) }}</b>
        </button>

        <div v-if="filterOpen" class="filter-popover">
          <div class="filter-popover-heading">
            <strong>筛选任务</strong>
            <button v-if="hasServerQuery" type="button" @click="emit('clear-filters')">清除</button>
          </div>

          <div class="filter-section">
            <span>状态</span>
            <div class="filter-group">
              <button type="button" :class="{ active: !filters.status }" @click="emit('set-status', '')">全部</button>
              <button
                v-for="option in statusOptions"
                :key="option.value"
                type="button"
                :class="{ active: filters.status === option.value }"
                @click="emit('set-status', option.value)"
              >
                {{ option.label }}
              </button>
            </div>
          </div>

          <div class="filter-section priority-filter-section">
            <span>优先级</span>
            <div class="filter-group">
              <button type="button" :class="{ active: !filters.priority }" @click="emit('set-priority', '')">全部</button>
              <button
                v-for="option in priorityOptions"
                :key="option.value"
                type="button"
                :class="{ active: filters.priority === option.value }"
                @click="emit('set-priority', option.value)"
              >
                {{ option.label }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="board-primary-actions">
      <button class="tool-button ai-trigger" type="button" @click="emit('open-ai')">
        <WandSparkles :size="17" />
        <span>AI 规划</span>
      </button>

      <button class="primary-button create-trigger" type="button" @click="emit('create-task')">
        <Plus :size="17" />
        <span>新建任务</span>
      </button>
    </div>
  </header>

  <div class="board-summary" aria-label="任务概览">
    <span><b>{{ stats.total }}</b> 当前视图</span>
    <span><b>{{ stats.todo }}</b> 待办</span>
    <span><b>{{ stats.dueToday }}</b> 今天截止</span>
    <span v-if="stats.overdue" class="summary-overdue"><b>{{ stats.overdue }}</b> 已逾期</span>
  </div>

  <div v-if="errorMessage" class="notice error list-error" role="alert">
    <span>{{ errorMessage }}</span>
    <button type="button" @click="emit('retry')">重试</button>
  </div>

  <div v-if="isLoading" class="task-list">
    <template v-for="index in 5" :key="index">
      <div class="task-skeleton" aria-hidden="true">
        <span></span>
        <div><i></i><i></i></div>
        <em></em>
      </div>
    </template>
  </div>

  <div v-else-if="tasks.length" class="task-groups">
    <section
      v-for="group in taskGroups"
      :key="group.key"
      class="task-group"
      :class="{ 'completed-task-group': group.completed }"
    >
      <button
        v-if="group.completed"
        class="completed-group-toggle"
        type="button"
        :aria-expanded="completedGroupOpen"
        @click="completedGroupOpen = !completedGroupOpen"
      >
        <ChevronRight class="completed-group-chevron" :class="{ open: completedGroupOpen }" :size="17" />
        <span>已完成</span>
        <b>{{ group.tasks.length }}</b>
      </button>

      <Transition name="completed-list">
        <VueDraggable
          v-show="!group.completed || completedGroupOpen"
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
          @start="emit('drag-start', $event)"
          @end="emit('drag-end', group.tasks)"
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
              @click="emit('open-task', task)"
              @keydown.enter="emit('open-task', task)"
            >
              <div class="task-leading-actions" @click.stop>
                <button
                  class="task-check"
                  type="button"
                  :class="{ done: task.status === 'DONE' }"
                  :aria-label="task.status === 'DONE' ? '恢复任务' : '完成任务'"
                  @click="emit('toggle-task', task)"
                >
                  <Check v-if="task.status === 'DONE'" :size="14" />
                </button>
                <button
                  class="task-drag-handle"
                  type="button"
                  :aria-label="`调整任务“${task.title}”的顺序`"
                  title="拖动排序；也可使用上下方向键"
                  @keydown.up.stop.prevent="emit('move-task', task, group.tasks, -1)"
                  @keydown.down.stop.prevent="emit('move-task', task, group.tasks, 1)"
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
                  loading: getStepStats(task.id).loading,
                  unavailable: getStepStats(task.id).error,
                  complete: getStepStats(task.id).progress === 100
                }"
              >
                <div class="task-step-copy">
                  <ListChecks :size="13" />
                  <span>{{ getStepLabel(task.id) }}</span>
                  <b v-if="!getStepStats(task.id).loading && !getStepStats(task.id).error">
                    {{ getStepStats(task.id).progress }}%
                  </b>
                </div>
                <div
                  class="task-step-track"
                  role="progressbar"
                  :aria-label="`${task.title}的步骤完成进度`"
                  :aria-valuenow="getStepStats(task.id).progress"
                  aria-valuemin="0"
                  aria-valuemax="100"
                >
                  <span :style="{ width: `${getStepStats(task.id).progress}%` }"></span>
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
      <button type="button" @click="emit('empty-action')">{{ emptyState.action }}</button>
    </section>
  </div>

  <div v-if="page.pages > 1 || page.total > 10" class="pagination-bar">
    <div class="page-info">
      <span>共 {{ page.total }} 条</span>
      <strong>{{ page.page }} / {{ page.pages || 1 }}</strong>
    </div>

    <div class="page-actions">
      <button class="page-arrow" type="button" :disabled="page.page <= 1" @click="emit('change-page', page.page - 1)">
        <ChevronLeft :size="17" />
        <span>上一页</span>
      </button>
      <button class="page-arrow" type="button" :disabled="page.page >= page.pages" @click="emit('change-page', page.page + 1)">
        <span>下一页</span>
        <ChevronRight :size="17" />
      </button>
    </div>

    <label class="page-size-select">
      <span class="sr-only">每页显示数量</span>
      <select :value="page.size" aria-label="每页显示数量" @change="emit('change-page-size', Number($event.target.value))">
        <option v-for="size in [10, 20, 50]" :key="size" :value="size">{{ size }} 条 / 页</option>
      </select>
      <ChevronDown :size="15" />
    </label>
  </div>
</template>
