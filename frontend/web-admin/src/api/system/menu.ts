import { http } from "@/utils/http";
import type { ApiResult } from "@/types/api";

export interface MenuItem {
  id: number;
  parentId: number;
  path: string;
  name: string;
  component: string;
  title: string;
  icon: string;
  rank: number;
  menuType: number;
  permission: string;
  showLink: number;
  status: number;
  remark?: string;
  children?: MenuItem[];
  createTime?: string;
}

/** 树形查询 */
export const getMenuTree = () => {
  return http.request<ApiResult<MenuItem[]>>("get", "/api/system/menu/tree");
};

/** 查询单个菜单 */
export const getMenuById = (id: number) => {
  return http.request<ApiResult<MenuItem>>("get", `/api/system/menu/${id}`);
};

/** 新增菜单 */
export const createMenu = (data: Partial<MenuItem>) => {
  return http.request<ApiResult<void>>("post", "/api/system/menu", { data });
};

/** 修改菜单 */
export const updateMenu = (data: Partial<MenuItem>) => {
  return http.request<ApiResult<void>>("put", "/api/system/menu", { data });
};

/** 删除菜单 */
export const deleteMenu = (id: number) => {
  return http.request<ApiResult<void>>("delete", `/api/system/menu/${id}`);
};
