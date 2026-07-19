# MCMIS 单元测试设计文档

## 测试架构

分层混合策略，Service/Controller/Security 统一走 `@SpringBootTest` + `@Transactional` 真实链路，Entity/DTO/Converter 纯 JUnit。

| 层 | 测试方式 | 依赖 |
|------|----------|------|
| Entity/DTO/VO/Converter/Validator | 纯 JUnit，不启动 Spring | 无 |
| Service | @SpringBootTest + @Transactional | DB（已有）+ Redis（已有） |
| Controller | @SpringBootTest + MockMvc + @Transactional | DB + Redis |
| Security | @SpringBootTest + @Transactional | DB + Redis |
| GlobalExceptionHandler | @WebMvcTest | 无（已有基础） |

### 测试基类

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public abstract class BaseSpringBootTest {
    @Autowired protected MockMvc mockMvc;
    @Autowired protected PasswordEncoder passwordEncoder;
}

public abstract class BaseAuthenticatedTest extends BaseSpringBootTest {
    protected String adminToken;

    @BeforeEach
    void loginAsAdmin() {
        // POST /auth/login 获取 admin 的 AccessToken
    }
}
```

### Seed 数据复用

测试复用 `init.sql` 的种子数据：

| 数据 | 用途 |
|------|------|
| `admin / 123456` | 全权限用户，鉴权通过场景 |
| `liming / 123456` | 无特殊权限，鉴权拒绝场景 |
| `ROLE_ADMIN` (id=1) | 全权限角色 |
| `sys_dept` (id=1-4) | 部门查询/树形结构 |

---

## 测试用例清单

### 1. 通用层（纯单元测试）

#### 1.1 PageVOTest

```
of() 正常
  - 构造 PageInfo(3条数据, total=10, pageNum=2, pageSize=3)
  - → records.size=3, total=10, pageNum=2, pageSize=3, totalPages=4

of() 空列表
  - 构造 PageInfo(空列表, total=0)
  - → records.size=0, total=0, totalPages=0
```

#### 1.2 BaseEntityTest

```
子类实例化后审计字段存在
  - new User() → createTime=null（由 MetaObjectHandler 填充）
  - @TableLogic 注解存在 → isDelete 字段
  - @Version 注解存在 → version 字段
```

#### 1.3 PhoneValidatorTest

```
合法手机号
  - "13800138000" → isValid() = true

非法输入
  - "1234" → isValid() = false
  - "abcdefghijk" → isValid() = false
  - null → isValid() = true（空值由 @NotNull 处理）
  - "" → isValid() = true
```

#### 1.4 GlobalExceptionHandlerTest（扩充）

```
BusinessException(409)   → code=409, message=指定消息
UnauthorizedException     → code=401, message="未登录或Token已过期"
ForbiddenException        → code=403, message="无访问权限"
AccessDeniedException     → code=403, message="无访问权限"
AuthenticationException   → code=401
DuplicateKeyException     → code=409
MissingRequestHeaderException → code=400
MethodArgumentNotValidException → code=400
HttpMessageNotReadableException → code=400
NoResourceFoundException       → code=404
```

### 2. Security 层

#### 2.1 JwtTokenServiceTest | @SpringBootTest

```
generateAccessToken
  - userId=1, username="admin", permissions=["organize:user","organize:menu"]
  - → 解码后 subject="1", username="admin", permissions 一致
  - → expiresAt 在 ~15min 后

generateAccessToken 空权限
  - permissions=[] → 解码后 permissions=""

verifyAccessToken 合法 Token
  - 用 generateAccessToken 生成的 Token
  - → 返回 DecodedJWT，claims 正确

verifyAccessToken 篡改 Token
  - 修改 payload 内容
  - → 抛出 UnauthorizedException

verifyAccessToken 过期 Token
  - 手动构造过期 Token（或等待...不宜做）
  - → 抛出 UnauthorizedException

generateRefreshToken
  - → 64 个 hex 字符（256 bit）

