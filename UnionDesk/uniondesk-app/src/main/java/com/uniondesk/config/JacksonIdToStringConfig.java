package com.uniondesk.config;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.CollectionSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

/**
 * 雪花 ID 精度修复配置
 *
 * <p>数据库主键使用雪花算法生成（19 位数字），超过 JS Number.MAX_SAFE_INTEGER（2^53-1），
 * 前端 JSON.parse 时会发生精度丢失，导致详情/合并/指派等基于 id 回传的操作异常。</p>
 *
 * <p>处理方式：序列化时，将属性名等于 {@code id}、以 {@code Id} 结尾（camelCase，如
 * ticketTypeId、businessDomainId、sessionId、parentId）或以 {@code _id} 结尾（snake_case，
 * 如 staff_account_id、domain_id、attachment_id）且类型为 {@link Long} / {@code long} 的
 * 标量字段统一输出为字符串；{@link List}/{@link Set} 等集合元素为 Long 的 id 字段
 * （如 watcherStaffAccountIds、menuIds）元素同样输出为字符串。其余数字字段
 * （total、version、count、page 等）保持数字不变。
 * 反序列化无需处理：前端回传字符串 id 时，Jackson 会自动将字符串转为 Long。</p>
 *
 * <p>以 {@link com.fasterxml.jackson.databind.Module} Bean 方式注册（Spring Boot 自动装配
 * 到 ObjectMapper），避免覆盖 Jackson2ObjectMapperBuilder 默认 module 装配
 * （JavaTimeModule 等 JSR-310 支持不受影响）。</p>
 */
@Configuration
public class JacksonIdToStringConfig {

    @Bean
    public com.fasterxml.jackson.databind.Module jacksonIdToStringModule() {
        SimpleModule module = new SimpleModule();
        module.setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                             BeanDescription beanDesc,
                                                             List<BeanPropertyWriter> beanProperties) {
                for (BeanPropertyWriter writer : beanProperties) {
                    JavaType type = writer.getType();
                    if (isIdFieldName(writer.getName())) {
                        if (isLongType(type.getRawClass())) {
                            writer.assignSerializer(ToStringSerializer.instance);
                        }
                        else if (type.isCollectionLikeType()
                                && isLongType(type.getContentType().getRawClass())) {
                            writer.assignSerializer(buildLongCollectionSerializer(type));
                        }
                    }
                }
                return beanProperties;
            }
        });
        return module;
    }

    /**
     * 是否为 id 语义字段名：等于 id、以 Id 结尾（camelCase）、或以 _id 结尾（snake_case）
     */
    private boolean isIdFieldName(String name) {
        return "id".equals(name) || name.endsWith("Id") || name.endsWith("_id");
    }

    private boolean isLongType(Class<?> rawClass) {
        return rawClass == Long.class || rawClass == long.class;
    }

    /**
     * 构造元素序列化为字符串的集合序列化器（List&lt;Long&gt; / Set&lt;Long&gt; 等）
     */
    @SuppressWarnings("unchecked")
    private JsonSerializer<Object> buildLongCollectionSerializer(JavaType collectionType) {
        JavaType contentType = collectionType.getContentType();
        return (JsonSerializer<Object>) (JsonSerializer<?>) new CollectionSerializer(
                contentType, false, null, ToStringSerializer.instance);
    }
}
