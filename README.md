# MCMIS — Marine Center Monitoring Information System

基于 Spring Boot 3.5.16 的**前后端分离单体**海洋中心监测信息系统后端项目。

## 技术栈

| 组件 | 版本/说明 |
|------|-----------|
| JDK | 17 |
| Spring Boot | 3.5.16 |
| Spring Security | 6.5.x（Spring Boot 托管） |
| MyBatis-Plus | 3.5.12 |
| 数据库 | IvorySQL 5.4（兼容 PostgreSQL） |
| 缓存 | Redis 7.x |
| JWT | java-jwt 4.5.0（Auth0） |
| API 文档 | SpringDoc 2.8.0 + Swagger UI |
| 对象映射 | MapStruct 1.6.3 |
| 分页 | PageHelper 2.1.0 |
| 工具库 | Commons Lang3 |

## 快速开始

### 环境准备

- JDK 17+
- Maven 3.8+
- IvorySQL（PostgreSQL）运行在 `127.0.0.1:5432`，用户 `ivorysql` / 密码 `123456`
- Redis 运行在 `127.0.0.1:6380`，密码 `123456`

### 初始化数据库

在 IvorySQL 中创建数据库 `mcmis`，然后执行初始化脚本：

```bash
psql -h 127.0.0.1 -U ivorysql -d mcmis -f src/main/resources/db/init.sql
```

如果数据库已经执行过旧版初始化脚本，请额外执行升级补丁：

```bash
psql -h 127.0.0.1 -U ivorysql -d mcmis -f src/main/resources/db/patch/2026-07-16-security-hardening.sql
psql -h 127.0.0.1 -U ivorysql -d mcmis -f src/main/resources/db/patch/2026-07-17-frontend-integration.sql
psql -h 127.0.0.1 -U ivorysql -d mcmis -f src/main/resources/db/patch/2026-07-18-user-permission.sql
psql -h 127.0.0.1 -U ivorysql -d mcmis -f src/main/resources/db/patch/2026-07-18-permission-followup.sql
psql -h 127.0.0.1 -U ivorysql -d mcmis -f src/main/resources/db/patch/2026-07-19-welcome-menu.sql
```

模拟数据包含 4 个部门、4 个角色、4 个用户（密码均为 BCrypt 加密的 `123456`）。

### 启动项目

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 访问 API 文档

启动后访问：`http://localhost:8080/swagger-ui.html`

Swagger UI 提供在线 API 调试界面。调试需鉴权的接口时，先调用 `/auth/login` 获取 Token，然后点击页面右上角 **Authorize** 按钮填入 AccessToken（不含 Bearer 前缀），后续所有请求将自动携带 `Authorization` 请求头。

生产环境必须显式激活 `prod` Profile，并提供 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、
`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`、`JWT_ACCESS_SECRET` 和
`CORS_ALLOWED_ORIGINS` 环境变量。`CORS_ALLOWED_ORIGINS` 使用逗号分隔多个前端来源。

---

## 项目结构

```
src/main/java/com/shou/mcmis/
├── McmisApplication.java              # 启动入口
├── common/                            # 公共模块
│   ├── config/           # MyBatisPlus、Redis、WebMvc、Knife4j 配置
│   ├── constant/         # 全局常量（Redis Key 前缀等）
│   ├── entity/           # 通用实体基类 BaseEntity
│   ├── enums/            # StatusEnum、GenderEnum 等枚举
│   ├── exception/        # 业务异常体系 + 全局异常处理
│   ├── response/         # Result<T>、PageVO<T>、CursorPageVO<T>
│   ├── cache/            # CacheService（Redis 操作封装）
│   ├── util/             # SecurityUtils 等工具类
│   └── validation/       # @Phone 等自定义校验注解
│
├── security/                         # 安全模块（JWT + Spring Security）
│   ├── config/           # SecurityConfig
│   ├── jwt/              # JwtTokenService、RefreshTokenService、配置属性
│   ├── filter/           # JwtAuthFilter
│   ├── handler/          # 登录成功/失败/权限拒绝处理器
│   ├── service/          # SecurityUserDetails、RsaKeyService、AuthService
│   ├── controller/       # AuthController（登录/刷新/注销/公钥）
│   ├── dto/              # LoginRequest、RefreshRequest
│   └── vo/               # LoginVO、UserInfoVO
│
├── organize/                         # 组织架构管理（RBAC）
│   ├── user/             # 用户管理 → /system/users
│   ├── userpermission/   # 用户直接权限、最终权限、权限审计与强制下线
│   ├── role/             # 角色管理 → /system/roles
│   ├── permission/       # 权限管理 → /system/permissions
│   ├── dept/             # 部门管理 → /system/depts
│   ├── menu/             # 菜单管理 → /system/menus
│   └── log/              # 操作日志 → /system/logs（含 @Log 注解 + LogAspect）
│
├── sample/              # 样品管理（空骨架）
├── project/             # 项目管理（空骨架）
├── task/                # 检测任务（空骨架）
├── instrument/          # 仪器设备（空骨架）
├── report/              # 报告管理（空骨架）
├── customer/            # 客户管理（空骨架）
├── contract/            # 合同管理（空骨架）
├── workflow/            # 审批流程（空骨架）
├── notification/        # 消息通知（空骨架）
└── file/                # 文件上传（空骨架）
```

