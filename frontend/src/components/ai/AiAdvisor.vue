<script setup>
import { ref } from 'vue'
import { BrainCircuit, Check, Copy, SendHorizontal, X } from '@lucide/vue'

defineProps({
  open: Boolean,
  promptOptions: {
    type: Array,
    default: () => []
  },
  unfinishedCount: {
    type: Number,
    default: 0
  },
  stats: {
    type: Object,
    required: true
  },
  isSubmitting: Boolean,
  isValid: Boolean,
  errorMessage: {
    type: String,
    default: ''
  },
  advice: {
    type: String,
    default: ''
  },
  adviceBlocks: {
    type: Array,
    default: () => []
  },
  copied: Boolean
})

const emit = defineEmits(['close', 'apply-prompt', 'submit', 'copy'])
const message = defineModel('message', { type: String, default: '' })
const messageInputRef = ref(null)

defineExpose({
  focus: () => messageInputRef.value?.focus()
})
</script>

<template>
  <Transition name="ai-overlay">
    <div v-if="open" class="ai-advisor-overlay" @click.self="emit('close')">
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
          <button class="icon-button ai-close" type="button" aria-label="关闭 AI 规划" @click="emit('close')">
            <X :size="19" />
          </button>
        </header>

        <div class="ai-context-strip" aria-label="当前任务上下文">
          <span><b>{{ unfinishedCount }}</b> 未完成</span>
          <span><b>{{ stats.inProgress }}</b> 进行中</span>
          <span><b>{{ stats.highPriority }}</b> 高优先级</span>
          <span><b>{{ stats.dueToday }}</b> 今天截止</span>
        </div>

        <div class="ai-workspace">
          <form class="ai-prompt-panel" @submit.prevent="emit('submit')">
            <div class="ai-section-heading">
              <span>01</span>
              <div>
                <small>CONTEXT</small>
                <h3>补充你的当前状态</h3>
              </div>
            </div>

            <div class="ai-prompt-options" aria-label="快捷咨询条件">
              <button
                v-for="option in promptOptions"
                :key="option.label"
                type="button"
                :class="{ active: message === option.prompt }"
                @click="emit('apply-prompt', option.prompt)"
              >
                <component :is="option.icon" :size="15" />
                <span>{{ option.label }}</span>
              </button>
            </div>

            <label class="ai-message-field">
              <span class="sr-only">咨询内容</span>
              <textarea
                ref="messageInputRef"
                v-model="message"
                maxlength="1000"
                rows="7"
                placeholder="例如：我现在有 40 分钟，精力一般，希望先推进最紧急的任务。"
                @keydown.ctrl.enter.prevent="emit('submit')"
                @keydown.meta.enter.prevent="emit('submit')"
              ></textarea>
            </label>

            <p v-if="errorMessage" class="notice error ai-error" role="alert">{{ errorMessage }}</p>

            <div class="ai-prompt-footer">
              <span :class="{ over: message.length > 1000 }">{{ message.length }} / 1000</span>
              <button class="ai-submit" type="submit" :disabled="isSubmitting || !isValid">
                <SendHorizontal :size="16" />
                <span>{{ isSubmitting ? '正在规划' : advice ? '重新规划' : '生成安排' }}</span>
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
                v-if="advice && !isSubmitting"
                class="ai-copy"
                type="button"
                :aria-label="copied ? '已复制建议' : '复制建议'"
                :title="copied ? '已复制' : '复制建议'"
                @click="emit('copy')"
              >
                <Check v-if="copied" :size="15" />
                <Copy v-else :size="15" />
              </button>
            </div>

            <div v-if="isSubmitting" class="ai-thinking" role="status">
              <div class="thinking-lines" aria-hidden="true">
                <span></span>
                <span></span>
                <span></span>
              </div>
              <p>正在结合任务进度与截止时间...</p>
            </div>

            <article v-else-if="advice" class="ai-advice-content">
              <template v-for="(block, index) in adviceBlocks" :key="`${block.type}-${index}`">
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
</template>
