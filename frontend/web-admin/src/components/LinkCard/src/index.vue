<script setup lang="ts">
defineOptions({ name: "LinkCard" });

defineProps<{
  image?: string;
  title?: string;
  description?: string;
}>();
</script>

<template>
  <div class="link-card">
    <!-- 顶部：标题 -->
    <div v-if="title" class="link-card__title" :title="title">
      {{ title }}
    </div>

    <!-- 底部：描述 + 图片 -->
    <div class="link-card__footer">
      <div v-if="description" class="link-card__desc" :title="description">
        {{ description }}
      </div>
      <div v-else class="link-card__desc" />
      <div v-if="image" class="link-card__img">
        <el-image :src="image" fit="cover" class="w-full h-full">
          <template #error>
            <div
              class="flex items-center justify-center w-full h-full bg-gray-50 text-gray-300 text-xs"
            >
              加载失败
            </div>
          </template>
        </el-image>
      </div>
    </div>
  </div>
</template>

<style scoped>
.link-card {
  width: 250px;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  padding: 12px;
}

.link-card__title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.31;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-all;
}

.link-card__footer {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-top: 4px;
}

.link-card__desc {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: #999;
  line-height: 1.31;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-all;
}

.link-card__img {
  width: 51px;
  height: 51px;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
}
</style>
