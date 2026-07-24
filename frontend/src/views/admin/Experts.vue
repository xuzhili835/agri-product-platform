<template>
  <div class="admin-experts-page">
    <div class="page-header">
      <h2>专家管理</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增专家
      </el-button>
    </div>

    <el-card>
      <el-table :data="expertList" v-loading="loading" border>
        <el-table-column prop="userName" label="用户名" width="130" />
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column prop="profession" label="专业领域" width="140" />
        <el-table-column prop="position" label="职位" width="120" />
        <el-table-column prop="belong" label="所属单位" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑专家' : '新增专家'"
      width="560px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="userName">
          <el-input v-model="form.userName" :disabled="isEditing" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="登录密码" prop="password" v-if="!isEditing">
          <el-input v-model="form.password" placeholder="默认 123456" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="专家真实姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="专业领域" prop="profession">
          <el-input v-model="form.profession" placeholder="如：农学、植保" />
        </el-form-item>
        <el-form-item label="职位" prop="position">
          <el-input v-model="form.position" placeholder="如：教授" />
        </el-form-item>
        <el-form-item label="所属单位" prop="belong">
          <el-input v-model="form.belong" placeholder="如：山东省农科院" />
        </el-form-item>
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
import { Plus } from '@element-plus/icons-vue'
import {
  getAdminExperts,
  createAdminExpert,
  updateAdminExpert,
  deleteAdminExpert
} from '@/api/admin'

const loading = ref(false)
const submitting = ref(false)
const expertList = ref([])
const dialogVisible = ref(false)
const isEditing = ref(false)
const formRef = ref(null)

const form = reactive({
  userName: '',
  password: '',
  realName: '',
  phone: '',
  profession: '',
  position: '',
  belong: ''
})

const rules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  profession: [{ required: true, message: '请输入专业领域', trigger: 'blur' }]
}

const loadExperts = async () => {
  loading.value = true
  try {
    const res = await getAdminExperts()
    expertList.value = res.data || []
  } catch (error) {
    ElMessage.error('加载专家列表失败')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.userName = ''
  form.password = ''
  form.realName = ''
  form.phone = ''
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
  form.realName = row.realName
  form.phone = row.phone
  form.profession = row.profession
  form.position = row.position
  form.belong = row.belong
  dialogVisible.value = true
}

const submitForm = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true
    if (isEditing.value) {
      await updateAdminExpert(form.userName, {
        realName: form.realName,
        phone: form.phone,
        profession: form.profession,
        position: form.position,
        belong: form.belong
      })
      ElMessage.success('修改成功')
    } else {
      await createAdminExpert({
        userName: form.userName,
        password: form.password,
        realName: form.realName,
        phone: form.phone,
        profession: form.profession,
        position: form.position,
        belong: form.belong
      })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadExperts()
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除专家「${row.realName || row.userName}」吗？将同时删除其账号。`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteAdminExpert(row.userName)
    ElMessage.success('删除成功')
    loadExperts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadExperts()
})
</script>

<style scoped>
.admin-experts-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #303133;
}
</style>
