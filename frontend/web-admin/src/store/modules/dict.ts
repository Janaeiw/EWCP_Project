import { defineStore } from "pinia";
import { store } from "../utils";
import {
  getAllDictData,
  type DictDataItem
} from "@/api/system/dict";

interface DictState {
  /** 按 dictCode 分组的字典数据 */
  dictMap: Record<string, DictDataItem[]>;
}

export const useDictStore = defineStore("pure-dict", {
  state: (): DictState => ({
    dictMap: {}
  }),
  actions: {
    /** 拉取全部字典数据并缓存 */
    async fetchDictAll() {
      try {
        const res = await getAllDictData();
        if (res.code === 0) {
          this.dictMap = res.data;
        }
      } catch {
        console.error("字典数据加载失败");
      }
    },
    /** 清空字典缓存（登出时调用） */
    clearDict() {
      this.dictMap = {};
    }
  },
  getters: {
    /** 根据 dictCode 获取字典列表 */
    getDictByCode:
      state =>
      (code: string): DictDataItem[] => {
        return state.dictMap[code] ?? [];
      }
  }
});

export function useDictStoreHook() {
  return useDictStore(store);
}
