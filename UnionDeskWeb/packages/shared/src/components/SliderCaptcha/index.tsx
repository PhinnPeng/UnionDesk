/**
 * 极简滑块验证组件
 * 用户拖动滑块到最右边即可验证通过
 */

import React, { useState, useRef, useCallback, useEffect } from 'react';
import type {
  SliderCaptchaProps,
  CaptchaStatus,
  TrackPoint,
  VerifyResult,
  SliderCaptchaVerifier,
} from './types';
import { verifyTrack } from './utils';
import './styles.css';

const SliderCaptcha: React.FC<SliderCaptchaProps> = ({
  onSuccess,
  onFail,
  verifier,
  width,
  height = 40,
  text = '请按住滑块，拖动到最右边',
  successText = '验证通过！',
  failText = '验证失败，请重试',
  disabled = false,
  className,
  style,
}) => {
  const [status, setStatus] = useState<CaptchaStatus>('idle');
  const [sliderLeft, setSliderLeft] = useState(0);
  const [startX, setStartX] = useState(0);
  const trackRef = useRef<TrackPoint[]>([]);
  const containerRef = useRef<HTMLDivElement>(null);
  const isDraggingRef = useRef(false);

  // 滑块宽度
  const sliderWidth = height;
  // 最大可滑动距离
  const maxDistance = (containerRef.current?.offsetWidth || width || 320) - sliderWidth;

  // 重置状态
  const reset = useCallback(() => {
    setStatus('idle');
    setSliderLeft(0);
    trackRef.current = [];
    isDraggingRef.current = false;
  }, []);

  // 开始拖动
  const handleStart = useCallback(
    (clientX: number) => {
      if (disabled || status === 'success') return;

      isDraggingRef.current = true;
      setStatus('moving');
      setStartX(clientX - sliderLeft);
      trackRef.current = [{ x: sliderLeft, t: Date.now() }];
    },
    [disabled, status, sliderLeft],
  );

  // 拖动中
  const handleMove = useCallback(
    (clientX: number) => {
      if (!isDraggingRef.current) return;

      const moveX = clientX - startX;
      const newLeft = Math.min(Math.max(moveX, 0), maxDistance);

      setSliderLeft(newLeft);
      trackRef.current.push({ x: newLeft, t: Date.now() });
    },
    [startX, maxDistance],
  );

  // 结束拖动
  const handleEnd = useCallback(async () => {
    if (!isDraggingRef.current) return;

    isDraggingRef.current = false;

    // 检查是否滑动到最右边
    const isAtEnd = sliderLeft >= maxDistance * 0.95;

    if (isAtEnd) {
      // 立即切换到校验中状态，提供即时视觉反馈
      setStatus('verifying');

      const normalizedTrack = trackRef.current.map(point => ({
        x: maxDistance > 0 ? Math.round((point.x / maxDistance) * 100) : 0,
        t: point.t,
      }));
      let result: VerifyResult;
      try {
        result = verifier
          ? await verifier(normalizedTrack)
          : verifyTrack(trackRef.current, maxDistance);
      } catch (error) {
        result = {
          success: false,
          message: error instanceof Error ? error.message : failText,
        };
      }

      if (result.success && result.token) {
        setStatus('success');
        onSuccess?.(result.token);
      } else {
        setStatus('fail');
        onFail?.(result.message || failText);

        // 1秒后重置
        setTimeout(() => {
          reset();
        }, 1000);
      }
    } else {
      // 未滑动到最右边，回弹到起点
      setStatus('idle');
      setSliderLeft(0);
      trackRef.current = [];
    }
  }, [sliderLeft, maxDistance, verifier, onSuccess, onFail, failText, reset]);

  // 鼠标事件
  const handleMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    handleStart(e.clientX);
  };

  const handleMouseMove = useCallback(
    (e: MouseEvent) => {
      handleMove(e.clientX);
    },
    [handleMove],
  );

  const handleMouseUp = useCallback(() => {
    void handleEnd();
  }, [handleEnd]);

  // 触摸事件
  const handleTouchStart = (e: React.TouchEvent) => {
    handleStart(e.touches[0].clientX);
  };

  const handleTouchMove = useCallback(
    (e: TouchEvent) => {
      if (isDraggingRef.current) {
        e.preventDefault();
        handleMove(e.touches[0].clientX);
      }
    },
    [handleMove],
  );

  const handleTouchEnd = useCallback(() => {
    void handleEnd();
  }, [handleEnd]);

  // 绑定全局事件
  useEffect(() => {
    if (isDraggingRef.current) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      document.addEventListener('touchmove', handleTouchMove, { passive: false });
      document.addEventListener('touchend', handleTouchEnd);

      return () => {
        document.removeEventListener('mousemove', handleMouseMove);
        document.removeEventListener('mouseup', handleMouseUp);
        document.removeEventListener('touchmove', handleTouchMove);
        document.removeEventListener('touchend', handleTouchEnd);
      };
    }
    return undefined;
  }, [handleMouseMove, handleMouseUp, handleTouchMove, handleTouchEnd]);

  // 显示文字
  const displayText = () => {
    switch (status) {
      case 'success':
        return successText;
      case 'fail':
        return failText;
      default:
        return text;
    }
  };

  // 容器类名
  const containerClassName = [
    'slider-captcha-container',
    status === 'moving' && 'moving',
    status === 'verifying' && 'verifying',
    disabled && 'disabled',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  // 已滑动区域类名
  const trackClassName = ['slider-captcha-track', status].filter(Boolean).join(' ');

  // 文字类名
  const textClassName = ['slider-captcha-text', status].filter(Boolean).join(' ');

  // 滑块类名
  const sliderClassName = [
    'slider-captcha-slider',
    status === 'moving' && 'moving',
    status === 'verifying' && 'verifying',
    status === 'success' && 'success',
    disabled && 'disabled',
  ]
    .filter(Boolean)
    .join(' ');

  // 图标类名
  const iconClassName = ['slider-captcha-icon', status].filter(Boolean).join(' ');

  return (
    <div
      ref={containerRef}
      className={containerClassName}
      style={{
        width: width ? `${width}px` : undefined,
        height: `${height}px`,
        ...style,
      }}
    >
      {/* 已滑动区域 */}
      <div
        className={trackClassName}
        style={{
          width: `${sliderLeft + sliderWidth}px`,
        }}
      />

      {/* 未滑动区域 */}
      <div
        className="slider-captcha-untracked"
        style={{
          left: `${sliderLeft + sliderWidth}px`,
        }}
      />

      {/* 提示文字 */}
      <div className={textClassName}>{displayText()}</div>

      {/* 滑块 */}
      <div
        className={sliderClassName}
        onMouseDown={handleMouseDown}
        onTouchStart={handleTouchStart}
        style={{
          left: `${sliderLeft}px`,
          width: `${sliderWidth}px`,
        }}
      >
        {/* 滑块图标 */}
        <svg
          className={iconClassName}
          viewBox="0 0 24 24"
        >
          <polyline points="9 18 15 12 9 6" />
        </svg>
      </div>
    </div>
  );
};

export default SliderCaptcha;
export type { SliderCaptchaProps, CaptchaStatus, TrackPoint, VerifyResult, SliderCaptchaVerifier };
