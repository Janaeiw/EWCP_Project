import { http } from "@/utils/http";
import type { ApiResult, PageResult } from "@/types/api";

export interface ContentItem {
  id: number;
  type: string;
  link?: string;
  title: string;
  description?: string;
  image?: string;
  video?: string;
  content?: string;
  status: number;
  createTime?: string;
}

export const getContentPage = (params: {
  pageNum?: number;
  pageSize?: number;
  type?: string;
  title?: string;
  status?: number;
}) => {
  return http.request<ApiResult<PageResult<ContentItem>>>(
    "get",
    "/api/library/content/page",
    { params }
  );
};

export const createContent = (data: Partial<ContentItem>) => {
  return http.request<ApiResult<void>>("post", "/api/library/content", {
    data
  });
};

export const updateContent = (data: Partial<ContentItem>) => {
  return http.request<ApiResult<void>>("put", "/api/library/content", {
    data
  });
};

export const deleteContent = (id: number) => {
  return http.request<ApiResult<void>>("delete", `/api/library/content/${id}`);
};

export const sendContentToMoment = (id: number) => {
  return http.request<ApiResult<any>>(
    "post",
    `/api/library/content/send-moment/${id}`
  );
};
