<script setup>
import {
  Crown,
  FolderPlus,
  Inbox,
  LogOut,
  Plus,
  RefreshCw,
  UserRound,
  UsersRound,
  X
} from '@lucide/vue'
import { groupInitial } from '../../utils/groups'

defineProps({
  open: Boolean,
  user: {
    type: Object,
    required: true
  },
  views: {
    type: Array,
    default: () => []
  },
  activeView: {
    type: String,
    default: 'all'
  },
  selectedGroup: {
    type: Object,
    default: null
  },
  groups: {
    type: Array,
    default: () => []
  },
  isGroupListLoading: Boolean,
  groupListError: {
    type: String,
    default: ''
  },
  pendingInvitationCount: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits([
  'close',
  'select-view',
  'select-group',
  'open-invitations',
  'create-group',
  'retry-groups',
  'logout'
])
</script>

<template>
  <button
    v-if="open"
    class="sidebar-backdrop"
    type="button"
    aria-label="关闭导航"
    @click="emit('close')"
  ></button>

  <aside class="sidebar" :class="{ open }">
    <div class="sidebar-brand">
      <span>AT</span>
      <strong>AI Todo</strong>
      <button class="mobile-sidebar-close" type="button" aria-label="关闭导航" @click="emit('close')">
        <X :size="18" />
      </button>
    </div>

    <div class="account-box">
      <div class="avatar">
        <UserRound :size="20" />
      </div>
      <div class="account-copy">
        <strong>{{ user.username }}</strong>
        <span>{{ user.email }}</span>
      </div>
    </div>

    <div class="sidebar-scroll-area">
      <nav class="nav-list" aria-label="任务视图">
        <button
          v-for="view in views"
          :key="view.key"
          type="button"
          :class="{ active: !selectedGroup && activeView === view.key }"
          :aria-current="!selectedGroup && activeView === view.key ? 'page' : undefined"
          @click="emit('select-view', view.key)"
        >
          <component :is="view.icon" :size="18" />
          <span>{{ view.label }}</span>
          <em>{{ view.count }}</em>
        </button>
      </nav>

      <section class="sidebar-groups" aria-labelledby="sidebar-groups-title">
        <header class="sidebar-group-heading">
          <span id="sidebar-groups-title">工作组</span>
          <div class="sidebar-group-actions">
            <button
              class="sidebar-group-invitation-button"
              :class="{ attention: pendingInvitationCount > 0 }"
              type="button"
              :aria-label="pendingInvitationCount ? `${pendingInvitationCount} 个待处理工作组邀请` : '查看工作组邀请'"
              title="协作邀请"
              @click="emit('open-invitations')"
            >
              <Inbox :size="15" />
              <b v-if="pendingInvitationCount">{{ pendingInvitationCount > 99 ? '99+' : pendingInvitationCount }}</b>
            </button>
            <button type="button" aria-label="创建工作组" title="创建工作组" @click="emit('create-group')">
              <FolderPlus :size="15" />
            </button>
          </div>
        </header>

        <div v-if="isGroupListLoading" class="group-nav-loading" aria-label="正在加载工作组">
          <span></span>
          <span></span>
        </div>

        <div v-else-if="groups.length" class="group-nav-list">
          <button
            v-for="group in groups"
            :key="group.id"
            type="button"
            class="group-nav-item"
            :class="{ active: String(selectedGroup?.id) === String(group.id) }"
            :aria-current="String(selectedGroup?.id) === String(group.id) ? 'page' : undefined"
            :title="group.name"
            @click="emit('select-group', group)"
          >
            <span class="group-nav-avatar">{{ groupInitial(group.name) }}</span>
            <span class="group-nav-copy">{{ group.name }}</span>
            <Crown v-if="group.currentUserRole === 'OWNER'" class="group-nav-role" :size="13" />
            <UsersRound v-else class="group-nav-role" :size="13" />
          </button>
        </div>

        <button v-else-if="!groupListError" class="group-nav-empty" type="button" @click="emit('create-group')">
          <Plus :size="14" />
          <span>创建工作组</span>
        </button>

        <div v-if="groupListError" class="group-nav-error">
          <span>{{ groupListError }}</span>
          <button type="button" aria-label="重新加载工作组" title="重试" @click="emit('retry-groups')">
            <RefreshCw :size="13" />
          </button>
        </div>
      </section>
    </div>

    <div class="sidebar-footer">
      <button class="ghost-button" type="button" @click="emit('logout')">
        <LogOut :size="17" />
        <span>退出登录</span>
      </button>
    </div>
  </aside>
</template>
