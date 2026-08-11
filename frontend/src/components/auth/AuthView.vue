<script setup>
defineProps({
  isLogin: Boolean,
  isSubmitting: Boolean,
  isValid: Boolean,
  errorMessage: {
    type: String,
    default: ''
  },
  successMessage: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['mode-change', 'submit'])
const account = defineModel('account', { type: String, default: '' })
const username = defineModel('username', { type: String, default: '' })
const email = defineModel('email', { type: String, default: '' })
const password = defineModel('password', { type: String, default: '' })
</script>

<template>
  <main class="auth-shell">
    <section class="auth-card">
      <div class="auth-visual">
        <div class="brand-lockup">
          <span>AT</span>
          <p class="brand">AI Todo</p>
        </div>
        <div class="auth-visual-copy">
          <p>PERSONAL FOCUS SYSTEM</p>
          <h1>让今天的重点<br />清晰可见</h1>
          <span>安静地收拢任务，把注意力留给真正重要的事。</span>
        </div>
        <div class="preview-list">
          <div class="preview-item strong">
            <span></span>
            <p>完成 Spring Boot 任务接口联调</p>
          </div>
          <div class="preview-item">
            <span></span>
            <p>整理明天的学习计划</p>
          </div>
          <div class="preview-item">
            <span></span>
            <p>记录一个 AI 功能想法</p>
          </div>
        </div>
      </div>

      <form class="auth-form" @submit.prevent="emit('submit')">
        <div class="auth-tabs" role="tablist">
          <button type="button" :class="{ active: isLogin }" @click="emit('mode-change', 'login')">
            登录
          </button>
          <button type="button" :class="{ active: !isLogin }" @click="emit('mode-change', 'register')">
            注册
          </button>
        </div>

        <div class="form-heading">
          <h2>{{ isLogin ? '欢迎回来' : '创建账户' }}</h2>
          <p>{{ isLogin ? '登录后查看和创建你的任务。' : '注册成功后再使用账号登录。' }}</p>
        </div>

        <label v-if="isLogin" class="field">
          <span>账号</span>
          <input v-model="account" type="text" autocomplete="username" placeholder="用户名或邮箱" />
        </label>

        <template v-else>
          <label class="field">
            <span>用户名</span>
            <input v-model="username" type="text" autocomplete="username" placeholder="3 到 20 个字符" />
          </label>

          <label class="field">
            <span>邮箱</span>
            <input v-model="email" type="email" autocomplete="email" placeholder="name@example.com" />
          </label>
        </template>

        <label class="field">
          <span>密码</span>
          <input
            v-model="password"
            :autocomplete="isLogin ? 'current-password' : 'new-password'"
            type="password"
            placeholder="至少 6 个字符"
          />
        </label>

        <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>
        <p v-if="successMessage" class="notice success">{{ successMessage }}</p>

        <button class="primary-button" type="submit" :disabled="isSubmitting || !isValid">
          {{ isSubmitting ? '提交中...' : isLogin ? '登录' : '注册' }}
        </button>
      </form>
    </section>
  </main>
</template>
