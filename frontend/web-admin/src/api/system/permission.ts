import { http } from "@/utils/http";
import type { ApiResult } from "@/types/api";

export type PermissionItem = {
  id: number;
  parentId: number;
  name: string;
  permissionKey: string;
  type: string;
  sort: number;
  status: number;
  remark?: string;
  children?: PermissionItem[];
  createTime?: string;
};

/** 树形查询 */
export const getPermissionTree = () => {
  return http.request<ApiResult<PermissionItem[]>>(
    "get",
    "/api/system/permission/tree"
  );
};

/** 新增权限 */
export const createPermission = (data: Partial<PermissionItem>) => {
  return http.request<ApiResult<void>>("post", "/api/system/permission", {
    data
  });
};

/** 修改权限 */
export const updatePermission = (data: Partial<PermissionItem>) => {
  return http.request<ApiResult<void>>("put", "/api/system/permission", {
    data
  });
};

/** 删除权限 */
export const deletePermission = (id: number) => {
  return http.request<ApiResult<void>>(
    "delete",
    `/api/system/permission/${id}`
  );
};
