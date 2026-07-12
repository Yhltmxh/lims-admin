# LIMS 环境检测系统 — 基础设施设计文档

**版本**：1.0  
**日期**：2026-07-12  
**状态**：已确认

---

## 概述

本项目为环境检测体制内项目，采用 **Spring Boot 3.5.16 单体 + 前后端分离** 架构。本次目标是构造具备基础功能、可供后续业务模块开发的**基础设施项目**。

### 开发范围

| 范围 | 内容 |
|------|------|
| ✅ 实现 | 环境配置、项目结构、common 模块、security 模块、system 模块（RBAC 五表 + 登录/注销/鉴权） |
| ❌ 不实现 | 业务模块仅建空目录骨架（sample/project/task/instrument/report/customer/contract/workflow/notification/file） |

### 核心决策摘要

| 决策 | 选择 |
|------|------|
| 基础包名 | `com.shou.lims` |
| 数据库 schema | 单一 schema（`public`），表名前缀区分模块 |
| Service 架构 | 方案 A — 严格分层，自定义接口隐藏 MyBatis-Plus IService |
| 时间类型 | `LocalDateTime` |
| JSON 库 | Jackson（Spring Boot 默认） |
| 响应体 | `Result<T>` |

---

## 一、技术选型与依赖版本

### 1.1 环境

| 组件 | 版本/配置 |
|------|-----------|
| JDK | 17 |
| Spring Boot | 3.5.16 |
| Spring Security | 6.5.11（Spring Boot 3.5.16 托管） |
| MyBatis-Plus | 3.5.12 |
| 数据库 | IvorySQL 3.4（127.0.0.1:5432，用户 `ivorysql`，密码 `123456`） |
| Redis | 7.x（127.0.0.1:6380，密码 `123456`） |

### 1.2 Maven 依赖清单

| 类别 | artifactId | 版本 |
|------|-----------|------|
| ORM | `mybatis-plus-spring-boot3-starter` | 3.5.12 |
| ORM 分页 | `mybatis-plus-jsqlparser` | 3.5.12 |
| 数据库 | `postgresql` | 42.7.11（IvorySQL 兼容 PG 驱动） |
| 缓存 | `spring-boot-starter-data-redis` | Spring Boot 托管 |
| 安全 | `spring-boot-starter-security` | Spring Boot 托管 |
| JWT | `java-jwt`（com.auth0） | 4.5.0 |
| API 文档 | `knife4j-openapi3-jakarta-spring-boot-starter` | 4.6.0 |
| 对象映射 | `mapstruct` | 1.6.3 |
| 对象映射 | `mapstruct-processor`（annotation processor） | 1.6.3 |
| 工具 | `guava` | 33.4.8-jre |
| 工具 | `commons-lang3` | 3.18.0 |
| 工具 | `commons-collections4` | 4.5.0 |
| 配置提示 | `spring-boot-configuration-processor` | Spring Boot 托管 |
| 开发 | `spring-boot-devtools` | 已引入（runtime） |

---

## 二、项目结构设计

### 2.1 完整目录树

```
src/main/java/com/shou/lims/
├── LimsApplication.java
├── common/
│   ├── config/          WebMvcConfig, MyBatisPlusConfig, RedisConfig, Knife4jConfig
│   ├── constant/        RedisKeyPrefix, GlobalConstants
│   ├── enums/           StatusEnum, GenderEnum
│   ├── exception/       BusinessException, NotFoundException, GlobalExceptionHandler
│   ├── response/        Result<T>, PageQuery, PageVO<T>
│   ├── util/            IpUtils, TreeUtils
│   ├── validation/      @Phone, EnumValueValidator
│   └── cache/           CacheService
├── security/
│   ├── jwt/             JwtTokenService, JwtAccessTokenProperties, JwtRefreshTokenProperties
│   ├── filter/          JwtAuthFilter
│   ├── handler/         LoginSuccessHandler, AuthenticationFailureHandler, AccessDeniedHandler
│   ├── service/         SecurityUserDetailsService
│   └── config/          SecurityConfig
├── system/
│   ├── user/            {controller, service/impl, mapper, entity, dto, vo, converter}
│   ├── role/            同上
│   ├── permission/      同上
│   ├── dept/            同上
│   ├── menu/            同上
│   └── log/             同上（+ annotation @Log + aop LogAspect）
├── sample/              空目录 + 子包骨架
├── project/             空目录 + 子包骨架
├── task/                空目录 + 子包骨架
├── instrument/          空目录 + 子包骨架
├── report/              空目录 + 子包骨架
├── customer/            空目录 + 子包骨架
├── contract/            空目录 + 子包骨架
├── workflow/            空目录 + 子包骨架
├── notification/        空目录 + 子包骨架
└── file/                空目录 + 子包骨架
```

