<template>
  <div class="address-container">
    <div class="page-header">
      <el-button @click="goBack" :icon="ArrowLeft" class="back-btn">返回</el-button>
      <h2>地址管理</h2>
      <div class="header-spacer"></div>
      <el-button type="primary" @click="showAddDialog">
        <el-icon><Plus /></el-icon>
        添加新地址
      </el-button>
    </div>

    <div class="address-list" v-if="addresses.length > 0">
      <el-card v-for="address in addresses" :key="address.id" class="address-card">
        <div class="address-content">
          <div class="address-info">
            <div class="name-phone">
              <span class="name">{{ address.consignee }}</span>
              <span class="phone">{{ address.phone }}</span>
              <el-tag v-if="address.isDefault === 1" type="success" size="small">默认</el-tag>
            </div>
            <div class="detail">
              {{ address.province }} {{ address.city }} {{ address.area }} {{ address.addressDetail }}
            </div>
          </div>
          <div class="address-actions">
            <el-button link type="primary" @click="editAddress(address)">编辑</el-button>
            <el-button link type="danger" @click="deleteAddress(address)">删除</el-button>
            <el-button
              v-if="address.isDefault !== 1"
              link
              type="warning"
              @click="setDefault(address)"
            >
              设为默认
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <el-empty v-else description="暂无地址，请添加" />

    <!-- 添加/编辑地址对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingAddress ? '编辑地址' : '添加地址'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号码" />
        </el-form-item>
        <el-form-item label="地区" prop="region">
          <el-cascader
            v-model="region"
            :options="regionOptions"
            :props="{ expandTrigger: 'hover', value: 'label', label: 'label' }"
            placeholder="请选择省/市/区"
            @change="handleRegionChange"
            style="width: 100%"
            clearable
          />
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input
            v-model="form.detail"
            type="textarea"
            :rows="3"
            placeholder="请输入详细地址，如街道、楼栋号、门牌号等"
          />
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="form.isDefault" />
          <span class="tip">设为默认地址后，下单时将自动选择此地址</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ArrowLeft } from '@element-plus/icons-vue'
import {
  getAddressList,
  addAddress,
  updateAddress,
  deleteAddress as deleteAddr,
  setDefaultAddress
} from '@/api/address'
import { regionData } from 'element-china-area-data'

const router = useRouter()
const addresses = ref([])
const dialogVisible = ref(false)
const editingAddress = ref(null)
const saving = ref(false)
const formRef = ref()
const region = ref([])

const form = reactive({
  realName: '',
  phone: '',
  province: '',
  city: '',
  area: '',
  detail: '',
  isDefault: false
})

const validatePhone = (rule, value, callback) => {
  if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的手机号码'))
  } else {
    callback()
  }
}

const validateRegion = (rule, value, callback) => {
  if (!region.value || region.value.length === 0) {
    callback(new Error('请选择地区'))
  } else {
    callback()
  }
}

const rules = {
  realName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { validator: validatePhone, trigger: 'blur' }
  ],
  region: [{ required: true, validator: validateRegion, trigger: 'change' }],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

// 中国省市区数据（完整版）
const regionOptions = regionData

onMounted(() => {
  loadAddresses()
})

const loadAddresses = async () => {
  try {
    const res = await getAddressList()
    addresses.value = res.data || []
  } catch (error) {
    ElMessage.error('加载地址失败')
  }
}

const showAddDialog = () => {
  editingAddress.value = null
  Object.assign(form, {
    realName: '',
    phone: '',
    province: '',
    city: '',
    area: '',
    detail: '',
    isDefault: false
  })
  region.value = []
  dialogVisible.value = true
}

const editAddress = (address) => {
  editingAddress.value = address
  Object.assign(form, {
    realName: address.consignee,
    phone: address.phone,
    province: address.province,
    city: address.city,
    area: address.area,
    detail: address.addressDetail,
    isDefault: address.isDefault === 1
  })
  region.value = [address.province, address.city, address.area]
  dialogVisible.value = true
}

const handleRegionChange = (value) => {
  if (value && value.length === 3) {
    form.province = value[0]
    form.city = value[1]
    form.area = value[2]
  }
}

const save = async () => {
  try {
    await formRef.value.validate()
    saving.value = true
    if (editingAddress.value) {
      await updateAddress(editingAddress.value.id, form)
      ElMessage.success('更新成功')
    } else {
      await addAddress(form)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await loadAddresses()
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error.message || '保存失败')
    }
  } finally {
    saving.value = false
  }
}

const deleteAddress = async (address) => {
  try {
    await ElMessageBox.confirm('确定删除此地址吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    await deleteAddr(address.id)
    ElMessage.success('删除成功')
    await loadAddresses()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const setDefault = async (address) => {
  try {
    await setDefaultAddress(address.id)
    ElMessage.success('设置成功')
    await loadAddresses()
  } catch (error) {
    ElMessage.error('设置失败')
  }
}

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.address-container {
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

.page-header h2 {
  margin: 0;
  color: #303133;
}

.header-spacer {
  flex: 1;
}

.back-btn {
  flex-shrink: 0;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
  color: #303133;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.address-card {
  margin-bottom: 0;
}

.address-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.address-info {
  flex: 1;
}

.name-phone {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 10px;
}

.name {
  font-weight: bold;
  font-size: 16px;
  color: #303133;
}

.phone {
  color: #606266;
}

.detail {
  color: #909399;
  line-height: 1.5;
}

.address-actions {
  display: flex;
  gap: 10px;
  padding-left: 20px;
  border-left: 1px solid #f0f0f0;
}

.tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}
</style>
