<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import {
  getDictPage,
  createDict,
  updateDict,
  deleteDict,
  getDictDataPage,
  createDictData,
  updateDictData,
  deleteDictData,
  type DictItem,
  type DictDataItem
} from "@/api/system/dict";

defineOptions({ name: "SystemDict" });

// ========== 字典类型 ==========

const searchForm = reactive({
  dictName: "",
  status: undefined as number | undefined
});

const loading = ref(false);
const tableData = ref<DictItem[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getDictPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      dictName: searchForm.dictName || undefined,
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
  searchForm.dictName = "";
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

// ===== 字典类型弹窗 =====
const dialogVisible = ref(false);
const dialogTitle = ref("新增字典");
const formRef = ref<FormInstance>();
const form = reactive<Partial<DictItem>>({
  id: undefined,
  dictCode: "",
  dictName: "",
  remark: "",
  status: 1
});

const rules: FormRules = {
  dictCode: [{ required: true, message: "请输入字典编码", trigger: "blur" }],
  dictName: [{ required: true, message: "请输入字典名称", trigger: "blur" }]
};

const handleAdd = () => {
  dialogTitle.value = "新增字典";
  Object.assign(form, {
    id: undefined,
    dictCode: "",
    dictName: "",
    remark: "",
    status: 1
  });
  dialogVisible.value = true;
};

const handleEdit = (row: DictItem) => {
  dialogTitle.value = "编辑字典";
  Object.assign(form, {
    id: row.id,
    dictCode: row.dictCode,
    dictName: row.dictName,
    remark: row.remark || "",
    status: row.status
  });
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  await formRef.value?.validate();
  try {
    const api = form.id ? updateDict : createDict;
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

const handleDelete = (row: DictItem) => {
  ElMessageBox.confirm(
    "删除字典类型将同时删除其下所有字典数据，确定要删除吗？",
    "提示",
    { type: "warning" }
  ).then(async () => {
    const res = await deleteDict(row.id);
    if (res.code === 0) {
      ElMessage.success("删除成功");
      fetchData();
    } else {
      ElMessage.error(res.msg);
    }
  });
};

// ========== 字典数据 ==========

const dataDialogVisible = ref(false);
const currentDictCode = ref("");
const currentDictName = ref("");

const dataLoading = ref(false);
const dataTableData = ref<DictDataItem[]>([]);
const dataTotal = ref(0);
const dataPageNum = ref(1);
const dataPageSize = ref(10);

const handleOpenData = (row: DictItem) => {
  currentDictCode.value = row.dictCode;
  currentDictName.value = row.dictName;
  dataPageNum.value = 1;
  fetchDataList();
  dataDialogVisible.value = true;
};

const fetchDataList = async () => {
  dataLoading.value = true;
  try {
    const res = await getDictDataPage({
      pageNum: dataPageNum.value,
      pageSize: dataPageSize.value,
      dictCode: currentDictCode.value
    });
    if (res.code === 0) {
      dataTableData.value = res.data.records;
      dataTotal.value = res.data.total;
    }
  } finally {
    dataLoading.value = false;
  }
};

const handleDataSizeChange = (val: number) => {
  dataPageSize.value = val;
  fetchDataList();
};

const handleDataCurrentChange = (val: number) => {
  dataPageNum.value = val;
  fetchDataList();
};

// ===== 字典数据弹窗内的编辑 =====
const dataFormVisible = ref(false);
const dataFormTitle = ref("新增数据");
const dataFormRef = ref<FormInstance>();
const dataForm = reactive<Partial<DictDataItem>>({
  id: undefined,
  dictCode: "",
  label: "",
  value: "",
  sort: 0,
  status: 1
});

const dataRules: FormRules = {
  label: [{ required: true, message: "请输入显示文本", trigger: "blur" }],
  value: [{ required: true, message: "请输入字典值", trigger: "blur" }]
};

const handleDataAdd = () => {
  dataFormTitle.value = "新增数据";
  Object.assign(dataForm, {
    id: undefined,
    dictCode: currentDictCode.value,
    label: "",
    value: "",
    sort: 0,
    status: 1
  });
  dataFormVisible.value = true;
};

const handleDataEdit = (row: DictDataItem) => {
  dataFormTitle.value = "编辑数据";
  Object.assign(dataForm, {
    id: row.id,
    dictCode: row.dictCode,
    label: row.label,
    value: row.value,
    sort: row.sort,
    status: row.status
  });
  dataFormVisible.value = true;
};

const handleDataSubmit = async () => {
  await dataFormRef.value?.validate();
  try {
    const api = dataForm.id ? updateDictData : createDictData;
    const res = await api(dataForm);
    if (res.code === 0) {
      ElMessage.success(dataForm.id ? "修改成功" : "新增成功");
      dataFormVisible.value = false;
      fetchDataList();
    } else {
      ElMessage.error(res.msg);
    }
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : "操作失败");
  }
};

const handleDataDelete = (row: DictDataItem) => {
  ElMessageBox.confirm("确定要删除该字典数据吗？", "提示", {
    type: "warning"
  }).then(async () => {
    const res = await deleteDictData(row.id);
    if (res.code === 0) {
      ElMessage.success("删除成功");
      fetchDataList();
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
        <el-form-item label="字典名称">
          <el-input
            v-model="searchForm.dictName"
            class="w-[180px]!"
            placeholder="请输入字典名称"
            clearable
            @keyup.enter="handleSearch"
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
          <span class="font-medium">字典管理</span>
          <el-button type="primary" @click="handleAdd">新增字典</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" stripe border>
        <el-table-column type="index" label="编号" width="70" align="center" />
        <el-table-column prop="dictCode" label="字典编码" min-width="140" />
        <el-table-column prop="dictName" label="字典名称" min-width="140" />
        <el-table-column prop="remark" label="备注" min-width="200" />
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
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleOpenData(row)">
              字典数据
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

    <!-- 字典类型新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="字典编码" prop="dictCode">
          <el-input
            v-model="form.dictCode"
            :disabled="!!form.id"
            placeholder="如 content_type"
          />
        </el-form-item>
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="form.dictName" placeholder="请输入字典名称" />
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

    <!-- 字典数据弹窗 -->
    <el-dialog
      v-model="dataDialogVisible"
      :title="`${currentDictName}（${currentDictCode}）`"
      width="700px"
      destroy-on-close
    >
      <div class="mb-4 flex justify-end">
        <el-button type="primary" size="small" @click="handleDataAdd">
          新增数据
        </el-button>
      </div>

      <el-table
        v-loading="dataLoading"
        :data="dataTableData"
        stripe
        border
        size="small"
      >
        <el-table-column type="index" label="编号" width="60" align="center" />
        <el-table-column prop="label" label="显示文本" min-width="120" />
        <el-table-column prop="value" label="字典值" min-width="120" />
        <el-table-column prop="sort" label="排序" width="70" align="center" />
        <el-table-column prop="status" label="状态" width="70" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : 'danger'"
              size="small"
            >
              {{ row.status === 1 ? "正常" : "禁用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleDataEdit(row)">
              编辑
            </el-button>
            <el-button link type="danger" size="small" @click="handleDataDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="dataPageNum"
          v-model:page-size="dataPageSize"
          :page-sizes="[10, 20, 50]"
          :total="dataTotal"
          layout="total, sizes, prev, pager, next"
          size="small"
          @size-change="handleDataSizeChange"
          @current-change="handleDataCurrentChange"
        />
      </div>
    </el-dialog>

    <!-- 字典数据新增/编辑弹窗 -->
    <el-dialog
      v-model="dataFormVisible"
      :title="dataFormTitle"
      width="450px"
      destroy-on-close
    >
      <el-form
        ref="dataFormRef"
        :model="dataForm"
        :rules="dataRules"
        label-width="80px"
      >
        <el-form-item label="显示文本" prop="label">
          <el-input v-model="dataForm.label" placeholder="如：推文" />
        </el-form-item>
        <el-form-item label="字典值" prop="value">
          <el-input v-model="dataForm.value" placeholder="如：post" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="dataForm.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="dataForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataFormVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDataSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
