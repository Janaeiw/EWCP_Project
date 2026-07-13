<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import {
  getPermissionTree,
  createPermission,
  updatePermission,
  deletePermission,
  type PermissionItem
} from "@/api/system/permission";

defineOptions({ name: "SystemPermission" });

// ===== 表格数据 =====
const loading = ref(false);
const tableData = ref<PermissionItem[]>([]);

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getPermissionTree();
    if (res.code === 0) {
      tableData.value = res.data;
    }
  } finally {
    loading.value = false;
  }
};

// ===== 弹窗表单 =====
const dialogVisible = ref(false);
const dialogTitle = ref("新增权限");
const formRef = ref<FormInstance>();
const form = reactive<Partial<PermissionItem>>({
  id: undefined,
  parentId: 0,
  name: "",
  permissionKey: "",
  type: "menu",
  sort: 0,
  status: 1,
  remark: ""
});

const rules: FormRules = {
  name: [{ required: true, message: "请输入权限名称", trigger: "blur" }],
  permissionKey: [
    { required: true, message: "请输入权限标识", trigger: "blur" }
  ],
  type: [{ required: true, message: "请选择类型", trigger: "change" }]
};

// 父级权限选择列表（扁平化，排除自身及子节点）
const flattenPermissions = (list: PermissionItem[], excludeId?: number) => {
  const result: { id: number; name: string; level: number }[] = [];
  const walk = (items: PermissionItem[], level: number) => {
    for (const item of items) {
      if (item.id === excludeId) continue;
      result.push({ id: item.id, name: item.name, level });
      if (item.children?.length) {
        walk(item.children, level + 1);
      }
    }
  };
  walk(list, 0);
  return result;
};

const parentOptions = computed(() =>
  flattenPermissions(tableData.value, form.id)
);

const handleAdd = (parentId = 0) => {
  dialogTitle.value = "新增权限";
  Object.assign(form, {
    id: undefined,
    parentId,
    name: "",
    permissionKey: "",
    type: "menu",
    sort: 0,
    status: 1,
    remark: ""
  });
  dialogVisible.value = true;
};

const handleEdit = (row: PermissionItem) => {
  dialogTitle.value = "编辑权限";
  Object.assign(form, {
    id: row.id,
    parentId: row.parentId,
    name: row.name,
    permissionKey: row.permissionKey,
    type: row.type,
    sort: row.sort,
    status: row.status,
    remark: row.remark || ""
  });
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  await formRef.value?.validate();
  try {
    const api = form.id ? updatePermission : createPermission;
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

const handleDelete = (row: PermissionItem) => {
  ElMessageBox.confirm("确定要删除该权限吗？子权限也会被删除。", "提示", {
    type: "warning"
  }).then(async () => {
    const res = await deletePermission(row.id);
    if (res.code === 0) {
      ElMessage.success("删除成功");
      fetchData();
    } else {
      ElMessage.error(res.msg);
    }
  });
};

const typeLabel = (type: string) => {
  const map: Record<string, string> = {
    menu: "菜单",
    button: "按钮",
    api: "接口"
  };
  return map[type] || type;
};

onMounted(fetchData);
</script>

<template>
  <div class="p-4">
    <!-- 操作栏 -->
    <el-button type="primary" class="mb-4" @click="handleAdd(0)">
      新增权限
    </el-button>

    <!-- 树形表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      row-key="id"
      border
      default-expand-all
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
    >
      <el-table-column prop="name" label="权限名称" min-width="200" />
      <el-table-column prop="permissionKey" label="权限标识" min-width="200" />
      <el-table-column prop="type" label="类型" width="80" align="center">
        <template #default="{ row }">
          <el-tag
            :type="
              row.type === 'menu'
                ? 'primary'
                : row.type === 'button'
                  ? 'success'
                  : 'info'
            "
            size="small"
          >
            {{ typeLabel(row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" align="center" />
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? "正常" : "禁用" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="180" />
      <el-table-column label="操作" width="220" fixed="right" align="center">
        <template #default="{ row }">
          <el-button
            v-if="row.type !== 'button'"
            link
            type="primary"
            @click="handleAdd(row.id)"
          >
            新增子级
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="父级权限">
          <el-select
            v-model="form.parentId"
            placeholder="无（顶级权限）"
            clearable
            class="w-full"
          >
            <el-option :value="0" label="顶级权限" />
            <el-option
              v-for="item in parentOptions"
              :key="item.id"
              :value="item.id"
              :label="item.name"
              :class="`pl-[${item.level * 20}px]`"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio value="menu">菜单</el-radio>
            <el-radio value="button">按钮</el-radio>
            <el-radio value="api">接口</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="权限名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入权限名称" />
        </el-form-item>
        <el-form-item label="权限标识" prop="permissionKey">
          <el-input
            v-model="form.permissionKey"
            placeholder="如 system:user:list"
          />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
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
  </div>
</template>
