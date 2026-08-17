<script setup>
import {
  AlignLeft,
  CalendarDays,
  Check,
  ChevronDown,
  Circle,
  Clock3,
  Flag,
  GripVertical,
  History,
  LockKeyhole,
  RefreshCw,
  Save,
  Trash2,
  UserRoundCheck,
  UserRoundPlus,
  UserRoundX,
  X
} from '@lucide/vue'
import { computed, reactive, ref, watch } from 'vue'
import {
  END_OF_DAY_TIME,
  formatDueAt,
  formatFullDateTime,
  resolveDuePartValues,
  resolveDuePresetDate,
  splitDateParts,
  splitTimeParts,
  toLocalDateKey
} from '../../utils/dateTime'
import { groupInitial, groupRoleLabel } from '../../utils/groups'
import { priorityText, statusText } from '../../utils/tasks'

const props = defineProps({
  task: { type: Object, required: true },
  groupName: { type: String, default: '当前工作组' },
  members: { type: Array, default: () => [] },
  canManage: Boolean,
  canUpdateStatus: Boolean,
  loading: Boolean,
  saving: Boolean,
  statusSaving: Boolean,
  assigneeSaving: Boolean,
  errorMessage: { type: String, default: '' },
  statusOptions: { type: Array, default: () => [] },
  priorityOptions: { type: Array, default: () => [] },
  duePresets: { type: Array, default: () => [] },
  panelWidth: { type: Number, required: true },
  panelBounds: { type: Object, required: true },
  resizing: Boolean
})

const emit = defineEmits([
  'close',
  'retry',
  'save',
  'status-change',
  'assignee-change',
  'delete',
  'clear-error',
  'resize-start',
  'resize-reset',
  'resize-keydown'
])

const expandedSection = ref(null)
const localError = ref('')
const draft = reactive({
  title: '',
  description: '',
  priority: 'MEDIUM'
})
const dueParts = reactive({
  year: '',
  month: '',
  day: '',
  hour: '',
  minute: ''
})

const dueResolution = computed(() => resolveDuePartValues(dueParts))
const dueLabel = computed(() => {
  if (dueResolution.value.error) {
    return '日期或时间填写中'
  }

  if (!dueResolution.value.date) {
    return '未设置截止时间'
  }

  return formatDueAt(`${dueResolution.value.date}T${dueResolution.value.time}`)
})
const isFormValid = computed(() => {
  const titleLength = draft.title.trim().length
  return titleLength > 0 && titleLength <= 100 && draft.description.length <= 1000
})
const isBusy = computed(() => props.loading || props.saving || props.statusSaving || props.assigneeSaving)

function hydrateDraft() {
  draft.title = props.task.title || ''
  draft.description = props.task.description || ''
  draft.priority = props.task.priority || 'MEDIUM'
  resetDueToTask()
  expandedSection.value = null
  localError.value = ''
}

function resetDueToTask() {
  const value = props.task.dueAt ? String(props.task.dueAt) : ''
  const date = value.slice(0, 10)
  const time = value.slice(11, 16)

  Object.assign(dueParts, {
    ...splitDateParts(date),
    ...splitTimeParts(time)
  })
  localError.value = ''
}

function toggleSection(section, editable = true) {
  if (!editable) {
    return
  }

  expandedSection.value = expandedSection.value === section ? null : section
}

function applyDuePreset(preset) {
  const date = toLocalDateKey(resolveDuePresetDate(preset))
  Object.assign(dueParts, {
    ...splitDateParts(date),
    ...splitTimeParts(END_OF_DAY_TIME)
  })
  localError.value = ''
  emit('clear-error')
}

function isDuePresetActive(preset) {
  return !dueResolution.value.error &&
    dueResolution.value.date === toLocalDateKey(resolveDuePresetDate(preset)) &&
    dueResolution.value.time === END_OF_DAY_TIME
}

function updateDuePart(part, value) {
  const maxLength = part === 'year' ? 4 : 2
  dueParts[part] = String(value).replace(/\D/g, '').slice(0, maxLength)
  localError.value = ''
  emit('clear-error')
}

function selectStatus(status) {
  if (!props.canUpdateStatus || props.statusSaving || props.task.status === status) {
    return
  }

  emit('clear-error')
  emit('status-change', status)
}