### 模块内部结构规范

每个业务模块（如 `organize/user`）内部标准分层：

```
{module}/
├── controller/          # RESTful 接口，仅负责参数接收、校验、调用 Service
├── service/             # Service 接口（不继承 MyBatis-Plus IService）
│   └── impl/            # Service 实现（内部持有 Mapper）
├── mapper/              # MyBatis-Plus BaseMapper 接口
├── entity/              # 数据库实体，继承 BaseEntity
├── dto/                 # CreateDTO / UpdateDTO / QueryDTO
├── vo/                  # 视图对象
└── converter/           # MapStruct 转换器（componentModel = "spring"）
```

---

## API 接口概览

### 认证接口 `/auth`

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/auth/public-key` | 获取 RSA 公钥（用于前端加密登录密码） | 公开 |
| POST | `/auth/login` | 登录（加密密码传输）| 公开 |
| POST | `/auth/refresh` | 刷新令牌（需携带 Authorization Header，可为已过期的 AccessToken）| 公开 |
| POST | `/auth/logout` | 注销 | 需登录 |
| GET | `/auth/me` | 获取当前用户信息 | 需登录 |
| GET | `/auth/menus` | 按当前用户最终有效权限获取 ProLayout 菜单树 | 需登录 |

### 系统管理接口 `/system/*`

| 模块 | 基础路径 | 主要接口 |
|------|----------|----------|
| 用户管理 | `/system/users` | 分页查询、详情、新增、编辑、批量删除 |
| 角色管理 | `/system/roles` | CRUD + `POST /{id}/permissions` + `POST /{id}/menus` |
| 权限管理 | `/system/permissions` | CRUD + 分页 |
| 部门管理 | `/system/depts` | CRUD + `GET /tree`（树形结构） |
| 菜单管理 | `/system/menus` | CRUD + `GET /tree`（树形结构） |
| 操作日志 | `/system/logs` | 分页查询（只读，游标分页） |
| 用户直接权限 | `/system/users` | 直接授权、最终权限、权限审计、强制下线 |

---

## 前端对接约定

### 1. 统一响应格式

所有接口返回统一结构 `Result<T>`：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数校验失败 |
| 401 | 未登录或 Token 已过期 |
| 403 | 无访问权限 |
| 404 | 资源不存在 |
| 409 | 数据冲突（如用户名已存在） |
| 500 | 服务器内部错误 |

### 2. 认证流程

采用 **RSA 加密 + JWT 双 Token** 方案：

1. 前端加载登录页 → 调用 `GET /auth/public-key` 获取 RSA 公钥和 keyId
2. 前端用公钥加密密码 → 调用 `POST /auth/login` 传入 `{ username, cipherPwd, keyId }`
3. 后端解密 → BCrypt 验证 → 返回 `{ accessToken, refreshToken, expiresIn: 900 }`
4. 前端存储双 Token → 后续请求在 Header 中携带 `Authorization: Bearer <accessToken>`
5. AccessToken 过期（15 分钟）→ 前端调用 `POST /auth/refresh`，Header 携带已过期的 AccessToken，Body 传 `{ refreshToken }`，换取新双 Token
6. RefreshToken 过期（7 天）或 RefreshToken 校验失败 → 跳转登录页

### 3. 分页规范

**偏移分页**（管理后台列表，数据量 < 10 万行）：

```json
// 请求
GET /system/users?pageNum=1&pageSize=20&username=admin

// 响应
{
  "code": 200,
  "data": {
    "records": [...],
    "total": 100,
    "pageNum": 1,
    "pageSize": 20,
    "totalPages": 5
  }
}
```

- `pageNum`：从 1 开始，默认 1
- `pageSize`：1-200，默认 20，硬上限 200
- 支持跳页

**游标分页**（数据量大或无限滚动场景，如操作日志）：

```json
// 请求
GET /system/logs?lastId=&pageSize=20

// 响应
{
  "code": 200,
  "data": {
    "records": [...],
    "nextCursor": 42,
    "hasMore": true
  }
}
```

- 首次请求 `lastId` 不传或传空
- 后续请求传入上页返回的 `nextCursor`
- `hasMore: false` 表示已是最后一页

### 4. 权限控制

接口使用 Spring Security `@PreAuthorize` 进行权限校验，前端需配合实现：

- **菜单权限**：根据用户拥有的菜单列表渲染侧边栏
- **按钮权限**：根据权限标识控制按钮显隐（如 `organize:user:add` 控制"新增用户"按钮）
- **路由守卫**：未登录跳转登录页，无权限跳转 403 页

权限标识命名规范：`{模块}:{资源}:{操作}`，如：
- `organize:user` — 用户管理菜单
- `organize:user:add` — 新增用户按钮
- `organize:user:edit` — 编辑用户按钮
- `organize:user:del` — 删除用户按钮

权限系统已从单一角色权限扩展为“角色权限 + 用户直接权限 + 最终有效权限”模型。运行时鉴权、当前用户信息和授权菜单均使用同一份最终有效权限快照，避免前端菜单、按钮与后端接口权限不一致。

#### 4.1 最终有效权限模型

普通用户的权限按以下顺序计算：

```text
角色权限
  + 当前时间段内生效的用户 ALLOW
  + ALLOW 所需的依赖权限
  - 当前时间段内生效的用户 DENY
  - 因依赖权限缺失而不可用的操作权限
= 最终有效权限
```

- `ALLOW = 1`：在角色权限之外直接增加权限。
- `DENY = 2`：从角色权限和直接 ALLOW 的合并结果中移除权限，DENY 优先。
- `add`、`edit`、`del`、`audit`、`force-logout` 等操作权限依赖同资源的 `list` 权限。
- `organize:user-permission:list` 显式依赖 `organize:user:list`。
- 如果列表权限被 DENY，依赖该列表权限的操作权限也会从最终结果中移除。
- 禁用或已删除的权限不会进入最终有效权限。

`GET /auth/me` 返回当前用户的 `roles`、最终 `permissions` 和 `nextPermissionBoundary`。JWT 中虽然包含签发时的权限信息，但每个已认证请求仍会根据数据库和 Redis 中的最新最终权限重新构建 Spring Security Authorities，因此权限变更不依赖旧 JWT 中的权限声明。

#### 4.2 超级管理员保护

`ROLE_ADMIN` 是系统最高权限角色，并受到以下保护：

- 超级管理员始终拥有所有启用的权限。
- 禁止为超级管理员创建或修改为 DENY 的用户直接权限。
- `ROLE_ADMIN` 角色不能禁用或删除。
- 系统必须至少保留一个启用的超级管理员账号；不能删除、禁用最后一个超级管理员，也不能移除其最高权限角色。
- 暂未引入 break-glass 账号，最高权限可用性由上述约束保证。

其他角色和普通用户允许配置 DENY。

#### 4.3 定时直接授权

用户直接权限存储于 `sys_user_permission`，支持永久授权和提前安排多段固定授权：

| 字段 | 说明 |
|------|------|
| `permission_id` | 被直接配置的权限 |
| `effect` | `1` 为 ALLOW，`2` 为 DENY |
| `valid_from` | 生效时间；为空表示立即生效 |
| `expires_at` | 失效时间；为空表示长期有效 |
| `reason` | 授权或变更原因，必填，最多 256 字符 |
| `grant_by` | 实际操作人 |
| `version` | 乐观锁版本号 |

- 时间区间采用左闭右开 `[valid_from, expires_at)`。
- 底层存储和权限判断按秒级精度执行，纳秒会被截断；管理界面可以按分钟输入。
- 对同一用户、同一权限，所有未删除授权的时间段不得重叠，包括 ALLOW 与 DENY 之间的重叠。
- 无界时间使用负无穷或正无穷参与区间判断。
- 服务层会预检查重叠，数据库同时使用 `btree_gist` 与 `EXCLUDE USING gist` 约束阻止并发写入造成的重叠。
- 新增、编辑和撤销均要求填写原因；编辑支持 `version` 乐观锁检查。

请求中的时间建议使用 ISO LocalDateTime，例如：

```json
{
  "permissionId": 12,
  "effect": 1,
  "validFrom": "2026-07-21T09:00:00",
  "expiresAt": "2026-07-21T18:00:00",
  "reason": "临时承担部门维护工作"
}
```

#### 4.4 权限缓存与自动失效

最终权限快照使用 Redis 缓存，Key 前缀为 `authz:effective:v2:`：

- 常规缓存最长 300 秒。
- 存在未来生效或失效时间时，TTL 会缩短到最近的 `nextPermissionBoundary`。
- 到达边界后缓存自动过期，下一请求按新的时间状态重新计算权限。
- 新增、编辑、撤销直接授权，以及用户角色、角色权限等关联变化后，会在事务提交后主动清理受影响用户的权限缓存。
- Redis 读取或写入异常时会记录警告并回源数据库，不以缓存可用性作为鉴权正确性的前提。

#### 4.5 菜单可见性

`GET /auth/menus` 返回与 Ant Design Pro `MenuDataItem` 对齐的树形结构，字段包括 `key`、`name`、`path`、`icon`、`hideInMenu` 和 `children`。

菜单可见性根据最终有效权限计算：

- 设置了 `required_permission_id` 的叶子菜单，仅在对应权限存在于最终有效权限时显示。
- 目录菜单不会因为自身没有权限码而无条件显示；只有至少一个子菜单可见时才连同祖先目录一起返回。
- 没有权限要求的普通叶子菜单可以显示，例如欢迎页。
- 禁用菜单不会出现在当前用户菜单中。
- `/system/menus/tree` 是后台菜单管理树，会包含禁用项，不应与当前用户授权菜单混用。

菜单记录必须绑定正确的列表权限，例如用户管理菜单绑定 `organize:user:list`。不能仅根据角色菜单关联或某一条 ALLOW/DENY 记录机械决定显示状态。

#### 4.6 独立权限审计

权限变更除写入通用操作日志 `sys_log` 外，还会写入独立的 `sys_user_permission_audit`：

- 审计操作包括 `CREATE`、`UPDATE` 和 `REVOKE`。
- 保存变更前后的 JSON 快照、必填原因、操作人 ID、操作人用户名、请求 ID、IP 和操作时间。
- 撤销直接授权采用逻辑删除，但审计记录保持独立，不随授权记录删除。
- 权限审计通过独立权限 `organize:user-permission:audit` 控制。

权限系统当前不包含审批流，授权操作在通过权限校验和数据约束后立即生效或按计划生效。

#### 4.7 强制下线

强制下线是独立于“禁用用户”的安全操作：

- 调用后递增用户的 `auth_version`。
- 撤销该用户的 RefreshToken。
- 清理该用户的最终权限缓存。
- 已签发 AccessToken 中的 `authVersion` 与数据库不一致后立即失效。
- 操作必须提供原因，并受 `organize:user:force-logout` 权限控制。

禁用用户仍会阻止登录并撤销刷新凭证；强制下线用于在不改变用户启用状态的情况下终止全部现有会话。

#### 4.8 用户直接权限接口

所有接口均位于 `/system/users` 下：

| 方法 | 路径 | 说明 | 所需权限 |
|------|------|------|----------|
| GET | `/permission-options` | 查询可直接授权的启用权限 | `organize:user-permission:list`、`organize:user-permission:add`、`organize:user-permission:edit` 任一 |
| GET | `/{userId}/permission-grants` | 查询用户全部直接授权时段 | `organize:user-permission:list` |
| GET | `/{userId}/permissions/effective` | 查询角色、ALLOW、DENY 和最终权限 | `organize:user-permission:list` |
| POST | `/{userId}/permission-grants` | 新增直接授权 | `organize:user-permission:add` |
| PUT | `/{userId}/permission-grants/{grantId}` | 编辑直接授权 | `organize:user-permission:edit` |
| DELETE | `/{userId}/permission-grants/{grantId}` | 撤销直接授权，Body 必须包含原因 | `organize:user-permission:del` |
| GET | `/{userId}/permission-grants/audit` | 查询权限审计记录 | `organize:user-permission:audit` |
| POST | `/{userId}/force-logout` | 强制用户全部会话下线 | `organize:user:force-logout` |

#### 4.9 数据库升级说明

全新数据库直接执行 `src/main/resources/db/init.sql`，已包含权限系统的完整表结构、约束、权限和菜单种子。

旧数据库按 README“初始化数据库”章节中的顺序执行补丁。其中：

- `2026-07-18-user-permission.sql` 创建 `btree_gist` 扩展、直接权限表、独立审计表、菜单权限外键和 `auth_version`。
- `2026-07-18-permission-followup.sql` 校验菜单权限外键约束。
- `2026-07-19-welcome-menu.sql` 增加所有已登录用户可见的欢迎页，并规范系统管理菜单图标和排序。

IvorySQL 必须支持并安装 `btree_gist`。如果数据库账号没有创建扩展的权限，应由数据库管理员预先安装后再执行补丁。

### 5. 请求/响应约定

- **Content-Type**：`application/json`
- **时间格式**：`yyyy-MM-dd HH:mm:ss`（字符串，非时间戳）
- **空值处理**：序列化时忽略 null 字段（`non_null`）
- **HTTP 方法**：GET（查询）、POST（新增）、PUT（修改）、DELETE（删除）
- **批量删除**：`DELETE` 请求体传 ID 数组 `[1, 2, 3]`

---

## 新模块开发指南

### 开发流程

按照项目规范的 **9 步流程**：

1. `superpowers:brainstorming` — 产出设计文档
2. `superpowers:writing-plans` — 生成实现计划
3. `superpowers:test-driven-development` — 先写测试
4. `superpowers:executing-plans` — 按阶段执行
5. `superpowers:verification-before-completion` — 完成前验证
6. `superpowers:requesting-code-review` — 代码审查
7. 输出技术细节 → `docs/technical_decisions/`
8. 输出学习总结 → Memory
9. 提交并推送

### 以新增"样品管理"模块为例

**Step 1：创建数据库表**

```sql
CREATE TABLE IF NOT EXISTS sam_sample (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sample_code VARCHAR(32) NOT NULL UNIQUE,     -- 样品编号
    name        VARCHAR(128) NOT NULL,            -- 样品名称
    type        VARCHAR(32),                      -- 样品类型
    status      SMALLINT DEFAULT 1,               -- 状态
    -- 通用审计字段（必须！）
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    is_delete   SMALLINT DEFAULT 0,
    version     INT DEFAULT 0
);
```

**Step 2：创建 Entity**（继承 BaseEntity）

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sam_sample")
public class Sample extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sampleCode;
    private String name;
    private String type;
    private Integer status;
}
```

**Step 3：创建 Mapper、DTO、VO、Converter**

- DTO 命名规范：`SampleCreateDTO`、`SampleUpdateDTO`、`SampleQueryDTO`（继承 `PageQuery`）
- VO 命名规范：`SampleVO`
- Converter：`SampleConverter`（MapStruct `@Mapper(componentModel = "spring")`）
- Mapper：`SampleMapper extends BaseMapper<Sample>`（简单 CRUD 无需 XML）

**Step 4：创建 Service**

```java
// SampleService.java — 自定义接口，不继承 MyBatis-Plus IService
public interface SampleService {
    PageVO<SampleVO> page(SampleQueryDTO query);
    SampleVO getById(Long id);
    Long create(SampleCreateDTO dto);
    void update(Long id, SampleUpdateDTO dto);
    void delete(List<Long> ids);
}

// SampleServiceImpl.java — 内部持有 Mapper
@Service
@RequiredArgsConstructor
public class SampleServiceImpl implements SampleService {

    private final SampleMapper sampleMapper;
    private final SampleConverter sampleConverter;

    @Override
    public PageVO<SampleVO> page(SampleQueryDTO query) {
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Sample> wrapper = new LambdaQueryWrapper<Sample>()
                .like(StringUtils.isNotBlank(query.getName()), Sample::getName, query.getName())
                .eq(query.getStatus() != null, Sample::getStatus, query.getStatus())
                .eq(Sample::getIsDelete, 0)
                .orderByDesc(Sample::getCreateTime);
        return PageVO.of(new PageInfo<>(sampleMapper.selectList(wrapper))
                .convert(sampleConverter::toVO));
    }
    // ... 其余方法
}
```

**Step 5：创建 Controller**

```java
@RestController
@RequestMapping("/sample/samples")
@RequiredArgsConstructor
@Tag(name = "样品管理")
public class SampleController {

    private final SampleService sampleService;

    @GetMapping
    @Operation(summary = "分页查询样品")
    @PreAuthorize("hasAuthority('sample:sample')")
    public Result<PageVO<SampleVO>> list(@Valid SampleQueryDTO query) {
        return Result.success(sampleService.page(query));
    }
    // ...
}
```

### Controller URL 命名建议

| 模块 | 建议 URL 前缀 |
|------|--------------|
| 样品管理 | `/sample/samples` |
| 项目管理 | `/project/projects` |
| 检测任务 | `/task/tasks` |
| 仪器设备 | `/instrument/instruments` |
| 报告管理 | `/report/reports` |
| 客户管理 | `/customer/customers` |
| 合同管理 | `/contract/contracts` |
| 文件上传 | `/file/files` |

---

## 编码规范（强制）

### Service 层

- ✅ 自定义 Service 接口，**不继承** MyBatis-Plus `IService`
- ✅ 实现类放在 `impl/` 子包，**不继承** `ServiceImpl`，内部持有 Mapper
- ✅ 每个公开方法职责单一

### MyBatis-Plus 查询

- ✅ **必须使用 `LambdaQueryWrapper` + 方法引用**
- ✅ `.eq(User::getUsername, username)`
- ❌ `.eq("username", username)`（禁止字符串字段名）

### 参数校验

- 统一使用 Jakarta Validation：`@Valid` + `@NotNull` / `@NotBlank` / `@Size` 等
- 校验在 Controller 层完成，不混入业务逻辑

### 枚举

- 禁止魔法数字，一律使用枚举：
- ✅ `StatusEnum.ENABLED.getValue()`
- ❌ `if (status == 1)`

### 分页

- 小数据量（< 10 万）：PageHelper 偏移分页
- 大数据量：`lastId` 游标分页（避免深度分页）
- 入参上限 `@Max(200)`

### JSON

- 使用 Jackson（Spring Boot 默认）
- 禁止引入 fastjson

### 时间类型

- 实体层：`LocalDateTime`（禁止使用 `Date`）
- 数据库：`TIMESTAMP WITH TIME ZONE`
- 序列化格式：`yyyy-MM-dd HH:mm:ss`

### 日志

- 占位符风格：`log.info("user {} logged in", username)`（禁止 `+` 拼接）
- 禁止打印密码、密钥
- 操作日志使用 `@Log(module = "样品管理", action = "新增样品")` 注解

### 数据库

- 所有业务表必须包含：`create_time`、`update_time`、`create_by`、`update_by`、`is_delete`、`version`
- 通过 `BaseEntity` 继承 + `MetaObjectHandler` 自动填充
- 频繁变更的表必须设置 `version` 字段（乐观锁）
- 事务范围最小化，不放 RPC / HTTP / MQ

### 安全

- 每个接口校验当前用户是否有权操作目标资源（越权校验）
- SQL 注入防护：使用参数化查询，禁止拼接 SQL
- 密码传输使用 RSA 加密，存储使用 BCrypt

---

## 参考文档

- [基础设施设计文档](docs/superpowers/specs/2026-07-12-mcmis-infrastructure-design.md)
- [实现计划](docs/superpowers/plans/2026-07-12-mcmis-infrastructure-plan.md)
- [开发规范](docs/开发规范.md)
- [开发文档](docs/dev.md)
