<template>
  <div class="profile-edit-container">
    <div class="page-header">
      <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
      <h1>编辑个人资料</h1>
    </div>

    <el-card class="form-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="info">
          <el-form
            :model="userForm"
            :rules="userRules"
            ref="userFormRef"
            label-width="100px"
            class="edit-form"
          >
            <el-form-item label="头像">
              <div class="avatar-upload">
                <el-avatar :size="100" :src="userForm.avatar || '/images/default-avatar.png'" />
                <el-upload
                  action="/api/upload"
                  :before-upload="beforeAvatarUpload"
                  :on-success="handleAvatarSuccess"
                  :show-file-list="false"
                  accept="image/*"
                >
                  <el-button type="primary" link>上传头像</el-button>
                </el-upload>
                <div class="avatar-presets">
                  <span class="preset-label">或选预设：</span>
                  <img
                    v-for="p in presetAvatars"
                    :key="p"
                    :src="p"
                    class="preset-avatar"
                    :class="{ active: userForm.avatar === p }"
                    @click="userForm.avatar = p"
                  />
                </div>
              </div>
            </el-form-item>

            <el-form-item label="用户名" prop="userName">
              <el-input v-model="userForm.userName" disabled />
            </el-form-item>

            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="userForm.realName" disabled placeholder="注册后不可修改" />
            </el-form-item>

            <el-form-item label="邮箱" prop="email">
              <el-input v-model="userForm.email" placeholder="请输入邮箱" />
            </el-form-item>

            <el-form-item label="电话" prop="phone">
              <el-input v-model="userForm.phone" placeholder="请输入电话号码" />
            </el-form-item>

            <el-form-item label="身份证号" prop="identityNum">
              <el-input
                v-model="userForm.identityNum"
                placeholder="办理融资/作为联合贷款人时使用"
                maxlength="18"
              />
            </el-form-item>

            <el-form-item label="所在地区">
              <el-cascader
                v-model="region"
                :options="regionOptions"
                :props="{ expandTrigger: 'hover', value: 'label', label: 'label' }"
                placeholder="请选择省/市/区"
                style="width: 100%"
                clearable
              />
            </el-form-item>

            <el-form-item label="详细地址">
              <el-input
                v-model="addressDetail"
                type="textarea"
                :rows="2"
                placeholder="请输入详细地址（街道、门牌号等）"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="saveUserInfo" :loading="saving">
                保存更改
              </el-button>
              <el-button @click="resetUserForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="修改密码" name="password">
          <el-form
            :model="passwordForm"
            :rules="passwordRules"
            ref="passwordFormRef"
            label-width="100px"
            class="edit-form"
          >
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                placeholder="请输入当前密码"
                show-password
              />
            </el-form-item>

            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="请输入新密码"
                show-password
              />
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="changePassword" :loading="changingPassword">
                修改密码
              </el-button>
              <el-button @click="resetPasswordForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { regionData } from 'element-china-area-data'
import { getUserInfo, updateUserInfo, updatePassword } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('info')
const saving = ref(false)
const changingPassword = ref(false)
const userFormRef = ref()
const passwordFormRef = ref()

// 省市区三级下拉数据
const regionOptions = regionData
// 选中的省/市/区（label 数组）
const region = ref([])
// 详细地址（街道门牌等）
const addressDetail = ref('')

const userForm = reactive({
  userName: '',
  realName: '',
  email: '',
  phone: '',
  identityNum: '',
  address: '',
  avatar: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validatePhone = (rule, value, callback) => {
  if (!value) {
    callback()
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的电话号码'))
  } else {
    callback()
  }
}

const validateEmail = (rule, value, callback) => {
  if (!value) {
    callback()
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
    callback(new Error('请输入正确的邮箱地址'))
  } else {
    callback()
  }
}

const validateIdNum = (rule, value, callback) => {
  if (!value) {
    callback()
  } else if (!/^\d{17}[\dXx]$/.test(value)) {
    callback(new Error('请输入正确的18位身份证号'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const userRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  email: [{ validator: validateEmail, trigger: 'blur' }],
  identityNum: [{ validator: validateIdNum, trigger: 'blur' }]
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

onMounted(() => {
  loadUserInfo()
})

const loadUserInfo = async () => {
  try {
    const res = await getUserInfo()
    if (res.data) {
      Object.assign(userForm, res.data)
      // 把已存的整段地址尽量拆回 省/市/区 + 详细地址，便于三级下拉回显
      const parsed = parseAddress(userForm.address)
      region.value = parsed.region
      addressDetail.value = parsed.detail
    }
  } catch (error) {
    ElMessage.error('加载用户信息失败')
  }
}

/**
 * 把整段地址尽量拆成 [省,市,区] + 详细地址。
 * 匹配不上时 region 为空、detail 取整段，保证旧数据不丢。
 */
const parseAddress = (full) => {
  if (!full || !full.trim()) return { region: [], detail: '' }
  let rest = full.trim()
  const picked = []
  let level = regionOptions
  for (let depth = 0; depth < 3; depth++) {
    const hit = level.find(o => rest.startsWith(o.label))
    if (!hit) break
    picked.push(hit.label)
    rest = rest.slice(hit.label.length).trim()
    level = hit.children || []
  }
  return { region: picked, detail: rest }
}

/** 把 省/市/区 + 详细地址 合成一段存入 userForm.address */
const composeAddress = () => {
  const head = (region.value || []).join('')
  userForm.address = (head + (addressDetail.value || '').trim()).trim()
}

const saveUserInfo = async () => {
  try {
    await userFormRef.value.validate()
    saving.value = true
    composeAddress()
    await updateUserInfo(userForm)
    ElMessage.success('保存成功')
    // 更新store中的用户信息
    userStore.setUserInfo({ ...userStore.userInfo, ...userForm })
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error.message || '保存失败')
    }
  } finally {
    saving.value = false
  }
}

const resetUserForm = () => {
  loadUserInfo()
}

const changePassword = async () => {
  try {
    await passwordFormRef.value.validate()
    changingPassword.value = true
    await updatePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    resetPasswordForm()
    // 跳转到登录页
    setTimeout(() => {
      userStore.logout()
      router.push('/login')
    }, 1500)
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error.message || '修改密码失败')
    }
  } finally {
    changingPassword.value = false
  }
}

const resetPasswordForm = () => {
  passwordFormRef.value?.resetFields()
}

const presetAvatars = [
  '/images/头像1.png',
  '/images/头像2.png',
  '/images/头像3.jpg',
  '/images/(专家)头像4.png'
]
const beforeAvatarUpload = (raw) => {
  const isImg = ['image/jpeg', 'image/png', 'image/jpg', 'image/webp'].includes(raw.type)
  if (!isImg) { ElMessage.error('只能上传图片'); return false }
  if (raw.size / 1024 / 1024 > 5) { ElMessage.error('图片不能超过 5MB'); return false }
  return true
}
const handleAvatarSuccess = (response) => {
  if (response.code === 200) {
    userForm.avatar = response.data.url
    ElMessage.success('头像上传成功，记得点「保存更改」')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.profile-edit-container {
  max-width: 800px;
  margin: 20px auto;
  padding: 0 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0;
  color: #303133;
}

.form-card {
  min-height: 400px;
}

.edit-form {
  max-width: 500px;
  margin-top: 20px;
}

.avatar-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.avatar-presets {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.preset-label {
  font-size: 12px;
  color: #909399;
}

.preset-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.2s;
}

.preset-avatar:hover {
  border-color: #c0c4cc;
}

.preset-avatar.active {
  border-color: #409eff;
}
</style>
