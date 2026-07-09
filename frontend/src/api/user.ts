import { http } from "@/utils/http";

export type UserResult = {
  success: boolean;
  data: {
    /** 头像 */
    avatar: string;
    /** 用户名 */
    username: string;
    /** 昵称 */
    nickname: string;
    /** 当前登录用户的角色 */
    roles: Array<string>;
    /** 按钮级别权限 */
    permissions: Array<string>;
    /** `token` */
    accessToken: string;
    /** 用于调用刷新`accessToken`的接口时所需的`token` */
    refreshToken: string;
    /** `accessToken`的过期时间（格式'xxxx/xx/xx xx:xx:xx'） */
    expires: Date;
  };
};

export type RefreshTokenResult = {
  success: boolean;
  data: {
    /** `token` */
    accessToken: string;
    /** 用于调用刷新`accessToken`的接口时所需的`token` */
    refreshToken: string;
    /** `accessToken`的过期时间（格式'xxxx/xx/xx xx:xx:xx'） */
    expires: Date;
  };
};

/** 后端原始响应格式 */
type BackendLoginResponse = {
  code: number;
  msg: string;
  data: {
    accessToken: string;
    username: string;
    nickname: string;
    avatar: string;
    roles: Array<string>;
    permissions: Array<string>;
    expires: Date;
  };
};

/** 登录 */
export const getLogin = (data?: object) => {
  // return http.request<UserResult>("post", "/login", { data });
  return http
    .request<BackendLoginResponse>("post", "/api/auth/login", { data })
    .then(res => {
      const result: UserResult = {
        success: res.code === 0,
        data: {
          ...res.data,
          refreshToken: res.data?.accessToken
            ? `${res.data.accessToken}Refresh`
            : ""
        }
      };
      return result;
    });
};

/** 刷新`token` */
export const refreshTokenApi = (data?: object) => {
  return http.request<RefreshTokenResult>("post", "/refresh-token", { data });
};
