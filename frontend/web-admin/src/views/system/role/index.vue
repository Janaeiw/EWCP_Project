<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import {
  getRolePage,
  createRole,
  updateRole,
  deleteRole,
  getRoleMenuIds,
  saveRoleMenus,
  type RoleItem
} from "@/api/system/role";
import { getMenuTree, type MenuItem } from "@/api/system/menu";
import type { ElTree } from "element-plus";

defineOptions({ name: "SystemRole" });

// ===== 搜索 =====
const searchForm = reactive({
  roleName: "",
  status: undefined as number | undefined
});

// ===== 表格 =====
const loading = ref(false);
const tableData = ref<RoleItem[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getRolePage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      roleName: searchForm.roleName || undefined,
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
  searchForm.roleName = "";
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

// ===== 弹窗表单 =====
const dialogVisible = ref(false);
const dialogTitle = ref("新增角色");
const formRef = ref<FormInstance>();
const form = reactive<Partial<RoleItem>>({
  id: undefined,
  roleName: "",
  roleKey: "",
  status: 1,
  remark: ""
});

const rules: FormRules = {
  roleName: [{ required: true, message: "请输入角色名称", trigger: "blur" }],
  roleKey: [{ required: true, message: "请输入角色标识", trigger: "blur" }]
};

const handleAdd = () => {
  dialogTitle.value = "新增角色";
  Object.assign(form, {
    id: undefined,
    roleName: "",
    roleKey: "",
    status: 1,
    remark: ""
  });
  dialogVisible.value = true;
};

const handleEdit = (row: RoleItem) => {
  dialogTitle.value = "编辑角色";
  Object.assign(form, {
    id: row.id,
    roleName: row.roleName,
    roleKey: row.roleKey,
    status: row.status,
    remark: row.remark || ""
  });
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  await formRef.value?.validate();
  try {
    const api = form.id ? updateRole : createRole;
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

const handleDelete = (row: RoleItem) => {
  ElMessageBox.confirm("确定要删除该角色吗？", "提示", {
    type: "warning"
  }).then(async () => {
    const res = await deleteRole(row.id);
    if (res.code === 0) {
      ElMessage.success("删除成功");
      fetchData();
    } else {
      ElMessage.error(res.msg);
    }
  });
};

// ===== 权限分配弹窗 =====
const permDialogVisible = ref(false);
const permDialogTitle = ref("分配权限");
const permTreeData = ref<MenuItem[]>([]);
const permCheckedKeys = ref<number[]>([]);
const permLoading = ref(false);
const permSaving = ref(false);
const currentRoleId = ref<number | null>(null);
const permTreeRef = ref<InstanceType<typeof ElTree> | null>(null);

const permTreeProps = {
  label: "title",
  children: "children"
};

const handlePerm = async (row: RoleItem) => {
  currentRoleId.value = row.id;
  permDialogTitle.value = `分配权限 - ${row.roleName}`;
  permDialogVisible.value = true;
  permLoading.value = true;
  try {
    const [treeRes, idsRes] = await Promise.all([
      getMenuTree(),
      getRoleMenuIds(row.id)
    ]);
    if (treeRes.code === 0) {
      permTreeData.value = treeRes.data;
    }
    if (idsRes.code === 0) {
      permCheckedKeys.value = idsRes.data ?? [];
    }
  } finally {
    permLoading.value = false;
  }
};

const handlePermSave = async () => {
  if (currentRoleId.value === null) return;
  const tree = permTreeRef.value;
  if (!tree) return;
  // 合并已勾选和半选中的节点（半选 = 父节点部分子节点被勾选）
  const checkedKeys = tree.getCheckedKeys() as number[];
  const halfCheckedKeys = tree.getHalfCheckedKeys() as number[];
  const allKeys = [...new Set([...checkedKeys, ...halfCheckedKeys])];
  permSaving.value = true;
  try {
    const res = await saveRoleMenus(currentRoleId.value, allKeys);
    if (res.code === 0) {
      ElMessage.success("权限保存成功");
      permDialogVisible.value = false;
    } else {
      ElMessage.error(res.msg);
    }
  } finally {
    permSaving.value = false;
  }
};

onMounted(fetchData);
</script>

<template>
  <div>
    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb-4">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="角色名称">
          <el-input
            v-model="searchForm.roleName"
            class="w-[180px]!"
            placeholder="请输入角色名称"
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
          <span class="font-medium">角色管理</span>
          <el-button type="primary" @click="handleAdd">新增角色</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" stripe border>
        <el-table-column type="index" label="编号" width="70" align="center" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="roleKey" label="角色标识" min-width="140" />
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
        <el-table-column prop="remark" label="备注" min-width="200" />
        <el-table-column
          prop="createTime"
          label="创建时间"
          min-width="160"
          align="center"
        />
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handlePerm(row)">
              权限
            </el-button>
            <el-button link type="primary" @click="handleEdit(row)">
              编辑
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
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色标识" prop="roleKey">
          <el-input v-model="form.roleKey" placeholder="如 admin、editor" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 权限分配弹窗 -->
    <el-dialog
      v-model="permDialogVisible"
      :title="permDialogTitle"
      width="600px"
      destroy-on-close
    >
      <div v-loading="permLoading" class="min-h-[300px]">
        <el-tree
          v-if="permTreeData.length"
          ref="permTreeRef"
          :data="permTreeData"
          :props="permTreeProps"
          :default-checked-keys="permCheckedKeys"
          node-key="id"
          show-checkbox
          default-expand-all
        />
        <el-empty
          v-if="!permLoading && !permTreeData.length"
          description="暂无菜单数据"
        />
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="permSaving" @click="handlePermSave">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
