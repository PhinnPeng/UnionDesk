/**
 * 滑块验证组件类型定义
 */

/**
 * 滑动轨迹点
 */
export interface TrackPoint {
  x: number;
  t: number; // 时间戳
}

/**
 * 验证结果
 */
export interface VerifyResult {
  success: boolean;
  token?: string;
  message?: string;
}

export type SliderCaptchaVerifier = (track: TrackPoint[]) => Promise<VerifyResult> | VerifyResult;

/**
 * 滑块验证组件属性
 */
export interface SliderCaptchaProps {
  /**
   * 验证成功回调
   */
  onSuccess?: (token: string) => void;

  /**
   * 验证失败回调
   */
  onFail?: (message: string) => void;

  /**
   * 自定义校验器，用于接入后端验证码校验
   */
  verifier?: SliderCaptchaVerifier;

  /**
   * 自定义宽度
   * @default undefined (100%)
   */
  width?: number;

  /**
   * 自定义高度
   * @default 40
   */
  height?: number;

  /**
   * 初始提示文字
   * @default "请按住滑块，拖动到最右边"
   */
  text?: string;

  /**
   * 成功提示文字
   * @default "验证通过！"
   */
  successText?: string;

  /**
   * 失败提示文字
   * @default "验证失败，请重试"
   */
  failText?: string;

  /**
   * 是否禁用
   * @default false
   */
  disabled?: boolean;

  /**
   * 自定义样式类名
   */
  className?: string;

  /**
   * 自定义样式
   */
  style?: React.CSSProperties;
}

/**
 * 验证状态
 */
export type CaptchaStatus = 'idle' | 'moving' | 'verifying' | 'success' | 'fail';
