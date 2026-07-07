<script setup>
import { computed, reactive, ref } from 'vue'
import { registerUser } from './services/api'

const form = reactive({
  username: '',
  email: '',
  password: ''
})

const registeredUser = ref(null)
const errorMessage = ref('')
const isSubmitting = ref(false)

const usernameLength = computed(() => form.username.trim().length)
const passwordLength = computed(() => form.password.length)

const isFormValid = computed(() => {
  return (
    usernameLength.value >= 3 &&
    usernameLength.value <= 20 &&
    form.email.includes('@') &&
    passwordLength.value >= 6 &&
    passwordLength.value <= 50
  )
})

async function handleRegister() {
  errorMessage.value = ''
  registeredUser.value = null

  if (!isFormValid.value) {
    errorMessage.value = '请按后端校验规则填写用户名、邮箱和密码。'
    return
  }

  isSubmitting.value = true

  try {
    registeredUser.value = await registerUser({
      username: form.username.trim(),
      email: form.email.trim(),
      password: form.password
    })

    form.password = ''
  } catch (error) {
    errorMessage.value = error.message || '注册失败，请稍后重试。'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="app-shell">
    <section class="workspace">
      <div class="intro-panel">
        <p class="eyebrow">AI Todo</p>
        <h1>账户注册</h1>
        <p class="intro-text">
          当前前端已接入后端已有的用户注册接口，后续登录和待办功能可以继续按同样的 API 封装方式扩展。
        </p>

        <div class="api-list">
          <div class="api-item active">
            <span class="method">POST</span>
            <span>/api/users/register</span>
          </div>
          <div class="api-item muted">
            <span class="method">TODO</span>
            <span>登录接口待后端实现</span>
          </div>
          <div class="api-item muted">
            <span class="method">TODO</span>
            <span>待办接口待后端实现</span>
          </div>
        </div>
      </div>

      <form class="register-panel" @submit.prevent="handleRegister">
        <div class="panel-heading">
          <h2>创建用户</h2>
          <p>字段与后端 `UserRegisterRequest` 保持一致。</p>
        </div>

        <label class="field">
          <span>用户名</span>
          <input
            v-model="form.username"
            type="text"
            name="username"
            autocomplete="username"
            placeholder="3 到 20 个字符"
          />
          <small :class="{ ok: usernameLength >= 3 && usernameLength <= 20 }">
            当前 {{ usernameLength }} 个字符
          </small>
        </label>

        <label class="field">
          <span>邮箱</span>
          <input
            v-model="form.email"
            type="email"
            name="email"
            autocomplete="email"
            placeholder="name@example.com"
          />
        </label>

        <label class="field">
          <span>密码</span>
          <input
            v-model="form.password"
            type="password"
            name="password"
            autocomplete="new-password"
            placeholder="6 到 50 个字符"
          />
          <small :class="{ ok: passwordLength >= 6 && passwordLength <= 50 }">
            当前 {{ passwordLength }} 个字符
          </small>
        </label>

        <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>

        <button class="primary-button" type="submit" :disabled="isSubmitting || !isFormValid">
          {{ isSubmitting ? '提交中...' : '注册' }}
        </button>
      </form>

      <aside class="result-panel">
        <h2>注册结果</h2>

        <div v-if="registeredUser" class="user-result">
          <dl>
            <div>
              <dt>ID</dt>
              <dd>{{ registeredUser.id }}</dd>
            </div>
            <div>
              <dt>用户名</dt>
              <dd>{{ registeredUser.username }}</dd>
            </div>
            <div>
              <dt>邮箱</dt>
              <dd>{{ registeredUser.email }}</dd>
            </div>
            <div>
              <dt>状态</dt>
              <dd>{{ registeredUser.status }}</dd>
            </div>
            <div>
              <dt>创建时间</dt>
              <dd>{{ registeredUser.createdAt }}</dd>
            </div>
          </dl>
        </div>

        <p v-else class="empty-state">提交注册后，这里会展示后端返回的用户信息。</p>
      </aside>
    </section>
  </main>
</template>
