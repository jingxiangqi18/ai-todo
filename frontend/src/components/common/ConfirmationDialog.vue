<script setup>
import { ref } from 'vue'
import { RefreshCw, X } from '@lucide/vue'

defineProps({
  open: Boolean,
  content: {
    type: Object,
    required: true
  },
  pending: Boolean,
  errorMessage: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['close', 'confirm', 'keydown'])
const cancelButtonRef = ref(null)

defineExpose({
  focus: () => cancelButtonRef.value?.focus()
})
</script>

<template>
  <Teleport to="body">
    <Transition name="delete-dialog">
      <div v-if="open" class="delete-dialog-overlay" @click.self="emit('close')">
        <section
          class="delete-dialog"
          :class="`${content.kind}-dialog`"
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="confirmation-dialog-title"
          aria-describedby="confirmation-dialog-description"
          @keydown="emit('keydown', $event)"
        >
          <header class="delete-dialog-header">
            <span class="delete-dialog-mark">
              <component :is="content.icon" :size="20" />
            </span>
            <span>{{ content.eyebrow }}</span>
            <button
              class="icon-button delete-dialog-close"
              type="button"
              aria-label="关闭确认窗口"
              :disabled="pending"
              @click="emit('close')"
            >
              <X :size="18" />
            </button>
          </header>

          <div class="delete-dialog-copy">
            <h2 id="confirmation-dialog-title">{{ content.title }}</h2>
            <p id="confirmation-dialog-description">{{ content.description }}</p>
          </div>

          <div class="delete-dialog-warning">
            <component :is="content.warningIcon" :size="16" />
            <span>{{ content.warning }}</span>
          </div>

          <p v-if="content.kind !== 'logout' && errorMessage" class="delete-dialog-error" role="alert">
            {{ errorMessage }}
          </p>

          <footer class="delete-dialog-actions">
            <button
              ref="cancelButtonRef"
              class="delete-cancel-button"
              type="button"
              :disabled="pending"
              @click="emit('close')"
            >
              取消
            </button>
            <button
              class="delete-confirm-button"
              :class="{
                'logout-confirm-button': content.kind === 'logout',
                'leave-group-confirm-button': content.kind === 'leave-group',
                'member-role-confirm-button': content.kind === 'member-role'
              }"
              type="button"
              :disabled="pending"
              @click="emit('confirm')"
            >
              <RefreshCw v-if="pending" class="spin-icon" :size="16" />
              <component :is="content.icon" v-else :size="16" />
              <span>{{ pending ? content.busyLabel : content.confirmLabel }}</span>
            </button>
          </footer>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>
