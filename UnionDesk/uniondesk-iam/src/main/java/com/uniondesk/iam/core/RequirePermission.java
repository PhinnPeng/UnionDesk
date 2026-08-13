package com.uniondesk.iam.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    String[] value();

    /**
     * 目标业务域的 URI 模板变量名（如 "domainId"、"id"）。
     * 非空时按该业务域进行域级权限校验，防止域角色跨域越权；为空保持原有全局校验。
     */
    String domainIdParam() default "";
}
