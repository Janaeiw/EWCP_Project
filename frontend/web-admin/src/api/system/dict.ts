import { http } from "@/utils/http";
import type { ApiResult, PageResult } from "@/types/api";

export interface DictItem {
  id: number;
  dictCode: string;
  dictName: string;
  remark?: string;
  status: number;
  createTime?: string;
}

export interface DictDataItem {
  id: number;
  dictCode: string;
  label: string;
  value: string;
  sort: number;
  status: number;
  createTime?: string;
}

// ========== 字典类型 ==========

export const getDictPage = (params: {
  pageNum?: number;
  pageSize?: number;
  dictName?: string;
  status?: number;
}) => {
  return http.request<ApiResult<PageResult<DictItem>>>(
    "get",
    "/api/system/dict/page",
    { params }
  );
};

export const createDict = (data: Partial<DictItem>) => {
  return http.request<ApiResult<void>>("post", "/api/system/dict", { data });
};

export const updateDict = (data: Partial<DictItem>) => {
  return http.request<ApiResult<void>>("put", "/api/system/dict", { data });
};

export const deleteDict = (id: number) => {
  return http.request<ApiResult<void>>("delete", `/api/system/dict/${id}`);
};

// ========== 字典数据 ==========

export const getDictDataPage = (params: {
  pageNum?: number;
  pageSize?: number;
  dictCode?: string;
  status?: number;
}) => {
  return http.request<ApiResult<PageResult<DictDataItem>>>(
    "get",
    "/api/system/dict/data/page",
    { params }
  );
};

export const createDictData = (data: Partial<DictDataItem>) => {
  return http.request<ApiResult<void>>("post", "/api/system/dict/data", {
    data
  });
};

export const updateDictData = (data: Partial<DictDataItem>) => {
  return http.request<ApiResult<void>>("put", "/api/system/dict/data", {
    data
  });
};

export const deleteDictData = (id: number) => {
  return http.request<ApiResult<void>>("delete", `/api/system/dict/data/${id}`);
};

// ========== 全局字典 ==========

export const getAllDictData = () => {
  return http.request<ApiResult<Record<string, DictDataItem[]>>>(
    "get",
    "/api/system/dict/all"
  );
};
