<script setup>
import { Building2, FolderPlus, GripVertical, RefreshCw, X } from '@lucide/vue'

defineProps({
  open: Boolean,
  submitting: Boolean,
  valid: Boolean,
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

const emit = defineEmits(['close', 'submit', 'clear-error', 'resize-start', 'resize-reset', 'resize-keydown'])
const name = defineModel('name', { type: String, default: '' })
const description = defineModel('description', { type: String, default: '' })
const vFocus = {
  mounted(element) {
    element.focus()
    element.select()
  }
}
</script>

<template>
  <aside v-if="open" class="detail-panel create-panel group-create-panel">
    <div
      class="detail-resize-handle"
      role="separator"
      aria-label="调整工作组创建栏宽度"
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
          <p>创建工作组</p>
          <span>新的协作空间</span>
        </div>
        <button type="button" class="icon-button" aria-label="关闭创建工作组" :disabled="submitting" @click="emit('close')">
          <X :size="18" />
        </button>
      </header>

      <form class="create-detail-form group-create-form" @submit.prevent="emit('submit')">
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
            v-model="name"
            type="text"
            maxlength="100"
            placeholder="例如：课程项目组"
            @input="emit('clear-error')"
          />
          <small>{{ name.length }} / 100</small>
        </label>

        <label class="group-description-field">
          <span>工作组描述</span>
          <textarea
            v-model="description"
            maxlength="500"
            rows="7"
            placeholder="记录工作组的目标、范围或协作约定"
            @input="emit('clear-error')"
          ></textarea>
          <small>{{ description.length }} / 500</small>
        </label>

        <p v-if="errorMessage" class="notice error create-panel-error" role="alert">{{ errorMessage }}</p>

        <footer class="create-panel-actions">
          <button type="button" :disabled="submitting" @click="emit('close')">取消</button>
          <button class="primary-button" type="submit" :disabled="submitting || !valid">
            <RefreshCw v-if="submitting" class="spin-icon" :size="16" />
            <FolderPlus v-else :size="17" />
            <span>{{ submitting ? '创建中...' : '创建工作组' }}</span>
          </button>
        </footer>
      </form>
    </div>
  </aside>
</template>
