<script setup>
import {
  AlignLeft,
  CalendarDays,
  Check,
  Circle,
  Clock3,
  Flag,
  GripVertical,
  RefreshCw,
  UserRoundCheck,
  UserRoundPlus,
  X
} from '@lucide/vue'
import { formatDueAt, formatFullDateTime } from '../../utils/dateTime'
import { groupInitial } from '../../utils/groups'
import { priorityText, statusText } from '../../utils/tasks'

defineProps({
  task: {
    type: Object,
    required: true
  },
  groupName: {
    type: String,
    default: '当前工作组'
  },
  loading: Boolean,
  errorMessage: {
    type: String,
    default: ''
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

const emit = defineEmits(['close', 'retry', 'resize-start', 'resize-reset', 'resize-keydown'])
</script>

<template>
  <aside class="detail-panel group-task-detail-panel" :aria-busy="loading">
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
        <button type="button" class="icon-button" aria-label="关闭团队任务详情" @click="emit('close')">
          <X :size="18" />
        </button>
      </header>

      <div v-if="loading" class="group-task-detail-loading"><span></span></div>

      <div v-if="errorMessage" class="notice error group-task-detail-error" role="alert">
        <span>{{ errorMessage }}</span>
        <button type="button" @click="emit('retry')">重试</button>
      </div>

      <article class="group-task-detail-content">
        <header class="group-task-detail-hero">
          <span>TEAM TASK</span>
          <h2>{{ task.title }}</h2>
          <div>
            <span class="group-task-detail-status" :class="`status-${task.status}`">
              <Check v-if="task.status === 'DONE'" :size="13" />
              <RefreshCw v-else-if="task.status === 'IN_PROGRESS'" :size="13" />
              <Circle v-else :size="13" />
              {{ statusText(task.status) }}
            </span>
            <span class="group-task-detail-priority" :class="`priority-${task.priority}`">
              <Flag :size="13" />
              {{ priorityText(task.priority) }}优先级
            </span>
          </div>
        </header>

        <section class="group-task-detail-section group-task-assignment-detail">
          <header><UserRoundCheck :size="16" /><span>任务负责人</span></header>
          <div>
            <span class="group-task-detail-avatar">
              {{ task.assigneeName ? groupInitial(task.assigneeName) : '—' }}
            </span>
            <span>
              <strong>{{ task.assigneeName || '暂未分配' }}</strong>
              <small>{{ task.assigneeName ? '负责推进该任务' : '当前没有指定负责人' }}</small>
            </span>
          </div>
        </section>

        <section class="group-task-detail-section group-task-description-detail">
          <header><AlignLeft :size="16" /><span>任务描述</span></header>
          <p>{{ task.description || '暂无任务描述。' }}</p>
        </section>

        <section class="group-task-detail-section group-task-due-detail">
          <header><CalendarDays :size="16" /><span>截止时间</span></header>
          <strong>{{ formatDueAt(task.dueAt) }}</strong>
        </section>

        <section class="group-task-detail-section group-task-activity-detail">
          <header><Clock3 :size="16" /><span>活动信息</span></header>
          <dl>
            <div>
              <dt><UserRoundPlus :size="14" />创建者</dt>
              <dd>{{ task.creatorName }}</dd>
            </div>
            <div>
              <dt>创建</dt>
              <dd>{{ formatFullDateTime(task.createdAt) }}</dd>
            </div>
            <div>
              <dt>更新</dt>
              <dd>{{ formatFullDateTime(task.updatedAt) }}</dd>
            </div>
          </dl>
        </section>
      </article>
    </div>
  </aside>
</template>