extractUserId
  - 合法 Token → 返回正确 userId
  - 不验证签名/过期也能正确提取
```

#### 2.2 AuthServiceTest | @SpringBootTest

```
login 成功
  - username="admin", rawPassword="123456"
  - → LoginVO: accessToken 非空, refreshToken 非空, expiresIn=900
  - → Redis 中存在 refresh_token:1

login 密码错误
  - rawPassword="wrong"
  - → AuthenticationException → 401

login 用户不存在
  - username="nobody"
  - → AuthenticationException → 401

login 用户被禁用
  - 先创建并禁用一用户, 用其登录
  - → AuthenticationException → 401

refresh 正常刷新
  - login 获取 accessToken + refreshToken
  - 调用 refresh(accessToken, refreshToken)
  - → 新 accessToken 非空, 新 refreshToken 非空
  - → 旧 refreshToken 已从 Redis 删除

refresh 错误 refreshToken
  - 用错误 refreshToken 刷新
  - → UnauthorizedException

refresh 重复刷新（防重放）
  - 用 refreshToken 刷新一次成功
  - 用同一个 refreshToken 再次刷新
  - → UnauthorizedException

logout
  - login 获取 accessToken + refreshToken
  - 将 accessToken 注入 SecurityContext
  - 调用 logout
  - → Redis 中 refresh_token 已删除
  - → 再次 refresh 返回 UnauthorizedException
```

#### 2.3 AuthControllerTest | @SpringBootTest + MockMvc

```
POST /auth/login 成功
  - body: {"username":"admin","cipherPwd":"123456"}（dev 环境明文）
  - → status=200, code=200, data.accessToken 非空

POST /auth/login 密码错误
  - body: {"username":"admin","cipherPwd":"wrong"}
  - → status=200, code=401

POST /auth/login 缺少参数
  - body: {"username":"admin"}（无 cipherPwd）
  - → status=200, code=400

GET /auth/public-key
  - → status=200, code=200, data.keyId 非空, data.publicKey 非空

POST /auth/refresh
  - 先 login 获取双 Token
  - Header: Authorization Bearer <旧accessToken>
  - body: {"refreshToken":"<旧refreshToken>"}
  - → status=200, code=200, data 有新双 Token

POST /auth/logout
  - 用 admin 的 Token
  - Header: Authorization Bearer <accessToken>
  - → status=200, code=200

GET /auth/me
  - Header: Authorization Bearer <accessToken>
  - → status=200, code=200, data.username="admin"
```

#### 2.4 JwtAuthFilterTest | @SpringBootTest + MockMvc

```
无 Authorization Header 请求公开接口
  - GET /auth/public-key
  - → 放行, 正常返回

无 Authorization Header 请求鉴权接口
  - GET /system/users
  - → 401

携带有效 Token 请求鉴权接口
  - Authorization: Bearer <adminToken>
  - GET /system/users
  - → 正常返回

携带过期 Token 请求鉴权接口
  - 用手动构造的过期 Token
  - → 401

携带有效 Token 但无权限
  - 用 liming 的 Token 访问 system:menu:add
  - → 403
```

### 3. Service 层（@SpringBootTest + @Transactional + 真实 Mapper）

#### 3.1 UserServiceImplTest

```
page 正常分页
  - query: pageNum=1, pageSize=2
  - → total>=4, records.size=2

page 按用户名模糊查询
  - query: username="admin"
  - → total=1, records[0].username="admin"

page 按状态过滤
  - query: status=1
  - → 所有记录 status=1, total>=4

getById 存在
  - id=1(admin)
  - → username="admin"

getById 不存在
  - id=9999
  - → NotFoundException

create
  - 提供所有必填字段
  - → 返回新 userId, DB 中存在该记录

create 用户名重复
  - username="admin"
  - → BusinessException(409)

update
  - id=2(liming), 修改 realName
  - → 查询后 realName 已变更

