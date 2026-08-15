<script setup>
import {
  CalendarDays,
  Check,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  ClipboardPlus,
  Crown,
  Flag,
  FolderPlus,
  LogOut,
  ListTodo,
  Menu,
  RefreshCw,
  SendHorizontal,
  ShieldCheck,
  UserPlus,
  UserRoundCheck,
  UsersRound,
  X
} from '@lucide/vue'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { formatDueAt } from '../../utils/dateTime'
import { formatGroupDate, groupInitial, groupRoleLabel } from '../../utils/groups'
import { priorityText, statusText } from '../../utils/tasks'

const props = defineProps({
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
  canCreateTask: Boolean,
  createdTask: {
    type: Object,
    default: null
  },
  tasks: {
    type: Array,
    default: () => []
  },
  taskPage: {
    type: Object,
    required: true
  },
  taskListLoading: Boolean,
  taskListError: {
    type: String,
    default: ''
  },
  selectedTaskId: {
    type: [String, Number],
    default: null
  },
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
  },
  memberRolePendingUserId: {
    type: [String, Number],
    default: null
  },
  memberRoleError: {
    type: String,
    default: ''
  },
  memberRoleMessage: {
    type: String,
    default: ''
  }
})

const emit = defineEmits([
  'open-sidebar',
  'create-group',
  'create-task',
  'dismiss-created-task',
  'open-task',
  'close-task',
  'retry-tasks',
  'change-task-page',
  'change-task-page-size',
  'leave',
  'retry',
  'toggle-invite',
  'close-invite',
  'submit-invite',
  'clear-invitation-feedback',
  'update-role'
])
const invitationAccount = defineModel('invitationAccount', { type: String, default: '' })
const invitePanelRef = ref(null)
const openRoleMenuUserId = ref(null)
const activeWorkspaceSection = ref('tasks')
const vFocus = {
  mounted(element) {
    element.focus()
    element.select()
  }
}

defineExpose({
  contains: (target) => Boolean(invitePanelRef.value?.contains(target))
})

function toggleRoleMenu(userId) {
  if (String(props.memberRolePendingUserId) === String(userId)) {
    return
  }

  openRoleMenuUserId.value = String(openRoleMenuUserId.value) === String(userId) ? null : userId
}

function selectMemberRole(member, role) {
  openRoleMenuUserId.value = null
  emit('update-role', member, role)
}

function selectWorkspaceSection(section) {
  if (activeWorkspaceSection.value === section) {
    return
  }

  activeWorkspaceSection.value = section
  openRoleMenuUserId.value = null

  if (section === 'tasks' && props.isInviteOpen) {
    emit('close-invite')
  }

  if (section === 'members') {
    emit('close-task')
  }
}

function closeRoleMenuOnOutsidePointer(event) {
  if (openRoleMenuUserId.value && !event.target.closest?.('.member-role-control')) {
    openRoleMenuUserId.value = null
  }
}

watch(() => props.group.id, () => {
  activeWorkspaceSection.value = 'tasks'
  openRoleMenuUserId.value = null
})

watch(() => props.createdTask?.id, (taskId) => {
  if (taskId) {
    activeWorkspaceSection.value = 'tasks'
  }
})

