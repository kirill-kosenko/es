package com.transportexchangegroup.replication.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.transportexchangegroup.replication.utils.LoggingRequestInterceptor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;

@Configuration
public class Config {

	@Bean("datasource_1")
	@Primary
	@ConfigurationProperties(prefix="datasource.1")
	public DataSource datasource_1() {
		return DataSourceBuilder.create().build();
	}

	@Bean("datasource_2")
	@ConfigurationProperties(prefix="datasource.2")
	public DataSource datasource_2() {
		return DataSourceBuilder.create().build();
	}

	@Bean("transaction_manager_1")
	@Primary
	public PlatformTransactionManager transactionManager_1() {
		return new DataSourceTransactionManager(datasource_1());
	}

	@Bean("transaction_manager_2")
	public PlatformTransactionManager transactionManager_2() {
		return new DataSourceTransactionManager(datasource_2());
	}

	@Bean("jdbcTemplate_1")
	@Primary
	public JdbcTemplate jdbcTemplate_1() {
		return new JdbcTemplate(datasource_1());
	}

	@Bean("jdbcTemplate_2")
	public JdbcTemplate jdbcTemplate_2() {
		return new JdbcTemplate(datasource_2());
	}

	@Bean
	public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(mappingJacksonHttpMessageConverter());
		restTemplate.getInterceptors().add(new LoggingRequestInterceptor());

		return restTemplate;
	}

	@Bean
	public MappingJackson2HttpMessageConverter mappingJacksonHttpMessageConverter() {
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setObjectMapper(objectMapper());
		return converter;
	}

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

		return mapper;
	}
}
