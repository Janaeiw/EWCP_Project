<script setup lang="ts">
import {
  ref,
  reactive,
  computed,
  onMounted,
  watch,
  nextTick,
  onBeforeUnmount
} from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type {
  FormInstance,
  FormRules,
  UploadProps,
  UploadFile
} from "element-plus";
import {
  getContentPage,
  createContent,
  updateContent,
  deleteContent,
  sendContentToMoment,
  type ContentItem
} from "@/api/library/content";
import { uploadImage } from "@/api/common/image";
import { uploadVideo } from "@/api/common/video";
import { LinkCard } from "@/components/LinkCard";
import { useDictStoreHook } from "@/store/modules/dict";
import { isValidUrl } from "@/utils/validate";
import { Editor, Toolbar } from "@wangeditor/editor-for-vue";
import type {
  IDomEditor,
  IEditorConfig,
  IToolbarConfig
} from "@wangeditor/editor";
import "@wangeditor/editor/dist/css/style.css";

defineOptions({ name: "LibraryContent" });

// ===== 字典 =====
const dictStore = useDictStoreHook();
const typeOptions = computed(() => dictStore.getDictByCode("content_type"));
const typeLabel = (type: string) => {
  const item = typeOptions.value.find(d => d.value === type);
  return item?.label ?? type;
};

// ===== 搜索 =====
const searchForm = reactive({
  title: "",
  type: undefined as string | undefined,
  status: undefined as number | undefined
});

