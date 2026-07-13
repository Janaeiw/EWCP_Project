import { http } from "@/utils/http";
import type { ApiResult, PageResult } from "@/types/api";

/** 用户实体 */
export type UserItem = {
  id: number;
  username: string;
  password?: string;
  nickname: string;
  avatar?: string;
  email?: string;
  phone?: string;
  deptId?: number;
  roleList?: string[];
  roleNames?: string[];
  roleIds?: number[];
  status: number;
  createTime?: string;
};

/** 分页查询 */
export const getUserPage = (params: {
  pageNum?: number;
  pageSize?: number;
  username?: string;
  nickname?: string;
  status?: number;
}) => {
  return http.request<ApiResult<PageResult<UserItem>>>(
    "get",
    "/api/system/user/page",
    { params }
  );
};

/** 查询单个用户 */
export const getUserById = (id: number) => {
  return http.request<ApiResult<UserItem>>("get", `/api/system/user/${id}`);
};

/** 新增用户 */
export const createUser = (data: Partial<UserItem>) => {
  return http.request<ApiResult<void>>("post", "/api/system/user", { data });
};

/** 修改用户 */
export const updateUser = (data: Partial<UserItem>) => {
  return http.request<ApiResult<void>>("put", "/api/system/user", { data });
};

/** 删除用户 */
export const deleteUser = (id: number) => {
  return http.request<ApiResult<void>>("delete", `/api/system/user/${id}`);
};

/** 重置密码 */
export const resetPassword = (id: number) => {
  return http.request<ApiResult<void>>(
    "put",
    `/api/system/user/reset-password/${id}`
  );
};
