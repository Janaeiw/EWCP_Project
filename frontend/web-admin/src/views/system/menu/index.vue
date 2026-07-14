<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import {
  getMenuTree,
  createMenu,
  updateMenu,
  deleteMenu,
  type MenuItem
} from "@/api/system/menu";

defineOptions({ name: "SystemMenu" });

// ===== 搜索 =====
const searchForm = reactive({ title: "" });

let searchTimer: ReturnType<typeof setTimeout> | null = null;
const handleSearchInput = () => {
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    fetchData();
  }, 300);
};

const handleSearch = () => {
  fetchData();
};

const handleReset = () => {
  searchForm.title = "";
  fetchData();
};

// ===== 表格数据 =====
const loading = ref(false);
const tableData = ref<MenuItem[]>([]);

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getMenuTree(searchForm.title || undefined);
    if (res.code === 0) {
      tableData.value = res.data;
    }
  } finally {
    loading.value = false;
  }
};

// ===== 弹窗表单 =====
const dialogVisible = ref(false);
const dialogTitle = ref("新增菜单");
const formRef = ref<FormInstance>();
const form = reactive<Partial<MenuItem>>({
  id: undefined,
  parentId: 0,
  path: "",
  name: "",
  component: "",
  title: "",
  icon: "",
  rank: 0,
  menuType: 0,
  permission: "",
  showLink: 0,
  status: 1,
  remark: ""
});

const rules: FormRules = {
  title: [{ required: true, message: "请输入菜单名称", trigger: "blur" }],
  name: [{ required: true, message: "请输入路由路径", trigger: "blur" }],
  path: [
    {
      required: true,
      validator: (_rule, _value, callback) => {
        if (form.menuType !== 1 && !form.path) {
          callback(new Error("请输入路由路径"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ],
  permission: [
    {
      required: true,
      validator: (_rule, _value, callback) => {
        if (form.menuType === 1 && !form.permission) {
          callback(new Error("请输入权限标识"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ]
};

// 父级菜单选择列表（扁平化，排除自身及子节点）
const flattenMenus = (list: MenuItem[], excludeId?: number) => {
  const result: { id: number; title: string; level: number }[] = [];
  const walk = (items: MenuItem[], level: number) => {
    for (const item of items) {
      if (item.id === excludeId) continue;
      result.push({ id: item.id, title: item.title, level });
      if (item.children?.length) {
        walk(item.children, level + 1);
      }
    }
  };
  walk(list, 0);
  return result;
};

const parentOptions = computed(() => flattenMenus(tableData.value, form.id));

const handleAdd = (parentId = 0) => {
  dialogTitle.value = "新增菜单";
  Object.assign(form, {
    id: undefined,
    parentId,
    path: "",
    name: "",
    component: "",
    title: "",
    icon: "",
    rank: 0,
    menuType: 0,
    permission: "",
    showLink: 0,
    status: 1,
    remark: ""
  });
  dialogVisible.value = true;
};

const handleEdit = (row: MenuItem) => {
  dialogTitle.value = "编辑菜单";
  Object.assign(form, {
    id: row.id,
    parentId: row.parentId,
    path: row.path,
    name: row.name || "",
    component: row.component || "",
    title: row.title,
    icon: row.icon || "",
    rank: row.rank,
    menuType: row.menuType ?? 0,
    permission: row.permission || "",
    showLink: row.showLink,
    status: row.status,
    remark: row.remark || ""
  });
  dialogVisible.value = true;
};

const handleTabChange = (val: number) => {
  form.showLink = val === 0 ? 1 : 0;
  formRef.value?.clearValidate();
};

const handleSubmit = async () => {
  await formRef.value?.validate();
  try {
    const api = form.id ? updateMenu : createMenu;
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

const handleDelete = (row: MenuItem) => {
  ElMessageBox.confirm("确定要删除该菜单吗？子菜单也会被删除。", "提示", {
    type: "warning"
  }).then(async () => {
    const res = await deleteMenu(row.id);
    if (res.code === 0) {
      ElMessage.success("删除成功");
      fetchData();
    } else {
      ElMessage.error(res.msg);
    }
  });
};

onMounted(fetchData);
</script>

<template>
  <div>
    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb-4">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="菜单名称">
          <el-input
            v-model="searchForm.title"
            class="w-[180px]!"
            placeholder="请输入菜单名称"
            clearable
            @input="handleSearchInput"
            @clear="fetchData"
          />
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
          <span class="font-medium">菜单管理</span>
          <el-button type="primary" @click="handleAdd(0)">新增菜单</el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="tableData"
        row-key="id"
        border
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      >
        <el-table-column prop="title" label="菜单名称" min-width="180" />
        <el-table-column
          prop="menuType"
          label="菜单类型"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="row.menuType === 1 ? 'info' : 'success'"
              size="small"
            >
              {{ row.menuType === 1 ? "按钮" : "菜单" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="icon" label="图标" width="100" align="center">
          <template #default="{ row }">
            <span class="text-gray-400">{{ row.icon || "-" }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" min-width="180" />
        <el-table-column
          prop="component"
          label="组件路径"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column prop="name" label="路由名称" width="140" />
        <el-table-column prop="rank" label="排序" width="80" align="center" />
        <el-table-column prop="showLink" label="显示" width="80" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.showLink === 1 ? 'success' : 'info'"
              size="small"
            >
              {{ row.showLink === 1 ? "显示" : "隐藏" }}
            </el-tag>
          </template>
        </el-table-column>
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
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.menuType !== 1"
              link
              type="primary"
              @click="handleAdd(row.id)"
            >
              新增子菜单
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
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group
            v-model="form.menuType"
            :disabled="!!form.id"
            @change="handleTabChange"
          >
            <el-radio-button :value="0">菜单</el-radio-button>
            <el-radio-button :value="1">按钮</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="父级菜单">
          <el-select
            v-model="form.parentId"
            placeholder="无（顶级菜单）"
            clearable
            class="w-full"
          >
            <el-option :value="0" label="顶级菜单" />
            <el-option
              v-for="item in parentOptions"
              :key="item.id"
              :value="item.id"
              :label="item.title"
              :class="`pl-[${item.level * 20}px]`"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          v-if="form.menuType === 1"
          label="权限标识"
          prop="permission"
        >
          <el-input
            v-model="form.permission"
            placeholder="如 system:user:add"
          />
        </el-form-item>
        <el-form-item label="菜单名称" prop="title">
          <el-input v-model="form.title" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 1" label="路由路径" prop="path">
          <el-input
            v-model="form.path"
            placeholder="如 /system/user 或 system/user"
          />
        </el-form-item>
        <el-form-item
          v-if="form.menuType !== 1"
          label="组件路径"
          prop="component"
        >
          <el-input
            v-model="form.component"
            placeholder="如 system/user/index"
          />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 1" label="路由名称" prop="name">
          <el-input v-model="form.name" placeholder="如 SystemUser" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 1" label="图标" prop="icon">
          <el-input v-model="form.icon" placeholder="如 ep:setting" />
        </el-form-item>
        <el-form-item label="排序" prop="rank">
          <el-input-number v-model="form.rank" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item
          v-if="form.menuType !== 1"
          label="是否显示"
          prop="showLink"
        >
          <el-radio-group v-model="form.showLink">
            <el-radio :value="1">显示</el-radio>
            <el-radio :value="0">隐藏</el-radio>
          </el-radio-group>
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