function selectAssignee(member) {
  if (!props.canManage || props.assigneeSaving) {
    return
  }

  emit('clear-error')
  emit('assignee-change', member?.userId ?? null)
}

function submitChanges() {
  localError.value = ''
  emit('clear-error')

  if (!props.canManage || props.saving) {
    return
  }

  if (!isFormValid.value) {
    localError.value = '标题应为 1 到 100 个字符，描述不能超过 1000 个字符。'
    return
  }

  if (dueResolution.value.error) {
    localError.value = dueResolution.value.error
    expandedSection.value = 'due'
    return
  }

  const payload = {
    title: draft.title.trim(),
    description: draft.description,
    priority: draft.priority
  }

  if (dueResolution.value.date) {
    payload.dueAt = `${dueResolution.value.date}T${dueResolution.value.time}`
  }

  emit('save', payload)
}

watch(() => props.task.id, hydrateDraft, { immediate: true })
</script>

<template>
  <aside class="detail-panel group-task-detail-panel" :aria-busy="isBusy">
    <div
      class="detail-resize-handle"
      role="separator"
      aria-label="调整团队任务详情宽度"
      aria-orientation="vertical"
      :aria-valuemin="panelBounds.min"
      :aria-valuemax="panelBounds.max"
      :aria-valuenow="panelWidth"
      tabindex="0"
      title="拖动调整详情宽度，双击恢复默认"
      @pointerdown="emit('resize-start', $event)"
      @dblclick="emit('resize-reset')"
      @keydown="emit('resize-keydown', $event)"
    >
      <GripVertical :size="15" />
      <span v-if="resizing">{{ panelWidth }} px</span>
    </div>

    <div class="detail-panel-scroll group-task-detail-scroll">
      <header class="detail-header group-task-detail-header">
        <div class="detail-heading-copy">
          <p>团队任务详情</p>
          <span>{{ groupName }}</span>
        </div>
        <button type="button" class="icon-button" aria-label="关闭团队任务详情" :disabled="isBusy" @click="emit('close')">
          <X :size="18" />
        </button>
      </header>

      <div v-if="loading" class="group-task-detail-loading"><span></span></div>

      <div v-if="errorMessage" class="notice error group-task-detail-error" role="alert">
        <span>{{ errorMessage }}</span>
        <button type="button" @click="emit('retry')">重试</button>
      </div>

      <form class="group-task-detail-content group-task-edit-form" @submit.prevent="submitChanges">
        <header class="group-task-detail-hero group-task-edit-hero">
          <div class="group-task-hero-kicker">
            <span>TEAM TASK</span>
            <span v-if="!canManage" class="group-task-readonly"><LockKeyhole :size="11" /> {{ canUpdateStatus ? '仅可更新状态' : '只读' }}</span>
          </div>
          <input
            v-if="canManage"
            v-model="draft.title"
            type="text"
            maxlength="100"
            aria-label="团队任务标题"
            @input="localError = ''; emit('clear-error')"
          />
          <h2 v-else>{{ task.title }}</h2>
          <div>
            <span class="group-task-detail-status" :class="`status-${task.status}`">
              <Check v-if="task.status === 'DONE'" :size="13" />
              <RefreshCw v-else-if="task.status === 'IN_PROGRESS'" :size="13" />
              <Circle v-else :size="13" />
              {{ statusText(task.status) }}
            </span>
            <span class="group-task-detail-priority" :class="`priority-${draft.priority}`">
              <Flag :size="13" />
              {{ priorityText(draft.priority) }}优先级
            </span>
          </div>
        </header>

        <div class="property-stack group-task-property-stack">
          <section class="property-item status-item" :class="{ expanded: expandedSection === 'status', readonly: !canUpdateStatus }">
            <button class="property-trigger" type="button" @click="toggleSection('status', canUpdateStatus)">
              <span class="property-icon status-icon" :class="`status-${task.status}`">
                <Circle v-if="task.status === 'TODO'" :size="16" />
                <RefreshCw v-else-if="task.status === 'IN_PROGRESS'" :size="16" />
                <Check v-else :size="16" />
              </span>
              <span class="property-copy"><small>状态</small><strong>{{ statusText(task.status) }}</strong></span>
              <RefreshCw v-if="statusSaving" class="spin-icon" :size="15" />
              <LockKeyhole v-else-if="!canUpdateStatus" :size="14" />
              <ChevronDown v-else class="property-chevron" :size="17" />
            </button>
            <Transition name="property-reveal">
              <div v-if="expandedSection === 'status'" class="property-editor status-option-grid group-task-option-grid">
                <button
                  v-for="option in statusOptions"
                  :key="option.value"
                  type="button"
                  :class="{ active: task.status === option.value }"
                  :disabled="statusSaving"
                  @click="selectStatus(option.value)"
                >
                  <Circle v-if="option.value === 'TODO'" :size="15" />
                  <RefreshCw v-else-if="option.value === 'IN_PROGRESS'" :size="15" />
                  <Check v-else :size="15" />
                  <span>{{ option.label }}</span>
                </button>
              </div>
            </Transition>
          </section>

          <section class="property-item group-task-assignee-item" :class="{ expanded: expandedSection === 'assignee', readonly: !canManage }">
            <button class="property-trigger" type="button" @click="toggleSection('assignee', canManage)">
              <span class="property-icon group-task-assignee-icon"><UserRoundCheck :size="16" /></span>
              <span class="property-copy"><small>负责人</small><strong>{{ task.assigneeName || '暂未分配' }}</strong></span>
              <RefreshCw v-if="assigneeSaving" class="spin-icon" :size="15" />
              <LockKeyhole v-else-if="!canManage" :size="14" />
              <ChevronDown v-else class="property-chevron" :size="17" />
            </button>
            <Transition name="property-reveal">
              <div v-if="expandedSection === 'assignee'" class="property-editor group-task-assignee-editor">
                <button
                  class="group-task-assignee-choice"
                  type="button"
                  :class="{ active: task.assigneeId == null }"
                  :disabled="assigneeSaving"
                  @click="selectAssignee(null)"
                >
                  <span class="group-task-detail-avatar empty"><UserRoundX :size="15" /></span>
                  <span><strong>暂不分配</strong><small>移除当前负责人</small></span>
                  <Check v-if="task.assigneeId == null" :size="14" />
                </button>
                <button
                  v-for="member in members"
                  :key="member.userId"
                  class="group-task-assignee-choice"
                  type="button"
                  :class="{ active: String(task.assigneeId) === String(member.userId) }"
                  :disabled="assigneeSaving"
                  @click="selectAssignee(member)"
                >
                  <span class="group-task-detail-avatar">{{ groupInitial(member.username) }}</span>
                  <span><strong>{{ member.username }}</strong><small>{{ groupRoleLabel(member.role) }}</small></span>
                  <Check v-if="String(task.assigneeId) === String(member.userId)" :size="14" />
                </button>
              </div>
            </Transition>
          </section>

          <section class="property-item description-item" :class="{ expanded: expandedSection === 'description' }">
            <button class="property-trigger" type="button" @click="toggleSection('description')">
              <span class="property-icon description-icon"><AlignLeft :size="16" /></span>
              <span class="property-copy"><small>描述</small><strong>{{ draft.description || '暂无任务描述' }}</strong></span>
              <ChevronDown class="property-chevron" :size="17" />
            </button>
            <Transition name="property-reveal">
              <div v-if="expandedSection === 'description'" class="property-editor description-editor">
                <textarea
                  v-if="canManage"
                  v-model="draft.description"
                  maxlength="1000"
                  rows="5"
                  placeholder="补充任务说明"
                  @input="localError = ''; emit('clear-error')"
                ></textarea>
                <p v-else class="group-task-readonly-copy">{{ task.description || '暂无任务描述。' }}</p>
                <small v-if="canManage">{{ draft.description.length }} / 1000</small>
              </div>
            </Transition>
          </section>

          <section class="property-item priority-item" :class="{ expanded: expandedSection === 'priority', readonly: !canManage }">
            <button class="property-trigger" type="button" @click="toggleSection('priority', canManage)">
              <span class="property-icon" :class="`priority-${draft.priority}`"><Flag :size="16" /></span>
              <span class="property-copy"><small>优先级</small><strong>{{ priorityText(draft.priority) }}优先级</strong></span>
              <LockKeyhole v-if="!canManage" :size="14" />
              <ChevronDown v-else class="property-chevron" :size="17" />
            </button>
            <Transition name="property-reveal">
              <div v-if="expandedSection === 'priority'" class="property-editor priority-option-grid group-task-option-grid">
                <button
                  v-for="option in priorityOptions"
                  :key="option.value"
                  type="button"
                  :class="[option.tone, { active: draft.priority === option.value }]"
                  @click="draft.priority = option.value; emit('clear-error')"
                >
                  <Flag :size="15" />
                  <span>{{ option.label }}优先级</span>
                </button>
              </div>
            </Transition>
          </section>

          <section class="property-item due-item" :class="{ expanded: expandedSection === 'due', readonly: !canManage }">
            <button class="property-trigger" type="button" @click="toggleSection('due', canManage)">
              <span class="property-icon due-icon"><CalendarDays :size="16" /></span>
              <span class="property-copy"><small>截止时间</small><strong>{{ canManage ? dueLabel : formatDueAt(task.dueAt) }}</strong></span>
              <LockKeyhole v-if="!canManage" :size="14" />
              <ChevronDown v-else class="property-chevron" :size="17" />
            </button>
            <Transition name="property-reveal">
              <div v-if="expandedSection === 'due'" class="property-editor compact-due-editor group-task-due-editor">
                <div class="due-editor-actions">
                  <button
                    v-for="preset in duePresets"
                    :key="preset.value"
                    type="button"
                    :class="{ active: isDuePresetActive(preset.value) }"
                    @click="applyDuePreset(preset.value)"
                  >
                    <strong>{{ preset.label }}</strong>
                    <small>{{ preset.meta }}</small>
                  </button>
                </div>
                <div class="due-custom-fields group-task-inline-due-fields">
                  <div class="date-part-grid" aria-label="团队任务截止日期">
                    <label v-for="part in ['year', 'month', 'day']" :key="part">
                      <span>{{ { year: '年', month: '月', day: '日' }[part] }}</span>
                      <input
                        :value="dueParts[part]"
                        inputmode="numeric"
                        :maxlength="part === 'year' ? 4 : 2"
                        :placeholder="{ year: '2026', month: '08', day: '31' }[part]"
                        @input="updateDuePart(part, $event.target.value)"
                      />
                    </label>
                  </div>
                  <div class="time-part-grid" aria-label="团队任务截止时间">
                    <label><span>时</span><input :value="dueParts.hour" inputmode="numeric" maxlength="2" placeholder="23" @input="updateDuePart('hour', $event.target.value)" /></label>
                    <i>:</i>
                    <label><span>分</span><input :value="dueParts.minute" inputmode="numeric" maxlength="2" placeholder="59" @input="updateDuePart('minute', $event.target.value)" /></label>
                  </div>
                </div>
                <button class="group-task-reset-due" type="button" @click="resetDueToTask"><History :size="13" /> 恢复原时间</button>
              </div>
            </Transition>
          </section>

          <section class="property-item activity-item" :class="{ expanded: expandedSection === 'activity' }">
            <button class="property-trigger" type="button" @click="toggleSection('activity')">
              <span class="property-icon activity-icon"><Clock3 :size="16" /></span>
              <span class="property-copy"><small>活动信息</small><strong>创建与最近更新</strong></span>
              <ChevronDown class="property-chevron" :size="17" />
            </button>
            <Transition name="property-reveal">
              <div v-if="expandedSection === 'activity'" class="property-editor group-task-activity-editor">
                <div><UserRoundPlus :size="14" /><span>创建者</span><strong>{{ task.creatorName }}</strong></div>
                <div><CalendarDays :size="14" /><span>创建</span><strong>{{ formatFullDateTime(task.createdAt) }}</strong></div>
                <div><RefreshCw :size="14" /><span>更新</span><strong>{{ formatFullDateTime(task.updatedAt) }}</strong></div>
              </div>
            </Transition>
          </section>
        </div>

        <p v-if="localError" class="notice error group-task-local-error" role="alert">{{ localError }}</p>

        <div v-if="canManage" class="detail-actions group-task-detail-actions">
          <button class="primary-button detail-save" type="submit" :disabled="saving || loading || !isFormValid">
            <RefreshCw v-if="saving" class="spin-icon" :size="16" />
            <Save v-else :size="17" />
            <span>{{ saving ? '保存中...' : '保存修改' }}</span>
          </button>
          <button class="danger-icon-button" type="button" aria-label="删除团队任务" title="删除团队任务" :disabled="isBusy" @click="emit('delete')">
            <Trash2 :size="17" />
          </button>
        </div>
      </form>
    </div>
  </aside>
</template>
