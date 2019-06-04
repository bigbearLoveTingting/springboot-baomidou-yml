# SpringBoot+Druid整合Mybatis-plus

## 1.Mybatis-plus简介

Mybatis-plus（以下简称MP）是一个Mybatis的增强工具，在 MyBatis 的基础上只做增强不做改变，为简化开发、提高效率而生。官方网站：<https://mp.baomidou.com/>

## 2.MP在SpringBoot中的配置

### 2.1引入的依赖

```xml
<dependency>
	<groupId>com.baomidou</groupId>
	<artifactId>mybatisplus-spring-boot-starter</artifactId>
	<version>1.0.5</version>
</dependency>
		
<dependency>
	<groupId>com.baomidou</groupId>
	<artifactId>mybatis-plus</artifactId>
	<version>2.2.0</version>
</dependency>

<dependency>
	<groupId>mysql</groupId>
	<artifactId>mysql-connector-java</artifactId>
	<scope>runtime</scope>
</dependency>

<dependency>
	<groupId>com.alibaba</groupId>
	<artifactId>druid</artifactId>
	<version>1.1.10</version>
</dependency>
```

### 2.2application.yml文件的配置

```yaml
#数据源配置
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/health?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowMultiQueries=true&zeroDateTimeBehavior=CONVERT_TO_NULL&serverTimezone=UTC
    username: root
    password: 724055
    driver-class-name: com.mysql.cj.jdbc.Driver
    platform: mysql
    type: com.alibaba.druid.pool.DruidDataSource
    
#MP配置
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  #实体扫描，多个package用逗号或者分号分隔
  typeAliasesPackage: com.greatwall.business.db
  global-config:
    #主键类型  0:"数据库ID自增", 1:"用户输入ID",2:"全局唯一ID (数字类型唯一ID)", 3:"全局唯一ID UUID";
    id-type: 0
    #字段策略 0:"忽略判断",1:"非 NULL 判断"),2:"非空判断"
    field-strategy: 1
    #数据库大写下划线转换
    capital-mode: true
    #刷新mapper 调试神器
    refresh-mapper: true
    #驼峰下划线转换
    db-column-underline: true
    #逻辑删除配置（下面3个配置）
    logic-delete-value: 1
    logic-not-delete-value: 0
    sql-injector: com.baomidou.mybatisplus.mapper.LogicSqlInjector
  #原生配置
  configuration:
    #配置返回数据库(column下划线命名&&返回java实体是驼峰命名)，自动匹配无需as（没开启这个，SQL需要写as： select user_id as userId） 
    map-underscore-to-camel-case: true
    cache-enabled: false
    call-setters-on-nulls: true
    #配置JdbcTypeForNull, oracle数据库必须配置
    jdbc-type-for-null: 'null'
```

### 2.3SpringBoot中需要编写的配置类

#### 2.3.1将数据源注入到IOC容器中

```java
package com.greatwall.business.config;

import javax.sql.DataSource;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alibaba.druid.pool.DruidDataSource;

/**
 * 
 * 将dataSource注入到IOC容器中
 * 
 * @author 尹雄标
 *
 */
@Configuration
@MapperScan(basePackages = "com.greatwall.business.db.*.dao")
public class DataSourceConfiguration {
	/**
	 * 数据库驱动
	 */
	@Value("${spring.datasource.driver-class-name}")
	private String jdbcDriver;
	/**
	 * Url
	 */
	@Value("${spring.datasource.url}")
	private String jdbcUrl;
	/**
	 * 用户名
	 */
	@Value("${spring.datasource.username}")
	private String jdbcUsername;
	/**
	 * 密码
	 */
	@Value("${spring.datasource.password}")
	private String jdbcPassword;

	/**
	 * 生成与spring-dao.xml对应的bean dataSource
	 * 
	 * @return
	 * @throws PropertyVetoException
	 */
	@Bean(name = "dataSource")
	@ConfigurationProperties(prefix = "spring.datasource")
	public DataSource dataSource() {
		return new DruidDataSource();
	}
}
```

#### 2.3.2使用MP的SqlSessionFactory以及分页插件的配置

```java
package com.greatwall.business.config;

import javax.sql.DataSource;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.baomidou.mybatisplus.entity.GlobalConfiguration;
import com.baomidou.mybatisplus.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;

/**
 * Mybatis-Plus的配置
 * 
 * @author 尹雄标
 *
 */
@Configuration
public class MybatisPlusConfig {

	private static final Logger log = LoggerFactory.getLogger(DataSourceConfiguration.class);
	
	@Autowired
	private DataSource dataSource;
	
	@Value("${mybatis-plus.global-config.id-type}")
	private Integer idType;
	
	@Value("${mybatis-plus.global-config.field-strategy}")
	private int fieldStrategy;
	
	@Value("${mybatis-plus.global-config.capital-mode}")
	private boolean capitalMode;
	
	@Value("${mybatis-plus.mapper-locations}")
	private String mapperLocations;
	
	@Value("${mybatis-plus.type-aliases-package}")
	private String typeAliasesPackage;
	
	@Value("${mybatis-plus.global-config.refresh-mapper}")
	private boolean refreshMapper;

	// mybatis plus 全局配置
	@Bean(name = "globalConfig")
	public GlobalConfiguration globalConfiguration() {
		log.info("初始化GlobalConfiguration");
		GlobalConfiguration configuration = new GlobalConfiguration();
		// 主键策略
		configuration.setRefresh(refreshMapper);
		configuration.setIdType(idType);
		// 字段策略
		configuration.setFieldStrategy(fieldStrategy);
		// 数据库大写 下划线转换
		configuration.setCapitalMode(capitalMode);
		return configuration;
	}

	@Bean(name = "sqlSessionFactory")
	public SqlSessionFactory createSqlSessionFactoryBean(
			@Qualifier(value = "globalConfig") GlobalConfiguration configuration) throws Exception {
		log.info("初始化SqlSessionFactory");
		MybatisSqlSessionFactoryBean sqlSessionFactoryBean = new MybatisSqlSessionFactoryBean();
		sqlSessionFactoryBean.setDataSource(dataSource);
		Interceptor[] interceptor = { new PaginationInterceptor() };
		sqlSessionFactoryBean.setPlugins(interceptor);
		ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		try {
			sqlSessionFactoryBean.setGlobalConfig(configuration);
			sqlSessionFactoryBean.setMapperLocations(resolver.getResources(mapperLocations));
			sqlSessionFactoryBean.setTypeAliasesPackage(typeAliasesPackage);
			return sqlSessionFactoryBean.getObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sqlSessionFactoryBean.getObject();
	}

	@Bean(name = "transactionManager")
	public DataSourceTransactionManager transactionManager() {
		log.info("初始化transactionManager");
		return new DataSourceTransactionManager(dataSource);
	}

}
```

