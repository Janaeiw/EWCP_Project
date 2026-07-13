<script setup lang="ts">
defineOptions({ name: "LibraryProduct" });
</script>

<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-medium">产品库</span>
        </div>
      </template>
      <el-empty description="产品库 - 待开发" />
    </el-card>
  </div>
</template>