### 2.2 业务模块内部结构

每个模块（如 `system/user`）标准子包：

```
user/
├── controller/          RESTful 接口
├── service/             Service 接口（不继承 MyBatis-Plus IService）
│   └── impl/            Service 实现（内部持有 Mapper）
├── mapper/              MyBatis-Plus BaseMapper 接口
├── entity/              数据库实体
├── dto/                 CreateDTO / UpdateDTO / QueryDTO
├── vo/                  UserVO / UserListVO
└── converter/           MapStruct 转换器
```

### 2.3 Controller 规范

严格按照 RESTful + 统一返回体 + DTO 入参 + VO 出参 + 分页统一封装：

| 规范 | 做法 |
|------|------|
| URL 风格 | RESTful（`GET /system/users/{id}`、`POST /system/users`） |
| Controller 职责 | 仅负责参数接收、校验、调用 Service、返回结果 |
| 入参 | `CreateDTO`、`UpdateDTO`、`QueryDTO`，不直接接收 Entity |
| 出参 | `VO`，不直接返回 Entity |
| 返回格式 | 统一 `Result<T>` |
| 参数校验 | `@Valid` + Jakarta Validation，配合全局异常处理 |
| 分页 | 统一 `PageQuery` 入参 / `PageVO<T>` 出参，不直接暴露 MyBatis-Plus `IPage` |
| 事务 | 仅放在 Service 层 |
| 权限 | `@PreAuthorize("hasAuthority('xxx')")` |

示例：

```java
@GetMapping("/{id}")
public Result<UserVO> getById(@PathVariable Long id) {
    return Result.success(userService.getById(id));
}

@PostMapping
public Result<Long> create(@Valid @RequestBody UserCreateDTO dto) {
    return Result.success(userService.create(dto));
}
```

---

## 三、环境配置

### 3.1 application.yml

```yaml
spring:
  application:
    name: lims-admin

  # 数据源 - IvorySQL
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://127.0.0.1:5432/lims?currentSchema=public
    username: ivorysql
    password: 123456
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 300000
      max-lifetime: 1200000

  # Redis
  data:
    redis:
      host: 127.0.0.1
      port: 6380
      password: 123456
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2

  # Jackson
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false

# MyBatis-Plus
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  type-aliases-package: com.shou.lims
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  pagination:
    db-type: postgresql

# JWT
jwt:
  access-token:
    secret: <256-bit-base64-key>
    expiration: 15           # 分钟
  refresh-token:
    expiration: 7            # 天

# Knife4j
knife4j:
  enable: true
  setting:
    language: zh-CN

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs

# 日志
logging:
  level:
    com.shou.lims: debug
    org.springframework.security: debug
```

### 3.2 多环境配置

```
src/main/resources/
├── application.yml           # 公共配置
├── application-dev.yml       # 开发环境：debug 日志、DevTools
└── application-prod.yml      # 生产环境：info 日志、关闭 swagger、隐藏错误详情
```

---

## 四、公共模块（common）设计

### 4.1 统一响应体 `Result<T>`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }
}
```

### 4.2 统一分页封装

```java
@Data
public class PageQuery {
    @Min(1)
    private Integer pageNum = 1;

