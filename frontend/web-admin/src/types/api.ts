/** 后端通用响应格式 */
export interface ApiResult<T> {
  code: number;
  msg: string;
  data: T;
}

/** MyBatis-Plus 分页数据 */
export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}
