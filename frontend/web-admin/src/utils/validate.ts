const REGEXP_URL = /^https?:\/\/.+/;
const REGEXP_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const REGEXP_PHONE = /^1[3-9]\d{9}$/;
/** 密码格式：8-18位数字、字母、符号的任意两种组合 */
const REGEXP_PWD =
  /^(?![0-9]+$)(?![a-z]+$)(?![A-Z]+$)(?!([^(0-9a-zA-Z)]|[()])+$)(?!^.*[\u4E00-\u9FA5].*$)([^(0-9a-zA-Z)]|[()]|[a-z]|[A-Z]|[0-9]){8,18}$/;

/** 校验是否为有效链接（http/https 开头） */
export const isValidUrl = (value: string): boolean => REGEXP_URL.test(value);

/** 校验邮箱格式 */
export const isValidEmail = (value: string): boolean =>
  REGEXP_EMAIL.test(value);

/** 校验手机号格式（中国大陆） */
export const isValidPhone = (value: string): boolean =>
  REGEXP_PHONE.test(value);

/** 校验密码格式（8-18位，数字/字母/符号至少两种组合） */
export const isValidPassword = (value: string): boolean =>
  REGEXP_PWD.test(value);
