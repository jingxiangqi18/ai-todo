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
  ListChecks,
  Plus,
  RefreshCw,
  X
} from '@lucide/vue'

defineProps({
  open: Boolean,
  viewLabel: {
    type: String,
    required: true
  },
  submitting: Boolean,
  valid: Boolean,
  errorMessage: {
    type: String,
    default: ''
  },
  statusOptions: {
    type: Array,
    default: () => []
  },
  priorityOptions: {
    type: Array,
    default: () => []
  },
  dueLabel: {
    type: String,
    default: '未设置截止时间'
  },
  hasDueValue: Boolean,
  duePresets: {
    type: Array,
    default: () => []
  },
  isDuePresetActive: {
    type: Function,
    required: true
  },
  customDueOpen: Boolean,
  dateParts: {
    type: Object,
    required: true
  },
  timeParts: {
    type: Object,
    required: true
  },
  stepDrafts: {
    type: Array,
    default: () => []
  },
  panelWidth: {
    type: Number,
    required: true
  },
  panelBounds: {
    type: Object,
    required: true
  },
  resizing: Boolean
})

const emit = defineEmits([
  'close',
  'submit',
  'clear-error',
  'status-change',
  'priority-change',
  'clear-due',
  'apply-due-preset',
  'toggle-custom-due',
  'update-date-part',
  'update-time-part',
  'add-step',
  'remove-step',
  'resize-start',
  'resize-reset',
  'resize-keydown'
])

const title = defineModel('title', { type: String, default: '' })
const description = defineModel('description', { type: String, default: '' })
const status = defineModel('status', { type: String, default: 'TODO' })
const priority = defineModel('priority', { type: String, default: 'MEDIUM' })
const stepDraft = defineModel('stepDraft', { type: String, default: '' })
const vFocus = {
  mounted(element) {
    element.focus()
    element.select()
  }
}
</script>

