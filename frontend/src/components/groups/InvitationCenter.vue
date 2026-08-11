<script setup>
import { ref } from 'vue'
import { Check, ChevronRight, RefreshCw, UserPlus, UsersRound, X } from '@lucide/vue'
import { formatInvitationDate, groupInitial } from '../../utils/groups'

defineProps({
  open: Boolean,
  invitations: {
    type: Array,
    default: () => []
  },
  pendingCount: {
    type: Number,
    default: 0
  },
  isLoading: Boolean,
  errorMessage: {
    type: String,
    default: ''
  },
  acceptedGroup: {
    type: Object,
    default: null
  },
  actionMessage: {
    type: String,
    default: ''
  },
  operationPending: Boolean,
  acceptingId: {
    type: [String, Number],
    default: null
  },
  rejectingId: {
    type: [String, Number],
    default: null
  },
  rejectConfirmationId: {
    type: [String, Number],
    default: null
  }
})

const emit = defineEmits([
  'close',
  'open-accepted',
  'retry',
  'cancel-rejection',
  'request-rejection',
  'reject',
  'accept'
])
const closeButtonRef = ref(null)

defineExpose({
  focus: () => closeButtonRef.value?.focus()
})
</script>

<template>
  <Transition name="invitation-drawer">
    <div v-if="open" class="invitation-drawer-overlay" @click.self="emit('close')">
      <section
        class="invitation-drawer"
        role="dialog"
        aria-modal="true"
        aria-labelledby="invitation-center-title"
      >
        <header class="invitation-drawer-header">
          <div>
            <span>WORKSPACE INVITES</span>
            <h2 id="invitation-center-title">协作邀请</h2>
          </div>
          <button
            ref="closeButtonRef"
            class="icon-button"
            type="button"
            :disabled="operationPending"
            aria-label="关闭协作邀请"
            @click="emit('close')"
          >
            <X :size="19" />
          </button>
        </header>

        <div class="invitation-drawer-summary">
          <span class="invitation-summary-icon"><UsersRound :size="18" /></span>
          <div>
            <strong>{{ errorMessage ? '邀请暂时无法读取' : `${pendingCount} 个待处理邀请` }}</strong>
            <span>{{ errorMessage ? '请重试以获取最新邀请' : '加入后，工作组会显示在你的侧栏中' }}</span>
          </div>
        </div>

        <div class="invitation-drawer-content">
          <div v-if="acceptedGroup" class="invitation-accepted" role="status">
            <span><Check :size="17" /></span>
            <div>
              <strong>已加入 {{ acceptedGroup.name }}</strong>
              <small>成员身份已经同步</small>
            </div>
            <button type="button" @click="emit('open-accepted')">
              打开工作组
              <ChevronRight :size="15" />
            </button>
          </div>

          <div v-if="actionMessage" class="invitation-action-notice" role="status">
            <Check :size="15" />
            <span>{{ actionMessage }}</span>
          </div>

          <div v-if="errorMessage" class="invitation-list-error" role="alert">
            <span>{{ errorMessage }}</span>
            <button type="button" :disabled="isLoading" @click="emit('retry')">重试</button>
          </div>

          <div v-if="isLoading" class="invitation-list-loading" aria-label="正在加载协作邀请">
            <div v-for="index in 3" :key="index">
              <span></span>
              <i></i>
              <i></i>
            </div>
          </div>

          <div v-else-if="!errorMessage && invitations.length" class="invitation-list">
            <article v-for="invitation in invitations" :key="invitation.id" class="invitation-card">
              <div class="invitation-card-mark">{{ groupInitial(invitation.groupName) }}</div>
              <div class="invitation-card-copy">
                <span>工作组邀请</span>
                <h3>{{ invitation.groupName }}</h3>
                <p><b>{{ invitation.inviterName }}</b> 邀请你加入协作</p>
                <time :datetime="invitation.createdAt">{{ formatInvitationDate(invitation.createdAt) }}</time>
              </div>
              <div
                class="invitation-card-actions"
                :class="{ confirming: String(rejectConfirmationId) === String(invitation.id) }"
              >
                <template v-if="String(rejectConfirmationId) === String(invitation.id)">
                  <span>确定拒绝这条邀请？</span>
                  <button type="button" :disabled="operationPending" @click="emit('cancel-rejection')">取消</button>
                  <button
                    class="invitation-reject-confirm"
                    type="button"
                    :disabled="operationPending"
                    @click="emit('reject', invitation)"
                  >
                    <RefreshCw
                      v-if="String(rejectingId) === String(invitation.id)"
                      class="spin-icon"
                      :size="14"
                    />
                    <X v-else :size="14" />
                    {{ String(rejectingId) === String(invitation.id) ? '处理中' : '确认拒绝' }}
                  </button>
                </template>

                <template v-else>
                  <button
                    class="invitation-reject-button"
                    type="button"
                    :disabled="operationPending"
                    @click="emit('request-rejection', invitation.id)"
                  >
                    <X :size="15" />
                    <span>拒绝</span>
                  </button>
                  <button
                    class="invitation-accept-button"
                    type="button"
                    :disabled="operationPending"
                    @click="emit('accept', invitation)"
                  >
                    <RefreshCw
                      v-if="String(acceptingId) === String(invitation.id)"
                      class="spin-icon"
                      :size="15"
                    />
                    <Check v-else :size="16" />
                    <span>{{ String(acceptingId) === String(invitation.id) ? '加入中' : '接受邀请' }}</span>
                  </button>
                </template>
              </div>
            </article>
          </div>

          <div v-else-if="!errorMessage" class="invitation-empty">
            <span><UserPlus :size="25" /></span>
            <strong>{{ acceptedGroup ? '其他邀请都处理完了' : '暂无待处理邀请' }}</strong>
            <p>新的工作组邀请会出现在这里。</p>
          </div>
        </div>
      </section>
    </div>
  </Transition>
</template>
