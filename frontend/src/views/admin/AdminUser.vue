<template>
  <div class="admin-user">
    <div class="header">
      <h2>用户管理</h2>
      <div class="header-tools">
        <el-select v-model="filterRole" placeholder="全部角色" clearable style="width: 140px" @change="loadUsers">
          <el-option label="管理员" value="admin" />
          <el-option label="农户" value="farmer" />
          <el-option label="买家" value="buyer" />
          <el-option label="专家" value="expert" />
          <el-option label="银行" value="bank" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width: 130px" @change="loadUsers">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户名/姓名/手机号"
          style="width: 260px"
          clearable
          @clear="loadUsers"
          @keyup.enter="loadUsers"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增用户
        </el-button>
      </div>
    </div>

    <el-table :data="users" style="width: 100%" v-loading="loading" border>
      <el-table-column prop="userName" label="用户名" width="130" />
      <el-table-column prop="realName" label="真实姓名" width="110" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="role" label="角色" width="90">
        <template #default="{ row }">
          <el-tag :type="getRoleType(row.role)">{{ getRoleLabel(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="信用分" width="90">
        <template #default="{ row }">
          <span v-if="row.role === 'farmer'">{{ row.credit ?? 5 }}</span>
          <span v-else style="color: #c0c4cc">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="170" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEditing ? '编辑用户' : '新增用户'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="userName">
          <el-input v-model="form.userName" :disabled="isEditing" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="密码" :prop="isEditing ? '' : 'password'">
          <el-input v-model="form.password" :placeholder="isEditing ? '留空则不修改' : '默认 123456'" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="管理员" value="admin" />
            <el-option label="农户" value="farmer" />
            <el-option label="买家" value="buyer" />
            <el-option label="专家" value="expert" />
            <el-option label="银行" value="bank" />
          </el-select>
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="form.identityNum" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="信用分" v-if="form.role === 'farmer'">
          <el-input-number v-model="form.credit" :min="0" :max="5" :step="1" />
          <span class="unit-text">0-5 分（影响农户的融资智能匹配）</span>
        </el-form-item>
        <el-form-item label="状态" v-if="isEditing">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="form.role === 'expert'">
          <el-form-item label="专业">
            <el-input v-model="form.profession" placeholder="如：农学、植保" />
          </el-form-item>
          <el-form-item label="职位">
            <el-input v-model="form.position" placeholder="如：教授" />
          </el-form-item>
          <el-form-item label="所属单位">
            <el-input v-model="form.belong" placeholder="如：山东省农科院" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { getUsers, createUser, updateUser, deleteUser, updateUserStatus } from '@/api/admin'

const loading = ref(false)
const submitting = ref(false)
const users = ref([])
const searchKeyword = ref('')
const filterRole = ref('')
const filterStatus = ref(null)
const dialogVisible = ref(false)
const isEditing = ref(false)
const formRef = ref(null)

const form = reactive({
  userName: '',
  password: '',
  realName: '',
  phone: '',
  identityNum: '',
  address: '',
  role: 'farmer',
  status: 1,
  credit: 5,
  profession: '',
  position: '',
  belong: ''
})

const rules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

onMounted(() => {
  loadUsers()
})

const loadUsers = async () => {
  loading.value = true
  try {
    const params = {}
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterRole.value) params.role = filterRole.value
    if (filterStatus.value !== null && filterStatus.value !== '' && filterStatus.value !== undefined) {
      params.status = filterStatus.value
    }
    const res = await getUsers(params)
    users.value = res.data || []
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const getRoleType = (role) => {
  const types = { admin: 'danger', farmer: 'success', buyer: 'primary', expert: 'warning', bank: 'info' }
  return types[role] || ''
}

const getRoleLabel = (role) => {
  const labels = { admin: '管理员', farmer: '农户', buyer: '买家', expert: '专家', bank: '银行' }
  return labels[role] || role
}

const resetForm = () => {
  form.userName = ''
  form.password = ''
  form.realName = ''
  form.phone = ''
  form.identityNum = ''
  form.address = ''
  form.role = 'farmer'
  form.status = 1
  form.credit = 5
  form.profession = ''
  form.position = ''
  form.belong = ''
}

const handleAdd = () => {
  isEditing.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEditing.value = true
  form.userName = row.userName
  form.password = ''
  form.realName = row.realName || ''
  form.phone = row.phone || ''
  form.identityNum = row.identityNum || ''
  form.address = row.address || ''
  form.role = row.role
  form.status = row.status
  form.credit = row.credit ?? 5
  form.profession = row.profession || ''
  form.position = row.position || ''
  form.belong = row.belong || ''
  dialogVisible.value = true
}

const submitForm = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true
    if (isEditing.value) {
      const data = {
        realName: form.realName,
        phone: form.phone,
        identityNum: form.identityNum,
        address: form.address,
        role: form.role,
        status: form.status,
        credit: form.credit,
        profession: form.profession,
        position: form.position,
        belong: form.belong
      }
      if (form.password) data.password = form.password
      await updateUser(form.userName, data)
      ElMessage.success('修改成功')
    } else {
      await createUser({
        userName: form.userName,
        password: form.password,
        realName: form.realName,
        phone: form.phone,
        identityNum: form.identityNum,
        address: form.address,
        role: form.role,
        profession: form.profession,
        position: form.position,
        belong: form.belong
      })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadUsers()
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

const toggleStatus = async (user) => {
  try {
    const newStatus = user.status === 1 ? 0 : 1
    const action = newStatus === 1 ? '启用' : '禁用'
    await ElMessageBox.confirm(`确定${action}用户 ${user.userName} 吗？`, '提示', { type: 'warning' })
    await updateUserStatus(user.userName, { status: newStatus })
    ElMessage.success(`${action}成功`)
    await loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handleDelete = async (user) => {
  try {
    await ElMessageBox.confirm(`确定删除用户 ${user.userName}（${getRoleLabel(user.role)}）吗？此操作不可恢复。`, '危险操作', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'error'
    })
    await deleteUser(user.userName)
    ElMessage.success('删除成功')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}
</script>

<style scoped>
.admin-user {
  max-width: 1200px;
  margin: 20px auto;
  padding: 0 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.header-tools {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.unit-text {
  margin-left: 8px;
  color: #909399;
  font-size: 13px;
}
</style>