    @Min(1)
    @Max(200)          // 硬上限
    private Integer pageSize = 20;
}

@Data
public class PageVO<T> {
    private List<T> records;
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private Integer totalPages;

    public static <T> PageVO<T> of(IPage<T> page) {
        PageVO<T> vo = new PageVO<>();
        vo.setRecords(page.getRecords());
        vo.setTotal(page.getTotal());
        vo.setPageNum((int) page.getCurrent());
        vo.setPageSize((int) page.getSize());
        vo.setTotalPages((int) page.getPages());
        return vo;
    }
}
```

### 4.3 全局异常处理

业务异常基类与子类：

| 类 | 用途 | 默认 HTTP 状态 |
|----|------|---------------|
| `BusinessException` | 通用业务异常（可设置 code） | 500 |
| `NotFoundException` | 资源不存在 | 404 |
| `UnauthorizedException` | 未认证 | 401 |
| `ForbiddenException` | 无权限 | 403 |

`@RestControllerAdvice` 全局拦截：
- `BusinessException` → `Result.fail(code, message)`
- `MethodArgumentNotValidException` → `Result.fail(400, "字段: 错误信息")`
- `Exception`（兜底）→ `Result.fail(500, "系统内部错误")` + 日志记录

错误码体系：

| 范围 | 类别 |
|------|------|
| 200 | 成功 |
| 400 | 参数校验失败 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 冲突 |
| 500 | 系统错误 |

### 4.4 其他子包

| 子包 | 内容 |
|------|------|
| `config/` | `MyBatisPlusConfig`（分页插件+拦截器）、`RedisConfig`（JSON 序列化）、`WebMvcConfig`（日期格式化+跨域）、`Knife4jConfig` |
| `constant/` | Redis Key 前缀常量、全局常量 |
| `enums/` | `StatusEnum(ENABLED, DISABLED)`、`GenderEnum` |
| `util/` | `IpUtils`、`TreeUtils`（菜单/部门树构建） |
| `validation/` | `@Phone`、`@EnumValue` 自定义校验注解 |
| `cache/` | `CacheService` 封装常用 Redis 操作 |

---

## 五、安全模块（security）设计

### 5.1 双 Token 流程

```
登录 POST /auth/login {username, password}
  │
  ▼
验证用户名密码 ──→ 失败 → 返回 401
  │
  ▼
生成 AccessToken (HS256, 15min) + RefreshToken (256bit随机字符串, 7天)
RefreshToken 写入 Redis: "refresh:<userId>" → token值, TTL=7天
  │
  ▼
响应: { accessToken, refreshToken, expiresIn: 900 }

访问受限接口 ──→ Authorization: Bearer <accessToken>
  │
  ▼
JwtAuthFilter 校验签名 + 是否过期
  ├── 通过 → 设置 SecurityContext 认证信息
  └── 过期 → 返回 401，前端用 refreshToken 调用 /auth/refresh
                    │
                    ▼
              校验 Redis 中 refreshToken 是否存在且匹配
              ├── 通过 → 签发新 accessToken + 新 refreshToken（旧 refreshToken 删除，轮转刷新）
              └── 失败 → 返回 401，跳转登录

注销 POST /auth/logout
  │
  ▼
