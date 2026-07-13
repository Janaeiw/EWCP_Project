<script setup lang="ts">
defineOptions({ name: "LibraryPoster" });
</script>

<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-medium">海报库</span>
        </div>
      </template>
      <el-empty description="海报库 - 待开发" />
    </el-card>
  </div>
</template>