<template>
  <aside v-if="open" class="detail-panel create-panel">
    <div
      class="detail-resize-handle"
      role="separator"
      aria-label="调整新建任务栏宽度"
      aria-orientation="vertical"
      :aria-valuemin="panelBounds.min"
      :aria-valuemax="panelBounds.max"
      :aria-valuenow="panelWidth"
      tabindex="0"
      title="拖动调整创建栏宽度，双击恢复默认"
      @pointerdown="emit('resize-start', $event)"
      @dblclick="emit('resize-reset')"
      @keydown="emit('resize-keydown', $event)"
    >
      <GripVertical :size="15" />
      <span v-if="resizing">{{ panelWidth }} px</span>
    </div>

    <div class="detail-panel-scroll create-panel-scroll">
      <header class="detail-header create-panel-header">
        <div class="detail-heading-copy">
          <p>创建任务</p>
          <span>{{ viewLabel }}</span>
        </div>
        <button type="button" class="icon-button" aria-label="关闭新建任务" :disabled="submitting" @click="emit('close')">
          <X :size="18" />
        </button>
      </header>

      <form class="create-detail-form" @submit.prevent="emit('submit')">
        <label class="create-title-field">
          <span>任务标题</span>
          <input
            v-focus
            v-model="title"
            type="text"
            maxlength="100"
            placeholder="准备做什么？"
            @input="emit('clear-error')"
          />
          <small>{{ title.length }} / 100</small>
        </label>

        <section class="create-form-section create-properties-section">
          <div class="create-section-heading">
            <div><span class="create-section-icon status-icon"><Circle :size="16" /></span><strong>状态</strong></div>
          </div>
          <div class="create-choice-grid status-choice-grid" aria-label="任务状态">
            <button
              v-for="option in statusOptions"
              :key="option.value"
              type="button"
              :class="[`status-${option.value}`, { active: status === option.value }]"
              @click="status = option.value; emit('status-change', option.value)"
            >
              <Circle v-if="option.value === 'TODO'" :size="15" />
              <RefreshCw v-else-if="option.value === 'IN_PROGRESS'" :size="15" />
              <Check v-else :size="15" />
              <span>{{ option.label }}</span>
            </button>
          </div>

          <div class="create-section-heading priority-create-heading">
            <div><span class="create-section-icon priority-icon"><Flag :size="16" /></span><strong>优先级</strong></div>
          </div>
          <div class="create-choice-grid priority-choice-grid" aria-label="任务优先级">
            <button
              v-for="option in priorityOptions"
              :key="option.value"
              type="button"
              :class="[option.tone, { active: priority === option.value }]"
              @click="priority = option.value; emit('priority-change', option.value)"
            >
              <Flag :size="14" />
              <span>{{ option.label }}</span>
            </button>
          </div>
        </section>

        <section class="create-form-section">
          <div class="create-section-heading">
            <div><span class="create-section-icon description-icon"><AlignLeft :size="16" /></span><strong>描述</strong></div>
            <small>{{ description.length }} / 100</small>
          </div>
          <textarea
            v-model="description"
            maxlength="100"
            rows="4"
            placeholder="补充任务背景、目标或注意事项"
            @input="emit('clear-error')"
          ></textarea>
        </section>

        <section class="create-form-section create-due-section">
          <div class="create-section-heading">
            <div>
              <span class="create-section-icon due-icon"><CalendarDays :size="16" /></span>
              <span><strong>截止时间</strong><small>{{ dueLabel }}</small></span>
            </div>
            <button v-if="hasDueValue" type="button" @click="emit('clear-due')">清除</button>
          </div>

          <div class="create-due-presets">
            <button
              v-for="preset in duePresets"
              :key="preset.value"
              type="button"
              :class="{ active: isDuePresetActive(preset.value) }"
              @click="emit('apply-due-preset', preset.value)"
            >
              <strong>{{ preset.label }}</strong>
              <small>{{ preset.meta }}</small>
            </button>
          </div>

          <button
            class="due-custom-toggle"
            type="button"
            :class="{ open: customDueOpen }"
            :aria-expanded="customDueOpen"
            @click="emit('toggle-custom-due')"
          >
            <span><Clock3 :size="15" /> 自定义日期与时间</span>
            <ChevronDown :size="15" />
          </button>

          <Transition name="property-reveal">
            <div v-if="customDueOpen" class="due-custom-fields">
              <div class="date-part-grid create-date-grid" aria-label="新任务截止日期">
                <label v-for="part in ['year', 'month', 'day']" :key="part">
                  <span>{{ { year: '年', month: '月', day: '日' }[part] }}</span>
                  <input
                    :value="dateParts[part]"
                    inputmode="numeric"
                    :maxlength="part === 'year' ? 4 : 2"
                    :placeholder="{ year: '2026', month: '07', day: '31' }[part]"
                    @input="emit('update-date-part', part, $event.target.value)"
                  />
                </label>
              </div>

              <div class="time-part-grid create-time-grid" aria-label="新任务截止时间">
                <label>
                  <span>时</span>
                  <input :value="timeParts.hour" inputmode="numeric" maxlength="2" placeholder="23" @input="emit('update-time-part', 'hour', $event.target.value)" />
                </label>
                <i>:</i>
                <label>
                  <span>分</span>
                  <input :value="timeParts.minute" inputmode="numeric" maxlength="2" placeholder="59" @input="emit('update-time-part', 'minute', $event.target.value)" />
                </label>
              </div>
            </div>
          </Transition>
        </section>

        <section class="create-form-section create-steps-section">
          <div class="create-section-heading">
            <div>
              <span class="create-section-icon steps-icon"><ListChecks :size="17" /></span>
              <span><strong>执行步骤</strong><small>{{ stepDrafts.length ? `${stepDrafts.length} 个步骤` : '尚未添加' }}</small></span>
            </div>
          </div>

          <TransitionGroup v-if="stepDrafts.length" name="create-step" tag="div" class="create-step-list">
            <div v-for="(step, index) in stepDrafts" :key="step.id" class="create-step-row">
              <span>{{ index + 1 }}</span>
              <p>{{ step.title }}</p>
              <button type="button" :aria-label="`移除步骤 ${step.title}`" title="移除" @click="emit('remove-step', step.id)">
                <X :size="14" />
              </button>
            </div>
          </TransitionGroup>

          <div class="create-step-composer">
            <Plus :size="16" />
            <input
              v-model="stepDraft"
              type="text"
              maxlength="100"
              placeholder="添加一个执行步骤"
              @input="emit('clear-error')"
              @keydown.enter.prevent="emit('add-step')"
            />
            <button type="button" :disabled="!stepDraft.trim()" aria-label="添加执行步骤" @click="emit('add-step')">
              <Plus :size="15" />
            </button>
          </div>
        </section>

        <p v-if="errorMessage" class="notice error create-panel-error" role="alert">{{ errorMessage }}</p>

        <footer class="create-panel-actions">
          <button type="button" :disabled="submitting" @click="emit('close')">取消</button>
          <button class="primary-button" type="submit" :disabled="submitting || !valid">
            <RefreshCw v-if="submitting" class="spin-icon" :size="16" />
            <Plus v-else :size="17" />
            <span>{{ submitting ? '创建中...' : '创建任务' }}</span>
          </button>
        </footer>
      </form>
    </div>
  </aside>
</template>