删除 Redis 中 refreshToken
accessToken 加入黑名单（Redis key: "blacklist:<jti>"，TTL=剩余有效期）
```

### 5.2 核心类

| 类 | 职责 |
|----|------|
| `JwtTokenService` | accessToken 生成（HS256）与校验，refreshToken 生成（SecureRandom 256bit hex） |
| `RefreshTokenService` | Redis 中 refreshToken 存储/校验/撤销/轮转 |
| `JwtAuthFilter` | 继承 `OncePerRequestFilter`，从 Header 取 Bearer Token，校验并设置 SecurityContext |
| `SecurityUserDetailsService` | 实现 `UserDetailsService`，查用户+角色+权限，构造 `SecurityUserDetails` |
| `LoginSuccessHandler` | 实现 `AuthenticationSuccessHandler`，登录成功生成双 Token 返回 |
| `AuthenticationFailureHandler` | 实现 `AuthenticationEntryPoint`，返回 401 |
| `AccessDeniedHandler` | 实现 `AccessDeniedHandler`，返回 403 |

### 5.3 SecurityConfig

```java
@Configuration
@EnableMethodSecurity          // 启用 @PreAuthorize
@EnableConfigurationProperties(JwtAccessTokenProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/auth/login", "/auth/refresh",
                    "/doc.html", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e
                .authenticationEntryPoint(authenticationFailureHandler)
                .accessDeniedHandler(accessDeniedHandler)
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 5.4 鉴权接口

| 接口 | 方法 | URL | 说明 | 鉴权 |
|------|------|-----|------|------|
| 登录 | POST | `/auth/login` | 传 username+password，返回双 Token | 公开 |
| 刷新 | POST | `/auth/refresh` | 传 refreshToken，返回新双 Token | 公开 |
| 注销 | POST | `/auth/logout` | 撤销 refreshToken，accessToken 加入黑名单 | 需登录 |
| 当前用户 | GET | `/auth/me` | 返回当前登录用户信息+权限列表 | 需登录 |

---

## 六、RBAC 数据表设计

### 6.1 表关系

```
sys_user ──┐                    sys_menu
  │        │                        │
  │    sys_user_role ── sys_role ── sys_role_menu
  │                        │
  │                    sys_role_permission
  │                        │
  └────────────────── sys_permission

sys_dept ←── sys_user (dept_id)
```

### 6.2 表结构

所有业务表统一字段：
- `deleted SMALLINT DEFAULT 0`（逻辑删除，1=已删，0=正常）
- `created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP`
- `updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP`

#### sys_dept（部门表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK 自增 | |
| parent_id | BIGINT | 父部门ID，0=顶级 |
| name | VARCHAR(64) NOT NULL | 部门名称 |
| sort_order | INT DEFAULT 0 | 排序 |
| leader | VARCHAR(32) | 负责人 |
| phone | VARCHAR(20) | 联系电话 |
| status | SMALLINT DEFAULT 1 | 1启用 0禁用 |

#### sys_user（用户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK 自增 | |
| username | VARCHAR(32) NOT NULL UNIQUE | 登录名 |
| password | VARCHAR(128) NOT NULL | BCrypt 加密 |
| real_name | VARCHAR(32) | 真实姓名 |
| phone | VARCHAR(20) | |
| email | VARCHAR(64) | |
| gender | SMALLINT DEFAULT 0 | 0未知 1男 2女 |
| avatar | VARCHAR(256) | 头像 URL |
| dept_id | BIGINT | FK → sys_dept.id |
| status | SMALLINT DEFAULT 1 | 1启用 0禁用 |

#### sys_role（角色表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK 自增 | |
| name | VARCHAR(32) NOT NULL | ROLE_ADMIN, ROLE_SAMPLER 等 |
| label | VARCHAR(32) NOT NULL | 显示名：管理员、采样员 |
| description | VARCHAR(128) | 说明 |
| status | SMALLINT DEFAULT 1 | |

#### sys_permission（权限表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK 自增 | |
| name | VARCHAR(64) NOT NULL | 权限名称 |
| code | VARCHAR(64) NOT NULL UNIQUE | 权限标识：`system:user:add` |
| type | SMALLINT NOT NULL | 1菜单 2按钮 3接口 |
| parent_id | BIGINT DEFAULT 0 | |
| sort_order | INT DEFAULT 0 | |
| status | SMALLINT DEFAULT 1 | |

#### sys_menu（菜单表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK 自增 | |
| parent_id | BIGINT DEFAULT 0 | |
| name | VARCHAR(32) NOT NULL | 路由名称 |
| path | VARCHAR(128) | 路由路径 |
| component | VARCHAR(128) | 前端组件路径 |
| icon | VARCHAR(32) | 图标 |
| sort_order | INT DEFAULT 0 | |
| hidden | SMALLINT DEFAULT 0 | 0显示 1隐藏 |
| status | SMALLINT DEFAULT 1 | |

#### sys_user_role（用户-角色关联）

| 字段 | 类型 |
|------|------|
| user_id | BIGINT NOT NULL |
| role_id | BIGINT NOT NULL |
| PRIMARY KEY | (user_id, role_id) |

#### sys_role_permission（角色-权限关联）

| 字段 | 类型 |
|------|------|
| role_id | BIGINT NOT NULL |
| permission_id | BIGINT NOT NULL |
| PRIMARY KEY | (role_id, permission_id) |

#### sys_role_menu（角色-菜单关联）

| 字段 | 类型 |
|------|------|
| role_id | BIGINT NOT NULL |
| menu_id | BIGINT NOT NULL |
| PRIMARY KEY | (role_id, menu_id) |

#### sys_log（操作日志表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK 自增 | |
| user_id | BIGINT | 操作人ID |
| username | VARCHAR(32) | 操作人登录名（冗余，防用户被删后日志丢失） |
| module | VARCHAR(32) | 模块 |
| action | VARCHAR(64) | 操作 |
| method | VARCHAR(128) | 请求方法 |
| params | TEXT | 请求参数（脱敏后） |
| result | TEXT | 返回结果摘要 |
| ip | VARCHAR(45) | 操作IP |
| duration | INT | 耗时（ms） |
| status | SMALLINT DEFAULT 1 | 1成功 0失败 |
| created_at | TIMESTAMP DEFAULT CURRENT_TIMESTAMP | |

### 6.3 索引

```sql
CREATE INDEX idx_sys_user_username  ON sys_user(username);
CREATE INDEX idx_sys_user_dept_id   ON sys_user(dept_id);
CREATE INDEX idx_sys_user_status    ON sys_user(status);
CREATE INDEX idx_sys_log_user_id    ON sys_log(user_id);
CREATE INDEX idx_sys_log_created_at ON sys_log(created_at);
```

### 6.4 模拟数据

- 4 个部门（总公司、检测部、质控部、综合部）
- 4 个角色（ROLE_ADMIN、ROLE_MANAGER、ROLE_SAMPLER、ROLE_ANALYST）
- 4 个用户（admin / liming / wangfang / zhaoqiang，密码均为 BCrypt 加密的 "123456"）
- 基础权限（system:user, system:user:add, system:user:edit, system:user:del）
- 基础菜单（系统管理 → 用户管理 / 角色管理）
- admin 拥有 ROLE_ADMIN 角色及所有权限

---

## 七、编码规范

以下规范部分来自团队通用 CLAUDE.md，部分针对本项目技术栈定制。**标注"强制"的必须遵守。**

### 7.1 Service 层（强制）

- 自定义 Service 接口，**不继承** MyBatis-Plus `IService`
- 实现类放在 `impl/` 子包，**不继承** `ServiceImpl`，内部持有 Mapper
- 每个公开方法职责单一，禁止"万能 Service"

### 7.2 MyBatis-Plus 查询（强制）

- **必须使用 `LambdaQueryWrapper` + 方法引用**，禁止字符串字段名
- ✅ `.eq(User::getUsername, username)`
- ❌ `.eq("username", username)`

### 7.3 参数校验（强制）

- 统一使用 Jakarta Validation（`@Valid` + `@NotNull` / `@NotBlank` / `@Size` 等）
- 校验在 Controller 层完成，不混入业务逻辑

### 7.4 枚举（强制）

- 禁止魔法数字，一律用枚举的 getter 方法
- ✅ `StatusEnum.ENABLED.getValue()`
- ❌ `if (status == 1)` 散落各处

### 7.5 分页

- 入参统一下限 `@Min(1)`，上限 `@Max(200)` 硬限制
- 出参统一 `PageVO<T>`，不直接暴露 MyBatis-Plus `IPage`

### 7.6 JSON

- 使用 Jackson（Spring Boot 默认），禁止引入 fastjson
- 序列化 `non_null`，不返回空字段

### 7.7 时间

- 实体层使用 `LocalDateTime`（Java 17 标准时间 API）
- 数据库使用 `TIMESTAMP`
- 全局序列化格式 `yyyy-MM-dd HH:mm:ss`

### 7.8 日志

- 关键链路打日志：入口参数、异常分支
- 占位符风格：`log.info("user {} logged in", username)`，禁止 `+` 拼接
- 禁止打印密码、密钥等敏感信息
- 级别：DEBUG（调试）、INFO（关键节点）、WARN（业务异常）、ERROR（系统异常）

### 7.9 异常处理

- 不吞异常：catch 后必须记录日志或重新抛出，禁止空 catch
- 事务回滚：注意 Spring 默认只对 RuntimeException 回滚

### 7.10 数据库

- 事务范围最小化：事务方法内不放 RPC / HTTP / MQ 操作
- 逻辑删除：所有业务表必须包含 `deleted` 字段

### 7.11 安全

- 越权校验：每个接口校验当前用户是否有权操作目标资源
- SQL 注入防护：使用参数化查询，禁止拼接 SQL
- 外部输入不可信：金额、ID 等关键参数后端二次校验

### 7.12 import

- import 顶部声明，代码行中不出现全限定类名

---

## 八、System 模块接口清单

| 模块 | 基础 URL | 主要接口 |
|------|----------|----------|
| 认证 | `/auth` | POST login / POST refresh / POST logout / GET me |
| 用户 | `/system/users` | CRUD + 分页 |
| 角色 | `/system/roles` | CRUD + 分页 + 分配权限/菜单 |
| 部门 | `/system/depts` | CRUD + 树形列表 |
| 菜单 | `/system/menus` | CRUD + 树形列表 |
| 权限 | `/system/permissions` | CRUD + 分页 |
| 日志 | `/system/logs` | 分页列表 |

---

## 九、初始化脚本

### 9.1 SQL 执行顺序

`resources/db/init.sql` 按顺序包含：

1. 建表：sys_dept → sys_user → sys_role → sys_permission → sys_menu
2. 建关联表：sys_user_role → sys_role_permission → sys_role_menu
3. 建日志表：sys_log
4. 建索引
5. 插入模拟数据

### 9.2 Mapper XML 规范

- 简单 CRUD：0 个 XML（MyBatis-Plus BaseMapper 自动生成）
- 复杂连表查询：`resources/mapper/{模块}/{Entity}Mapper.xml`
- 统计汇总：XML 手写 SQL

---

## 十、配置类清单

| 类 | 包路径 | 职责 |
|----|--------|------|
| `MyBatisPlusConfig` | `common/config` | 分页插件注册、分页上限 200 |
| `RedisConfig` | `common/config` | RedisTemplate JSON 序列化配置 |
| `WebMvcConfig` | `common/config` | 日期格式化、CORS 跨域 |
| `Knife4jConfig` | `common/config` | OpenAPI 文档标题/版本 |
| `SecurityConfig` | `security/config` | Spring Security 过滤器链、BCrypt、@EnableMethodSecurity |
| `JwtAccessTokenProperties` | `security/jwt` | `jwt.access-token.*` 配置绑定 |
| `JwtRefreshTokenProperties` | `security/jwt` | `jwt.refresh-token.*` 配置绑定 |

---

## 十一、开发工具

| 工具 | 用途 | 访问地址 |
|------|------|----------|
| Knife4j | API 在线调试 | `/doc.html` |
| DevTools | 热部署 | 开发阶段自动 |
| SpringDoc | OpenAPI 3 JSON | `/v3/api-docs` |