onMounted(() => document.addEventListener('pointerdown', closeRoleMenuOnOutsidePointer))
onBeforeUnmount(() => document.removeEventListener('pointerdown', closeRoleMenuOnOutsidePointer))
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
      <button
        v-if="canCreateTask"
        class="primary-button group-task-create-trigger"
        type="button"
        aria-label="新建团队任务"
        title="新建团队任务"
        @click="emit('create-task')"
      >
        <ClipboardPlus :size="17" />
        <span>新建团队任务</span>
      </button>
      <button
        class="group-create-trigger"
        type="button"
        aria-label="新建工作组"
        title="新建工作组"
        @click="emit('create-group')"
      >
        <FolderPlus :size="17" />
        <span>新建工作组</span>
      </button>
    </div>
  </header>

  <div class="board-summary group-summary" aria-label="工作组概览">
    <span><b>{{ taskPage.total }}</b> 项团队任务</span>
    <span><b>{{ members.length }}</b> 位成员</span>
    <span class="group-role-summary">
      <Crown v-if="group.currentUserRole === 'OWNER'" :size="13" />
      <ShieldCheck v-else-if="group.currentUserRole === 'ADMIN'" :size="13" />
      <UsersRound v-else :size="13" />
      {{ roleLabel }}
    </span>
    <span>创建于 {{ formatGroupDate(group.createdAt) }}</span>
    <button
      v-if="group.currentUserRole !== 'OWNER'"
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
      <span
        class="group-role-badge"
        :class="{ owner: group.currentUserRole === 'OWNER', admin: group.currentUserRole === 'ADMIN' }"
      >
        <Crown v-if="group.currentUserRole === 'OWNER'" :size="14" />
        <ShieldCheck v-else-if="group.currentUserRole === 'ADMIN'" :size="14" />
        <UsersRound v-else :size="14" />
        {{ roleLabel }}
      </span>
    </header>

    <nav class="group-workspace-tabs" role="tablist" aria-label="工作组内容">
      <button
        type="button"
        role="tab"
        :class="{ active: activeWorkspaceSection === 'tasks' }"
        :aria-selected="activeWorkspaceSection === 'tasks'"
        aria-controls="group-tasks-panel"
        @click="selectWorkspaceSection('tasks')"
      >
        <ListTodo :size="16" />
        <span>团队任务</span>
        <b>{{ taskPage.total }}</b>
      </button>
      <button
        type="button"
        role="tab"
        :class="{ active: activeWorkspaceSection === 'members' }"
        :aria-selected="activeWorkspaceSection === 'members'"
        aria-controls="group-members-panel"
        @click="selectWorkspaceSection('members')"
      >
        <UsersRound :size="16" />
        <span>成员管理</span>
        <b>{{ members.length }}</b>
      </button>
    </nav>

    <Transition name="workspace-view" mode="out-in">
      <div v-if="activeWorkspaceSection === 'tasks'" id="group-tasks-panel" key="tasks" role="tabpanel" class="group-workspace-view">
        <Transition name="group-task-receipt">
          <article v-if="createdTask" class="group-task-receipt" aria-live="polite">
            <span class="group-task-receipt-mark"><Check :size="17" /></span>
            <div class="group-task-receipt-copy">
              <small>团队任务已创建</small>
              <strong>{{ createdTask.title }}</strong>
            </div>
            <div class="group-task-receipt-meta">
              <span><UserRoundCheck :size="13" /> {{ createdTask.assigneeName || '暂未分配' }}</span>
              <span :class="`priority-${createdTask.priority}`"><Flag :size="13" /> {{ priorityText(createdTask.priority) }}优先级</span>
              <span><CalendarDays :size="13" /> {{ formatDueAt(createdTask.dueAt) }}</span>
            </div>
            <button type="button" aria-label="关闭任务创建结果" title="关闭" @click="emit('dismiss-created-task')">
              <X :size="15" />
            </button>
          </article>
        </Transition>

        <section class="group-tasks-section">
      <header class="group-section-header">
        <div class="group-members-heading-copy group-tasks-heading-copy">
          <span>团队任务</span>
          <strong>{{ taskPage.total }}</strong>
        </div>
        <small>按创建时间排列</small>
      </header>

      <div v-if="taskListError" class="group-task-list-error" role="alert">
        <span>{{ taskListError }}</span>
        <button type="button" @click="emit('retry-tasks')">重新加载</button>
      </div>

      <div v-else-if="taskListLoading" class="group-task-list-loading" aria-label="正在加载团队任务">
        <span v-for="index in 3" :key="index"><i></i><i></i><i></i></span>
      </div>

      <template v-else-if="tasks.length">
        <div class="group-task-columns" aria-hidden="true">
          <span>任务</span>
          <span>负责人</span>
          <span>状态</span>
          <span>截止时间</span>
        </div>

        <div class="group-task-list">
          <button
            v-for="task in tasks"
            :key="task.id"
            type="button"
            class="group-task-row"
            :class="[`status-${task.status}`, { selected: String(selectedTaskId) === String(task.id) }]"
            @click="emit('open-task', task)"
          >
            <span class="group-task-title-cell">
              <i :class="`priority-${task.priority}`"><Flag :size="12" /></i>
              <span>
                <strong>{{ task.title }}</strong>
                <small>{{ priorityText(task.priority) }}优先级 · {{ task.creatorName }} 创建</small>
              </span>
            </span>
            <span class="group-task-assignee-cell">
              <i>{{ task.assigneeName ? groupInitial(task.assigneeName) : '—' }}</i>
              <span>{{ task.assigneeName || '未分配' }}</span>
            </span>
            <span class="group-task-status-cell" :class="`status-${task.status}`">
              <Check v-if="task.status === 'DONE'" :size="12" />
              <RefreshCw v-else-if="task.status === 'IN_PROGRESS'" :size="12" />
              <span v-else class="status-dot"></span>
              {{ statusText(task.status) }}
            </span>
            <time class="group-task-due-cell" :class="{ empty: !task.dueAt }">
              <CalendarDays :size="13" />
              {{ formatDueAt(task.dueAt) }}
            </time>
          </button>
        </div>

        <footer class="group-task-pagination">
          <span>第 {{ taskPage.page }} / {{ taskPage.pages }} 页</span>
          <div class="group-task-page-nav">
            <button
              type="button"
              aria-label="上一页团队任务"
              :disabled="taskPage.page <= 1"
              @click="emit('change-task-page', taskPage.page - 1)"
            >
              <ChevronLeft :size="15" />
            </button>
            <button
              type="button"
              aria-label="下一页团队任务"
              :disabled="taskPage.page >= taskPage.pages"
              @click="emit('change-task-page', taskPage.page + 1)"
            >
              <ChevronRight :size="15" />
            </button>
          </div>
          <div class="group-task-page-sizes" aria-label="团队任务每页数量">
            <button
              v-for="size in [5, 10, 20]"
              :key="size"
              type="button"
              :class="{ active: taskPage.size === size }"
              @click="emit('change-task-page-size', size)"
            >
              {{ size }}
            </button>
          </div>
        </footer>
      </template>

      <div v-else class="group-tasks-empty">
        <span><ListTodo :size="24" /></span>
        <div>
          <strong>暂无团队任务</strong>
          <small>{{ canCreateTask ? '从一个清晰的协作事项开始' : '负责人创建的任务会显示在这里' }}</small>
        </div>
        <button v-if="canCreateTask" type="button" @click="emit('create-task')">
          <ClipboardPlus :size="14" />
          新建任务
        </button>
      </div>
        </section>
      </div>

      <section v-else id="group-members-panel" key="members" role="tabpanel" class="group-members-section group-workspace-view">
      <header class="group-section-header">
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

      <p v-if="memberRoleError" class="member-role-feedback error" role="alert">{{ memberRoleError }}</p>
      <p v-else-if="memberRoleMessage" class="member-role-feedback success" role="status">
        <Check :size="14" />
        <span>{{ memberRoleMessage }}</span>
      </p>

      <div v-if="members.length" class="group-member-list">
        <article v-for="member in members" :key="member.userId" class="group-member-row">
          <span class="group-member-avatar">{{ groupInitial(member.username) }}</span>
          <div>
            <strong>{{ member.username }}</strong>
            <small>{{ String(member.userId) === String(group.ownerId) ? '工作组创建者' : '工作组成员' }}</small>
          </div>
          <div v-if="group.currentUserRole === 'OWNER' && member.role !== 'OWNER'" class="member-role-control">
            <button
              class="group-member-role editable"
              :class="{ admin: member.role === 'ADMIN', open: String(openRoleMenuUserId) === String(member.userId) }"
              type="button"
              :disabled="String(memberRolePendingUserId) === String(member.userId)"
              :aria-expanded="String(openRoleMenuUserId) === String(member.userId)"
              :aria-label="`修改 ${member.username} 的角色，当前为${groupRoleLabel(member.role)}`"
              @click="toggleRoleMenu(member.userId)"
            >
              <RefreshCw v-if="String(memberRolePendingUserId) === String(member.userId)" class="spin-icon" :size="13" />
              <ShieldCheck v-else-if="member.role === 'ADMIN'" :size="13" />
              <UsersRound v-else :size="13" />
              {{ groupRoleLabel(member.role) }}
              <ChevronDown :size="12" />
            </button>

            <Transition name="property-reveal">
              <div v-if="String(openRoleMenuUserId) === String(member.userId)" class="member-role-menu" role="menu">
                <button
                  type="button"
                  :class="{ active: member.role === 'ADMIN' }"
                  role="menuitem"
                  @click="selectMemberRole(member, 'ADMIN')"
                >
                  <span><ShieldCheck :size="14" /></span>
                  <span><strong>管理员</strong><small>协助管理工作组</small></span>
                  <Check v-if="member.role === 'ADMIN'" :size="13" />
                </button>
                <button
                  type="button"
                  :class="{ active: member.role === 'MEMBER' }"
                  role="menuitem"
                  @click="selectMemberRole(member, 'MEMBER')"
                >
                  <span><UsersRound :size="14" /></span>
                  <span><strong>成员</strong><small>参与工作组协作</small></span>
                  <Check v-if="member.role === 'MEMBER'" :size="13" />
                </button>
              </div>
            </Transition>
          </div>
          <span v-else class="group-member-role" :class="{ owner: member.role === 'OWNER', admin: member.role === 'ADMIN' }">
            <Crown v-if="member.role === 'OWNER'" :size="13" />
            <ShieldCheck v-else-if="member.role === 'ADMIN'" :size="13" />
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
    </Transition>
  </section>
</template>
