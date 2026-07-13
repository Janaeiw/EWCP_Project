<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import {
  getUserPage,
  createUser,
  updateUser,
  deleteUser,
  resetPassword,
  type UserItem
} from "@/api/system/user";
import { getRoleList, type RoleItem } from "@/api/system/role";
import { isValidUrl, isValidEmail, isValidPhone } from "@/utils/validate";

defineOptions({ name: "SystemUser" });

// ===== 搜索 =====
const searchForm = reactive({
  username: "",
  nickname: "",
  status: undefined as number | undefined
});

// ===== 表格 =====
const loading = ref(false);
const tableData = ref<UserItem[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getUserPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      username: searchForm.username || undefined,
      nickname: searchForm.nickname || undefined,
      status: searchForm.status
    });
    if (res.code === 0) {
      tableData.value = res.data.records;
      total.value = res.data.total;
    }
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pageNum.value = 1;
  fetchData();
};

const handleReset = () => {
  searchForm.username = "";
  searchForm.nickname = "";
  searchForm.status = undefined;
  handleSearch();
};

const handleSizeChange = (val: number) => {
  pageSize.value = val;
  fetchData();
};

const handleCurrentChange = (val: number) => {
  pageNum.value = val;
  fetchData();
};

// ===== 角色列表 =====
const allRoles = ref<RoleItem[]>([]);
const fetchRoles = async () => {
  const res = await getRoleList();
  if (res.code === 0) {
    allRoles.value = res.data;
  }
};

// ===== 弹窗表单 =====
const dialogVisible = ref(false);
const dialogTitle = ref("新增用户");
const formRef = ref<FormInstance>();
const form = reactive<Partial<UserItem>>({
  id: undefined,
  username: "",
  password: "",
  nickname: "",
  avatar: "",
  email: "",
  phone: "",
  status: 1,
  roleIds: []
});

const rules: FormRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [],
  nickname: [{ required: true, message: "请输入昵称", trigger: "blur" }],
  roleIds: [{ required: true, message: "请选择角色", trigger: "blur" }],
  avatar: [
    {
      validator: (_rule, value: string, callback) => {
        if (value && !isValidUrl(value)) {
          callback(new Error("请输入有效的链接地址"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ],
  email: [
    {
      validator: (_rule, value: string, callback) => {
        if (value && !isValidEmail(value)) {
          callback(new Error("邮箱格式不正确"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ],
  phone: [
    {
      validator: (_rule, value: string, callback) => {
        if (value && !isValidPhone(value)) {
          callback(new Error("手机号格式不正确"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ]
};

const handleAdd = () => {
  dialogTitle.value = "新增用户";
  Object.assign(form, {
    id: undefined,
    username: "",
    password: "",
    nickname: "",
    avatar: "",
    email: "",
    phone: "",
    status: 1,
    roleIds: []
  });
  dialogVisible.value = true;
};

const handleEdit = (row: UserItem) => {
  dialogTitle.value = "编辑用户";
  Object.assign(form, {
    id: row.id,
    username: row.username,
    password: "",
    nickname: row.nickname,
    avatar: row.avatar || "",
    email: row.email || "",
    phone: row.phone || "",
    status: row.status,
    roleIds: row.roleIds ? [...row.roleIds] : []
  });
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  await formRef.value?.validate();
  try {
    const api = form.id ? updateUser : createUser;
    const res = await api(form);
    if (res.code === 0) {
      ElMessage.success(form.id ? "修改成功" : "新增成功");
      dialogVisible.value = false;
      fetchData();
    } else {
      ElMessage.error(res.msg);
    }
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : "操作失败");
  }
};

const handleDelete = (row: UserItem) => {
  ElMessageBox.confirm("确定要删除该用户吗？", "提示", {
    type: "warning"
  }).then(async () => {
    const res = await deleteUser(row.id);
    if (res.code === 0) {
      ElMessage.success("删除成功");
      fetchData();
    } else {
      ElMessage.error(res.msg);
    }
  });
};

const handleResetPwd = (row: UserItem) => {
  ElMessageBox.confirm("确定要重置该用户密码为 Password123 吗？", "提示", {
    type: "warning"
  }).then(async () => {
    const res = await resetPassword(row.id);
    if (res.code === 0) {
      ElMessage.success("密码重置成功");
    } else {
      ElMessage.error(res.msg);
    }
  });
};

onMounted(() => {
  fetchData();
  fetchRoles();
});
</script>

<template>
  <div>
    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb-4">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户名">
          <el-input
            v-model="searchForm.username"
            class="w-[180px]!"
            placeholder="请输入用户名"
            clearable
          />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input
            v-model="searchForm.nickname"
            class="w-[180px]!"
            placeholder="请输入昵称"
            clearable
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            class="w-[180px]!"
            placeholder="全部"
            clearable
          >
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-medium">用户管理</span>
          <el-button type="primary" @click="handleAdd">新增用户</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" stripe border>
        <el-table-column type="index" label="编号" width="70" align="center" />
        <el-table-column prop="avatar" label="头像" width="70" align="center">
          <template #default="{ row }">
            <div class="flex justify-center">
              <el-avatar v-if="row.avatar" :size="32" :src="row.avatar" />
              <el-avatar v-else :size="32">{{
                row.nickname?.charAt(0)
              }}</el-avatar>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="roleNames" label="角色" min-width="120">
          <template #default="{ row }">
            <el-tag
              v-for="role in row.roleNames"
              :key="role"
              size="small"
              class="mr-1"
            >
              {{ role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : 'danger'"
              size="small"
            >
              {{ row.status === 1 ? "正常" : "禁用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="创建时间"
          min-width="160"
          align="center"
        />
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button link type="warning" @click="handleResetPwd(row)">
              重置密码
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码，密码为空时默认 Password123"
          />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select
            v-model="form.roleIds"
            multiple
            placeholder="请选择角色"
            class="w-full"
          >
            <el-option
              v-for="role in allRoles"
              :key="role.id"
              :label="role.roleName"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="头像" prop="avatar">
          <el-input v-model="form.avatar" placeholder="请输入头像" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