// ===== 表格 =====
const loading = ref(false);
const tableData = ref<ContentItem[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getContentPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      type: searchForm.type,
      title: searchForm.title || undefined,
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
  searchForm.title = "";
  searchForm.type = undefined;
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

// ===== 封面图片上传 =====
const handleCoverUpload: UploadProps["httpRequest"] = async options => {
  try {
    const res = await uploadImage(options.file as File);
    if (res.code === 0) {
      form.image = res.data.url;
      formRef.value?.clearValidate("image");
      options.onSuccess(res.data);
    } else {
      // @ts-expect-error element-plus internal type UploadAjaxError not exported
      options.onError(new Error(res.msg));
      ElMessage.error(res.msg);
    }
  } catch (e) {
    // @ts-expect-error element-plus internal type UploadAjaxError not exported
    options.onError(e instanceof Error ? e : new Error("上传失败"));
    ElMessage.error("上传失败");
  }
};

const handleCoverRemove = () => {
  form.image = "";
};

// ===== 视频上传 =====
const handleVideoUpload: UploadProps["httpRequest"] = async options => {
  try {
    const res = await uploadVideo(options.file as File);
    if (res.code === 0) {
      form.video = res.data.url;
      formRef.value?.clearValidate("video");
      options.onSuccess(res.data);
    } else {
      // @ts-expect-error element-plus internal type UploadAjaxError not exported
      options.onError(new Error(res.msg));
      ElMessage.error(res.msg);
    }
  } catch (e) {
    // @ts-expect-error element-plus internal type UploadAjaxError not exported
    options.onError(e instanceof Error ? e : new Error("上传失败"));
    ElMessage.error("上传失败");
  }
};

const handleVideoRemove = () => {
  form.video = "";
};

// 上传组件 file-list（从 form 字段计算）
const coverFileList = computed(() =>
  form.image ? [{ name: "已上传", url: form.image }] : []
);
const videoFileList = computed(() =>
  form.video ? [{ name: "已上传", url: form.video }] : []
);

const previewImageUrl = ref("");
const previewVisible = ref(false);
const handleCoverPreview = (file: UploadFile) => {
  if (file.url) {
    previewImageUrl.value = file.url;
    previewVisible.value = true;
  }
};

// ===== 富文本编辑器 =====
const editorRef = ref<IDomEditor | null>(null);

const editorConfig: Partial<IEditorConfig> = {
  placeholder: "请输入正文内容...",
  MENU_CONF: {
    uploadImage: {
      customUpload: async (file: File, insertFn: (url: string) => void) => {
        try {
          const res = await uploadImage(file);
          if (res.code === 0) {
            insertFn(res.data.url);
          } else {
            ElMessage.error(res.msg);
          }
        } catch {
          ElMessage.error("图片上传失败");
        }
      }
    }
  }
};

const toolbarConfig: Partial<IToolbarConfig> = {
  toolbarKeys: [
    "headerSelect",
    "bold",
    "italic",
    "bulletedList",
    "numberedList",
    "link",
    "uploadImage",
    "|",
    "undo",
    "redo"
  ]
};

const handleEditorCreated = (editor: IDomEditor) => {
  editorRef.value = editor;
};

onBeforeUnmount(() => {
  if (editorRef.value) {
    editorRef.value.destroy();
    editorRef.value = null;
  }
});

// ===== 弹窗表单 =====
const dialogVisible = ref(false);
const dialogTitle = ref("新增内容");
const dialogTab = ref("tweet");
const formRef = ref<FormInstance>();
const form = reactive<Partial<ContentItem>>({
  id: undefined,
  type: "tweet",
  link: "",
  title: "",
  description: "",
  image: "",
  video: "",
  content: "",
  status: 1
});

const rules = reactive<FormRules>({
  link: [
    {
      required: true,
      message: "请输入推文链接",
      trigger: "blur"
    },
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
  title: [{ required: true, message: "请输入标题", trigger: "blur" }],
  image: [
    {
      required: true,
      validator: (_rule, _value: string, callback) => {
        if (!form.image) {
          callback(new Error("请上传图片"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ],
  video: [
    {
      required: true,
      validator: (_rule, _value: string, callback) => {
        if (!form.video) {
          callback(new Error("请上传视频"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ]
});

const handleDialogTabChange = () => {
  form.type = dialogTab.value;
  formRef.value?.clearValidate();
};

const handleAdd = () => {
  dialogTitle.value = "新增内容";
  dialogTab.value = "tweet";
  Object.assign(form, {
    id: undefined,
    type: "tweet",
    link: "",
    title: "",
    description: "",
    image: "",
    video: "",
    content: "",
    status: 1
  });
  dialogVisible.value = true;
};

const handleEdit = (row: ContentItem) => {
  dialogTitle.value = "编辑内容";
  dialogTab.value = row.type;
  Object.assign(form, {
    id: row.id,
    type: row.type,
    link: row.link || "",
    title: row.title,
    description: row.description || "",
    image: row.image || "",
    video: row.video || "",
    content: row.content || "",
    status: row.status
  });
  dialogVisible.value = true;
};

// 弹窗关闭时销毁编辑器
watch(dialogVisible, val => {
  if (!val && editorRef.value) {
    editorRef.value.destroy();
    editorRef.value = null;
  }
});

const handleSubmit = async () => {
  await formRef.value?.validate();
  const submitData = { ...form };
  try {
    const api = form.id ? updateContent : createContent;
    const res = await api(submitData);
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

const handleDelete = (row: ContentItem) => {
  ElMessageBox.confirm("确定要删除该内容吗？", "提示", {
    type: "warning"
  }).then(async () => {
    const res = await deleteContent(row.id);
    if (res.code === 0) {
      ElMessage.success("删除成功");
      fetchData();
    } else {
      ElMessage.error(res.msg);
    }
  });
};

const handleSendMoment = async (row: ContentItem) => {
  try {
    await ElMessageBox.confirm(
      `确定要将「${row.title}」发送到企微朋友圈吗？`,
      "发送朋友圈",
      { type: "info", confirmButtonText: "发送", cancelButtonText: "取消" }
    );
  } catch {
    return; // 用户取消
  }
  const res = await sendContentToMoment(row.id);
  if (res.code === 0) {
    ElMessage.success("朋友圈任务创建成功");
  } else {
    ElMessage.error(res.msg || "发送失败");
  }
};

onMounted(() => {
  fetchData();
});
</script>

<template>
  <div>
    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb-4">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="标题">
          <el-input
            v-model="searchForm.title"
            class="w-[180px]!"
            placeholder="请输入标题"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select
            v-model="searchForm.type"
            class="w-[180px]!"
            placeholder="全部"
            clearable
          >
            <el-option
              v-for="item in typeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            class="w-[180px]!"
            placeholder="全部"
            clearable
          >
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
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
          <span class="font-medium">内容库</span>
          <el-button type="primary" @click="handleAdd">新增内容</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" stripe border>
        <el-table-column type="index" label="编号" width="70" align="center" />
        <el-table-column prop="type" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag type="primary" size="small">
              {{ typeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="内容" min-width="280">
          <template #default="{ row }">
            <LinkCard
              v-if="row.type === 'tweet' || row.type === 'article'"
              :image="row.image"
              :title="row.title"
              :description="row.description"
            />
            <div
              v-else-if="row.type === 'image'"
              class="flex gap-3"
              style="flex-direction: column"
            >
              <span>{{ row.title }}</span>
              <el-image
                v-if="row.image"
                :src="row.image"
                style="width: 100px; height: 100px"
                fit="cover"
                class="rounded shrink-0"
              />
            </div>
            <div
              v-else-if="row.type === 'video'"
              class="flex gap-3"
              style="flex-direction: column"
            >
              <span>{{ row.title }}</span>
              <video
                :src="row.video"
                controls
                style="width: 250px; height: 100px"
                class="rounded shrink-0"
              />
            </div>
            <div v-else>
              <span>{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? "上架" : "下架" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="创建时间"
          min-width="160"
          align="center"
        />
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button link type="warning" @click="handleSendMoment(row)">
              发朋友圈
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
      width="600px"
      destroy-on-close
    >
      <!-- 弹窗内类型 Tab -->
      <el-tabs
        v-model="dialogTab"
        class="mb-4"
        :class="{ 'pointer-events-none opacity-60': !!form.id }"
        @tab-change="handleDialogTabChange"
      >
        <el-tab-pane
          v-for="item in typeOptions"
          :key="item.value"
          :label="item.label"
          :name="item.value"
        />
      </el-tabs>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <!-- ===== 推文专属字段 ===== -->
        <template v-if="form.type === 'tweet'">
          <el-form-item label="推文链接" prop="link">
            <el-input
              v-model="form.link"
              placeholder="请输入推文链接"
              maxlength="500"
            />
          </el-form-item>
          <el-form-item label="推文标题" prop="title">
            <el-input
              v-model="form.title"
              placeholder="请输入推文标题"
              maxlength="50"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="推文描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              placeholder="请输入推文描述（非必填）"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="推文封面" prop="image">
            <div class="w-full">
              <el-upload
                :file-list="coverFileList"
                :http-request="handleCoverUpload"
                :on-remove="handleCoverRemove"
                :on-preview="handleCoverPreview"
                :limit="1"
                list-type="picture-card"
                accept="image/*"
                drag
              >
                <div class="el-upload__text">
                  将图片拖到此处，或<em>点击上传</em>
                </div>
              </el-upload>
              <div class="text-xs text-gray-400 mt-1">
                支持 JPG / PNG 格式，文件大小不超过 10MB。
              </div>
            </div>
          </el-form-item>
        </template>

        <!-- ===== 文章专属字段 ===== -->
        <template v-if="form.type === 'article'">
          <el-form-item label="文章标题" prop="title">
            <el-input
              v-model="form.title"
              placeholder="请输入文章标题"
              maxlength="50"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="文章描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              placeholder="请输入文章描述（非必填）"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="文章封面" prop="image">
            <div class="w-full">
              <el-upload
                :file-list="coverFileList"
                :http-request="handleCoverUpload"
                :on-remove="handleCoverRemove"
                :on-preview="handleCoverPreview"
                :limit="1"
                list-type="picture-card"
                accept="image/*"
                drag
              >
                <div class="el-upload__text">
                  将图片拖到此处，或<em>点击上传</em>
                </div>
              </el-upload>
              <div class="text-xs text-gray-400 mt-1">
                支持 JPG / PNG 格式，文件大小不超过 10MB。
              </div>
            </div>
          </el-form-item>
          <el-form-item label="正文" prop="content">
            <div class="w-full border border-gray-200 rounded">
              <Toolbar
                :editor="editorRef"
                :defaultConfig="toolbarConfig"
                class="border-b border-gray-200"
              />
              <Editor
                v-model="form.content"
                :defaultConfig="editorConfig"
                class="h-[300px]! overflow-y-hidden"
                @onCreated="handleEditorCreated"
              />
            </div>
          </el-form-item>
        </template>

        <!-- ===== 图片专属字段 ===== -->
        <template v-if="form.type === 'image'">
          <el-form-item label="图片标题" prop="title">
            <el-input
              v-model="form.title"
              placeholder="请输入图片标题"
              maxlength="50"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="图片" prop="image">
            <div class="w-full">
              <el-upload
                :file-list="coverFileList"
                :http-request="handleCoverUpload"
                :on-remove="handleCoverRemove"
                :on-preview="handleCoverPreview"
                :limit="1"
                list-type="picture-card"
                accept="image/*"
                drag
              >
                <div class="el-upload__text">
                  将图片拖到此处，或<em>点击上传</em>
                </div>
              </el-upload>
              <div class="text-xs text-gray-400 mt-1">
                支持 JPG / PNG 格式，文件大小不超过 10MB。
              </div>
            </div>
          </el-form-item>
        </template>

        <!-- ===== 视频专属字段 ===== -->
        <template v-if="form.type === 'video'">
          <el-form-item label="视频标题" prop="title">
            <el-input
              v-model="form.title"
              placeholder="请输入视频标题"
              maxlength="50"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="视频" prop="video">
            <div class="w-full">
              <el-upload
                :file-list="videoFileList"
                :http-request="handleVideoUpload"
                :on-remove="handleVideoRemove"
                :limit="1"
                accept="video/*"
                drag
              >
                <div class="el-upload__text">
                  将视频拖到此处，或<em>点击上传</em>
                </div>
              </el-upload>
              <div class="text-xs text-gray-400 mt-1">
                支持 MP4 格式，文件大小不超过 10MB。
              </div>
            </div>
          </el-form-item>
        </template>

        <!-- ===== 通用字段 ===== -->
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 图片预览 -->
    <el-dialog
      v-model="previewVisible"
      title="图片预览"
      width="400px"
      append-to-body
    >
      <img
        :src="previewImageUrl"
        class="w-full object-contain"
        alt="预览图片"
      />
    </el-dialog>
  </div>
</template>
