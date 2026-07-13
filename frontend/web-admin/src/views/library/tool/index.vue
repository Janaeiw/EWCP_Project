<script setup lang="ts">
defineOptions({ name: "LibraryTool" });
</script>

<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-medium">工具库</span>
        </div>
      </template>
      <el-empty description="工具库 - 待开发" />
    </el-card>
  </div>
</template>
