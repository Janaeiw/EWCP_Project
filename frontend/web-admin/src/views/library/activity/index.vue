<script setup lang="ts">
defineOptions({ name: "LibraryActivity" });
</script>

<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-medium">活动库</span>
        </div>
      </template>
      <el-empty description="活动库 - 待开发" />
    </el-card>
  </div>
</template>