update 不存在
  - id=9999
  - → NotFoundException

delete
  - ids=[2]
  - → 查询返回 null（逻辑删除）

delete 空列表
  - ids=[]
  - → 无异常（MyBatis-Plus 不做任何事）
```

#### 3.2 RoleServiceImplTest

```
page 正常
getById 存在/不存在
create 正常/名称重复
update 正常/不存在
delete 正常
assignPermissions
  - roleId=2, permissionIds=[3,4]
  - → 关联表存在 (2,3) 和 (2,4)
  - → 再次 assign 新列表 [5] → 旧关联 (3,4) 已删除
assignMenus（同上模式）
```

#### 3.3 DeptServiceImplTest

```
getTree
  - → 返回树形结构, 根节点 parentId=null/0
  - → children 包含子部门

create
  - 提供 parentId=1
  - → 新部门在树形中 child 列表出现
```

#### 3.4 MenuServiceImplTest

```
getTree → 返回顶层菜单, 子菜单在 children 中
```

#### 3.5 PermissionServiceImplTest

```
page 按 code 过滤
  - code="organize:user"
  - → total=1
```

#### 3.6 LogServiceImplTest

```
page 游标分页首屏
  - lastId=null, pageSize=2
  - → records.size=2, hasMore=true, nextCursor 非空

page 游标分页翻页
  - lastId=上页nextCursor
  - → 下页数据正确

page hasMore=false 末页
  - → hasMore=false, nextCursor=null

page 按时间范围过滤
  - startTime/endTime
  - → 仅返回区间内记录
```

### 4. Controller 层（@SpringBootTest + MockMvc + @Transactional）

#### 4.1 通用模式

每个 Controller 测试继承 `BaseAuthenticatedTest`，自动以 admin 登录。测试覆盖：

```
GET /{resource}?pageNum=1&pageSize=10  → 200 + PageVO
GET /{resource}/{id} 存在              → 200 + VO
GET /{resource}/{id} 不存在             → 404
POST /{resource} 正常参数               → 200 + id
POST /{resource} 缺失必填字段            → 400
POST /{resource} 唯一键冲突              → 409
PUT /{resource}/{id} 正常               → 200
PUT /{resource}/{id} 不存在             → 404
DELETE /{resource} [ids] 正常           → 200
无 Token 请求                            → 401
无权限 Token 请求（如 liming 访 admin 专属）→ 403
```

#### 4.2 控制器清单

- **UserControllerTest** — /system/users 全套
- **RoleControllerTest** — /system/roles + assignPermissions + assignMenus
- **DeptControllerTest** — /system/depts + getTree
- **MenuControllerTest** — /system/menus + getTree
- **PermissionControllerTest** — /system/permissions
- **LogControllerTest** — /system/logs（游标分页特有参数）

### 5. AOP 层

#### 5.1 LogAspectTest | @SpringBootTest

```
@Log 注解方法调用
  - 执行被标记的方法
  - → sys_log 表插入一条记录
  - → module/action/username 正确
  - → duration >= 0

@Log 注解方法抛异常
  - 方法抛出 RuntimeException
  - → sys_log.status=0, result 含异常消息

@Log 序列化失败（如参数含不可序列化对象）
  - → sys_log.params="[serialization failed: ...]"
  - → 方法正常执行（日志不中断业务）
```

---

## 实施顺序

1. **基础层** — BaseSpringBootTest、BaseAuthenticatedTest 基类
2. **通用层** — PageVOTest、PhoneValidatorTest、GlobalExceptionHandlerTest 扩充
3. **Security 层** — JwtTokenServiceTest → AuthServiceTest → AuthControllerTest → JwtAuthFilterTest
4. **Service 层** — User → Role → Dept → Menu → Permission → Log
5. **Controller 层** — User → Role → Dept → Menu → Permission → Log
6. **AOP 层** — LogAspectTest

每层完成后运行 `./mvnw test` 确认全绿再进下一层。
