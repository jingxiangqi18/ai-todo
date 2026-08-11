<script setup>
import {
  Check,
  Crown,
  FolderPlus,
  LogOut,
  Menu,
  RefreshCw,
  SendHorizontal,
  UserPlus,
  UsersRound,
  X
} from '@lucide/vue'
import { ref } from 'vue'
import { formatGroupDate, groupInitial, groupRoleLabel } from '../../utils/groups'

defineProps({
  group: {
    type: Object,
    required: true
  },
  members: {
    type: Array,
    default: () => []
  },
  roleLabel: {
    type: String,
    default: '成员'
  },
  pendingInvitationCount: {
    type: Number,
    default: 0
  },
  detailError: {
    type: String,
    default: ''
  },
  isLoading: Boolean,
  canInvite: Boolean,
  isInviteOpen: Boolean,
  isInviteSubmitting: Boolean,
  isInvitationValid: Boolean,
  invitationError: {
    type: String,
    default: ''
  },
  invitationMessage: {
    type: String,
    default: ''
  }
})

const emit = defineEmits([
  'open-sidebar',
  'create-group',
  'leave',
  'retry',
  'toggle-invite',
  'close-invite',
  'submit-invite',
  'clear-invitation-feedback'
])
const invitationAccount = defineModel('invitationAccount', { type: String, default: '' })
const invitePanelRef = ref(null)
const vFocus = {
  mounted(element) {
    element.focus()
    element.select()
  }
}

defineExpose({
  contains: (target) => Boolean(invitePanelRef.value?.contains(target))
})
</script>

<template>
  <header class="board-header group-board-header">
    <div class="board-title">
      <button class="mobile-menu-button" type="button" aria-label="打开导航" @click="emit('open-sidebar')">
        <Menu :size="20" />
        <b v-if="pendingInvitationCount">{{ pendingInvitationCount > 99 ? '99+' : pendingInvitationCount }}</b>
      </button>
      <p class="date-line">协作工作组</p>
      <h1>{{ group.name }}</h1>
    </div>

    <div class="board-primary-actions group-primary-actions">
      <button class="primary-button group-create-trigger" type="button" @click="emit('create-group')">
        <FolderPlus :size="17" />
        <span>新建工作组</span>
      </button>
    </div>
  </header>

  <div class="board-summary group-summary" aria-label="工作组概览">
    <span><b>{{ members.length }}</b> 位成员</span>
    <span class="group-role-summary">
      <Crown v-if="group.currentUserRole === 'OWNER'" :size="13" />
      <UsersRound v-else :size="13" />
      {{ roleLabel }}
    </span>
    <span>创建于 {{ formatGroupDate(group.createdAt) }}</span>
    <button
      v-if="group.currentUserRole === 'MEMBER'"
      class="group-leave-trigger"
      type="button"
      @click="emit('leave')"
    >
      <LogOut :size="14" />
      <span>退出工作组</span>
    </button>
  </div>

  <div v-if="detailError" class="notice error list-error" role="alert">
    <span>{{ detailError }}</span>
    <button type="button" @click="emit('retry')">重试</button>
  </div>

  <div v-if="isLoading" class="group-workspace group-workspace-loading" aria-label="正在加载工作组">
    <div class="group-profile-skeleton"><span></span><i></i><i></i></div>
    <div class="group-member-skeleton" v-for="index in 3" :key="index"><span></span><i></i></div>
  </div>

  <section v-else class="group-workspace">
    <header class="group-profile-band">
      <span class="group-profile-mark">{{ groupInitial(group.name) }}</span>
      <div class="group-profile-copy">
        <span>WORKSPACE</span>
        <h2>{{ group.name }}</h2>
        <p>{{ group.description || '这个工作组暂时没有填写描述。' }}</p>
      </div>
      <span class="group-role-badge" :class="{ owner: group.currentUserRole === 'OWNER' }">
        <Crown v-if="group.currentUserRole === 'OWNER'" :size="14" />
        <UsersRound v-else :size="14" />
        {{ roleLabel }}
      </span>
    </header>

    <div class="group-workspace-divider"></div>

    <section class="group-members-section">
      <header>
        <div class="group-members-heading-copy">
          <span>成员</span>
          <strong>{{ members.length }}</strong>
        </div>

        <div class="group-members-actions">
          <small>按加入时间排列</small>
          <div v-if="canInvite" ref="invitePanelRef" class="group-member-invite">
            <button
              class="group-invite-trigger"
              type="button"
              :class="{ active: isInviteOpen }"
              :aria-expanded="isInviteOpen"
              @click="emit('toggle-invite')"
            >
              <UserPlus :size="14" />
              <span>邀请成员</span>
            </button>

            <Transition name="property-reveal">
              <form v-if="isInviteOpen" class="group-invite-panel" @submit.prevent="emit('submit-invite')">
                <header>
                  <span><UserPlus :size="16" /></span>
                  <div>
                    <strong>邀请成员</strong>
                    <small>用户名或邮箱</small>
                  </div>
                  <button type="button" aria-label="关闭邀请面板" @click="emit('close-invite')">
                    <X :size="14" />
                  </button>
                </header>

                <label>
                  <span class="sr-only">被邀请用户账号</span>
                  <input
                    v-focus
                    v-model="invitationAccount"
                    type="text"
                    maxlength="100"
                    autocomplete="off"
                    placeholder="输入用户名或邮箱"
                    @input="emit('clear-invitation-feedback')"
                  />
                  <button type="submit" :disabled="isInviteSubmitting || !isInvitationValid">
                    <RefreshCw v-if="isInviteSubmitting" class="spin-icon" :size="14" />
                    <SendHorizontal v-else :size="14" />
                    <span>{{ isInviteSubmitting ? '发送中' : '发送邀请' }}</span>
                  </button>
                </label>

                <p v-if="invitationError" class="group-invite-feedback error" role="alert">{{ invitationError }}</p>
                <p v-if="invitationMessage" class="group-invite-feedback success" role="status">
                  <Check :size="14" />
                  <span>{{ invitationMessage }}</span>
                </p>
              </form>
            </Transition>
          </div>
        </div>
      </header>

      <div v-if="members.length" class="group-member-list">
        <article v-for="member in members" :key="member.userId" class="group-member-row">
          <span class="group-member-avatar">{{ groupInitial(member.username) }}</span>
          <div>
            <strong>{{ member.username }}</strong>
            <small>{{ String(member.userId) === String(group.ownerId) ? '工作组创建者' : '工作组成员' }}</small>
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
