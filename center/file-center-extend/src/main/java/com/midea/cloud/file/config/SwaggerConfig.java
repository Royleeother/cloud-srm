package com.midea.cloud.file.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * swagger文档
 * 
 * @author artifact
 *
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

	@Bean
	public Docket docket() {
		return new Docket(DocumentationType.SWAGGER_2).groupName("file-center-swagger")
				.apiInfo(new ApiInfoBuilder().title("文件中心swagger接口文档")
						.contact(new Contact("artifact", "", "huanghb14@meicloud.com")).version("1.0").build())
				.select().apis(RequestHandlerSelectors.basePackage("com.midea.cloud")).paths(PathSelectors.any()).build();
	}
}
