# 开发文档

本项目为一个业务复杂的单体项目，采用前后端分离架构，类型为环境检测的体制项目。现在需要你作为一名专业架构师严格制定计划，完善这个开发文档，补充具体细节，目的是构造一个具备基础功能用于后续其他模块功能开发的基础项目。

## 一、技术选型
### 1. jdk17
### 2. Spring Boot 3.5.16 + mybatis plus
### 3. 数据库：IvorySQL
### 4. Redis
### 5. 安全框架：Spring Security也相应为6.5.11
### 6. 用户鉴权：JWT：Java JWT
### 7. 工作流引擎：Dromara Warm-Flow
### 8. API文档：knife4j
### 9. 工具库：MapStruct，Guava + Commons Lang3 + Commons Collections4等



## 二、任务

以下任务均建立在前后端分离的单体项目架构

### 1. 根据技术选型完成环境配置
    （1）redis：127.0.0.1:6380 密码：123456
    （2）IvorySQL：127.0.0.1:5432 用户名：ivorysql 密码：123456
### 2. 项目结构设计

业务复杂但仍采用单体架构，推荐采用**按业务模块组织代码**，在每个模块内部再进行分层

推荐结构：

```
src/main/java
└── com.company.lims
    ├── common                 // 公共模块
    │   ├── config
    │   ├── constant
    │   ├── enums
    │   ├── exception
    │   ├── response
    │   ├── util
    │   ├── validation
    │   └── cache
    │
    ├── security               // JWT、Spring Security
    │   ├── jwt
    │   ├── filter
    │   ├── handler
    │   ├── service
    │   └── config
    │
    ├── system                 // 系统管理
    │   ├── user
    │   ├── role
    │   ├── permission
    │   ├── dept
    │   ├── menu
    │   └── log
    │
    ├── sample                 // 样品管理
    │
    ├── project                // 项目管理
    │
    ├── task                   // 检测任务
    │
    ├── instrument             // 仪器设备
    │
    ├── report                 // 报告管理
    │
    ├── customer               // 客户
    │
    ├── contract               // 合同
    │
    ├── workflow               // 审批流程
    │
    ├── notification           // 消息通知
    │
    ├── file                   // 文件上传
    │
    └── LimsApplication.java
```

每个业务模块内部：

例如 Sample：

```
sample
├── controller
├── service
│   ├── SampleService
│   └── impl
├── mapper
├── entity
├── dto
├── vo
├── converter
├── repository（可选）
└── event（可选）
```

注意上面示例的其他业务模块还未确定暂时不用真正添加

DTO、VO 不建议放公共目录。common 只放真正公共的东西，Security 独立，数据库建议同步分模块，Service 不要写成"万能 Service"，可以内部拆分，保持每个公开方法职责清晰。

controller层全部采用 RESTful 风格，遵循 **RESTful + 统一返回体 + DTO 入参 + VO 出参 + 分页统一封装** 的规范

类似：

```
@GetMapping("/{id}")
public Result<UserVO> getById(@PathVariable Long id) {
	return Result.success(userService.getById(id));
}

@PostMapping
public Result<Long> create(@Valid @RequestBody UserCreateDTO dto) {
	return Result.success(userService.create(dto));
}
```

| 规范            | 推荐做法                                                     |
| --------------- | ------------------------------------------------------------ |
| URL 风格        | RESTful（`GET /users/{id}`、`POST /users`）                  |
| Controller 职责 | 仅负责参数接收、校验、调用 Service、返回结果                 |
| 入参            | `CreateDTO`、`UpdateDTO`、`QueryDTO` 等，不直接接收 Entity   |
| 出参            | 返回 `VO`，不直接返回 Entity                                 |
| 返回格式        | 统一 `Result<T>`                                             |
| 参数校验        | `@Valid` + Jakarta Validation，配合全局异常处理              |
| 分页            | 使用统一分页 DTO/VO，不直接暴露 MyBatis-Plus `Page`          |
| 事务            | 只放在 Service 层                                            |
| MyBatis-Plus    | `ServiceImpl` 内部使用，对 Controller 隐藏 `IService` 和 `Mapper` |

MyBatis-Plus 不要暴露 IService，参数校验统一使用 Jakarta Validation等其他细节需要你详细制定

### 3. 实现完整的基于Spring Security和JWT的RBAC模型

    （1）设计并构建完整的RBAC数据表，插入一些模拟数据
    （2）JWT采用AccessToken+RefreshToken双Token方案
        Access Token：15分钟； Refresh Token：7天；同时Refresh Token要存储进redis。
        JWT签名算法采用HS256，Refresh Token使用256bit随机字符串。
    （3）实现完整的登录，注销，鉴权功能接口


