import { http } from "@/utils/http";
import type { ApiResult, PageResult } from "@/types/api";

export type RoleItem = {
  id: number;
  roleName: string;
  roleKey: string;
  sort: number;
  status: number;
  remark?: string;
  createTime?: string;
};

/** 分页查询 */
export const getRolePage = (params: {
  pageNum?: number;
  pageSize?: number;
  roleName?: string;
  status?: number;
}) => {
  return http.request<ApiResult<PageResult<RoleItem>>>(
    "get",
    "/api/system/role/page",
    { params }
  );
};

/** 获取全部角色列表 */
export const getRoleList = () => {
  return http.request<ApiResult<RoleItem[]>>("get", "/api/system/role/list");
};

/** 新增角色 */
export const createRole = (data: Partial<RoleItem>) => {
  return http.request<ApiResult<void>>("post", "/api/system/role", { data });
};

/** 修改角色 */
export const updateRole = (data: Partial<RoleItem>) => {
  return http.request<ApiResult<void>>("put", "/api/system/role", { data });
};

/** 删除角色 */
export const deleteRole = (id: number) => {
  return http.request<ApiResult<void>>("delete", `/api/system/role/${id}`);
};
