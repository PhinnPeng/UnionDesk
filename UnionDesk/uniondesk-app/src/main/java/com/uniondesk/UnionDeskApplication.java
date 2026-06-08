package com.uniondesk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan(value = "com.uniondesk.**.mapper", nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class UnionDeskApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnionDeskApplication.class, args);
    }
}
