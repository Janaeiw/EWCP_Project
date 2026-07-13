import { http } from "@/utils/http";
import type { ApiResult } from "@/types/api";

export interface UploadResult {
  id: number;
  url: string;
}

export const uploadImage = (file: File) => {
  const formData = new FormData();
  formData.append("file", file);
  return http.request<ApiResult<UploadResult>>("post", "/api/image/upload", {
    data: formData,
    headers: { "Content-Type": "multipart/form-data" }
  });
};