#### 2.3.3MP分页插件使用示例

MP的分页实现是对sql进行拦截，并且在sql中注入分页，所以在sql中需要分页时，不需要我们考虑在sql中写分页，只需要传入MP的分页类对象，MP就会帮我们完成分页

（1）Controller层

```java
/**
	 * 用户列表
	 * 
	 * @param userModel 用户模型
	 * @param pageForm  分页参数
	 * @return
	 */
	@RequestMapping("/list")
	public ResponseVo list(UserModel userModel, PageForm pageForm) {
		return ResponseTool.success(userService.list(userModel, pageForm));
	}
```

（2）serviceImpl层

```java
@Override
	public Map<String, Object> list(UserModel userModel, PageForm pageForm) {
		Map<String, Object> resultMap = Maps.newHashMap();
		Page<UserModel> pageRecod = new Page<>(pageForm.getPageNo(), pageForm.getPageSize()); 
		List<UserModel> userList = userMapper.queryByTrem(userModel, pageRecod);
		resultMap.put("total", pageRecod.getTotal());
		resultMap.put("rows", userList);
		return resultMap;
	}
```

（3）mapper层

```java
List<UserModel> queryByTrem(UserModel userModel, Pagination page);
```

（4）sql

```xml
<select id="queryByTrem" parameterType="com.greatwall.business.db.user.model.UserModel" resultType="com.greatwall.business.db.user.model.UserModel" >
		SELECT
		   user_id
	      ,wx_open_id
	      ,mobile
	      ,nick_name
	      ,real_name
	      ,avatar
	      ,sex
	      ,status
	      ,create_time
		FROM
		  user
        <where>
            <if test="nickName != null and nickName != '' ">
                AND nick_name LIKE #{nickName}
            </if>
            <if test="sex == 0 or sex == 1 ">
                AND sex = #{sex}
            </if>
            <if test="status == 0 or  status == 1">
                AND status = #{status}
            </if>
        </where>
	</select>
```

（5）返回值

```json
{
    "code": 200,
    "message": "OK",
    "log": null,
    "data": {
        "total": 110010,
        "rows": [
            {
                "userId": null,
                "wxOpenId": null,
                "mobile": "17752850085",
                "nickName": null,
                "realName": null,
                "avatar": "1.jpg",
                "sex": 1,
                "status": 0,
                "createTime": null
            },
            {
                "userId": null,
                "wxOpenId": null,
                "mobile": "18274863848",
                "nickName": null,
                "realName": null,
                "avatar": "2.jpg",
                "sex": 1,
                "status": 0,
                "createTime": null
            },
            {
                "userId": null,
                "wxOpenId": null,
                "mobile": "18684819900",
                "nickName": null,
                "realName": null,
                "avatar": "3.jpg",
                "sex": 0,
                "status": 0,
                "createTime": null
            },
            {
                "userId": null,
                "wxOpenId": null,
                "mobile": "18608406427",
                "nickName": null,
                "realName": null,
                "avatar": "4.jpg",
                "sex": 0,
                "status": 0,
                "createTime": null
            },
            {
                "userId": null,
                "wxOpenId": null,
                "mobile": "15367630200",
                "nickName": null,
                "realName": null,
                "avatar": "5.jpg",
                "sex": 1,
                "status": 0,
                "createTime": null
            },
            {
                "userId": null,
                "wxOpenId": null,
                "mobile": "15874161515",
                "nickName": null,
                "realName": null,
                "avatar": "6.jpg",
                "sex": 1,
                "status": 0,
                "createTime": null
            },
            {
                "userId": null,
                "wxOpenId": null,
                "mobile": "15207338670",
                "nickName": null,
                "realName": null,
                "avatar": "7.jpg",
                "sex": 0,
                "status": 0,
                "createTime": null
            },
            {
                "userId": null,
                "wxOpenId": null,
                "mobile": "13786173754",
                "nickName": null,
                "realName": null,
                "avatar": "8.jpg",
                "sex": 0,
                "status": 0,
                "createTime": null
            },
            {
                "userId": null,
                "wxOpenId": null,
                "mobile": "18229333369",
                "nickName": null,
                "realName": null,
                "avatar": "9.jpg",
                "sex": 1,
                "status": 0,
                "createTime": null
            },
            {
                "userId": null,
                "wxOpenId": null,
                "mobile": "18390588753",
                "nickName": null,
                "realName": null,
                "avatar": "10.jpg",
                "sex": 1,
                "status": 0,
                "createTime": null
            }
        ]
    }
}
```

