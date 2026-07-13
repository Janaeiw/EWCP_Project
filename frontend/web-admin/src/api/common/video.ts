import { http } from "@/utils/http";
import type { ApiResult } from "@/types/api";

export interface UploadResult {
  id: number;
  url: string;
}

export const uploadVideo = (file: File) => {
  const formData = new FormData();
  formData.append("file", file);
  return http.request<ApiResult<UploadResult>>("post", "/api/video/upload", {
    data: formData,
    headers: { "Content-Type": "multipart/form-data" }
  });
};
