# 开发文档

## 开发环境配置

以下配置基于 Windows 11 操作系统，仅供参考，可根据实际情况选择其他配置方式。

### 一、安装 JDK 17

本项目要求 JDK 17
安装完成后配置环境变量并验证：

```powershell
java -version
```

### 二、安装 Maven（可选）
这里统一使用3.9.16版本

项目自带 [Maven Wrapper](https://maven.apache.org/wrapper/)（`mvnw` / `mvnw.cmd`），首次执行 `./mvnw` 时会自动下载匹配的 Maven 版本，**无需手动安装 Maven**。

如果你习惯在 IDE 中使用系统 Maven，或需要 `mvn` 命令全局可用：

1. 从 [maven.apache.org/download.cgi](https://maven.apache.org/download.cgi) 下载 Binary zip archive（推荐 3.9.16）
2. 解压到固定目录，如 `C:\tools\apache-maven-3.9.x`
3. 添加环境变量：
   - 新建 `MAVEN_HOME`，值为 Maven 解压路径
   - 在 `Path` 中添加 `%MAVEN_HOME%\bin`
4. 验证：
   ```powershell
   mvn --version
   ```
   应输出 Maven 版本和使用的 JDK 信息。

> **注意：** 项目脚本中始终使用 `./mvnw`（而非 `mvn`），确保所有人使用相同版本的 Maven，避免环境差异导致的问题。
>
> **IDEA 用户无需手动操作：** IntelliJ IDEA 检测到项目包含 Maven Wrapper 后会自动使用它。Settings → Build Tools → Maven → "Use Maven wrapper" 默认勾选，Maven 面板的所有操作（刷新、编译、运行）都走 wrapper，版本自动匹配。只有终端手动输命令时才需要 `./mvnw`。

### 三、安装 WSL2（Ubuntu-24.04）

在 PowerShell（管理员）中执行：

```powershell
wsl --install -d Ubuntu-24.04
```

安装完成后重启电脑，首次进入 Ubuntu 会提示创建 Linux 用户名和密码。后续可通过 Windows Terminal 或 `wsl` 命令进入。

> **为什么需要 WSL2？** Docker Desktop 在 Windows 上依赖 WSL2 作为后端运行 Linux 容器，性能远优于传统的 Hyper-V 后端。

### 四、安装 Docker Desktop

从 [docker.com](https://www.docker.com/products/docker-desktop/) 下载并安装 Docker Desktop for Windows。安装时确保勾选 "Use WSL 2 instead of Hyper-V"。

1. 配置镜像源 registry-mirrors（Docker Desktop → Settings → Docker Engine）：
    ```json
    {
      "builder": {
        "gc": {
          "defaultKeepStorage": "20GB",
          "enabled": true
        }
      },
      "experimental": false,
      "registry-mirrors": [
        "https://docker.1ms.run"
      ]
    }
    ```
    配置完成后点击 "Apply & Restart" 重启 Docker 引擎。

2. 开启 WSL Integration（Docker Desktop → Settings → Resources → WSL Integration）：
   - 勾选 "Enable integration with my default WSL distro"
   - 确保 `Ubuntu-24.04` 处于开启状态

3. 在 Ubuntu 中验证 Docker 是否正常：
    ```shell
    docker version
    docker compose version
    ```
    两条命令都应正常输出版本信息，无报错。

### 五、创建 docker-compose.yml 文件

找一个固定目录（建议 `~/docker/lims/`），创建 `docker-compose.yml`（可用 `code docker-compose.yml` 调起 VS Code 编辑）：

```yaml
services:
  ivorysql:
    image: ivorysql/ivorysql:5.4-bookworm
    container_name: ivorysql
    restart: unless-stopped
    ports:
      - "5432:5432"
    environment:
      IVORYSQL_PASSWORD: 123456
    volumes:
      - ivorysql_data:/var/local/ivorysql/ivorysql-5/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ivorysql -d ivorysql"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7.2-alpine
    container_name: redis
    restart: unless-stopped
    command:
      - redis-server
      - --appendonly
      - "yes"
      - --requirepass
      - "123456"
    ports:
      - "6380:6379" # 若本机没有安装 redis 可直接写为 6379:6379，修改 6380 是为了避免端口冲突
    volumes:
      - redis_data:/data

volumes:
  ivorysql_data:
  redis_data:
```

> **连接信息：** IvorySQL 默认用户 `ivorysql`，密码 `123456`，端口 `5432`。Redis 密码 `123456`，端口 `6380`。
>
> **数据持久化：** 两个服务均通过 named volume 持久化数据。即使容器被删除重建，数据也不会丢失。如需完全重置，执行 `docker compose down -v`（会清空数据卷）。

### 六、启动容器

在 `docker-compose.yml` 所在目录执行：

```shell
docker compose up -d
```

首次启动会拉取镜像（约 1-2 分钟）。`-d` 表示后台运行。

此时若一切正常可在 Docker Desktop 的 Containers 界面看到 `ivorysql` 和 `redis` 两个容器，状态均为 Running。

### 七、初始化数据库

1. 确认 IvorySQL 容器已在运行。
2. 用数据库客户端连接 `127.0.0.1:5432`（用户 `ivorysql`，密码 `123456`）。
3. 创建 `lims` 数据库（如果尚不存在）：
   ```sql
   CREATE DATABASE lims;
   ```
4. 导入初始化脚本：执行 `src/main/resources/db/init.sql`。该脚本会建表并插入模拟数据（用户密码均为 `123456`）。
可以在DBeaver上导入，也可通过命令行一步完成：

```shell
docker exec -e PGPASSWORD=123456 -i ivorysql psql -U ivorysql -d ivorysql -c "CREATE DATABASE lims;"
docker exec -e PGPASSWORD=123456 -i ivorysql psql -U ivorysql -d lims < src/main/resources/db/init.sql
```

### 八、安装 Redis Insight（可选）

Redis Insight 是 Redis 官方提供的 GUI 管理工具，方便查看缓存数据（如 refresh token、RSA 密钥等）。

从 [redis.io/insight](https://redis.io/insight/) 下载安装。添加连接时填写：

- Host: `127.0.0.1`
- Port: `6380`
- Password: `123456`

### 九、安装 DBeaver（可选）

DBeaver 是开源的通用数据库管理工具，支持 PostgreSQL/IvorySQL。

从 [dbeaver.io](https://dbeaver.io/download/) 下载安装。添加连接时：

- 数据库类型：PostgreSQL
- Host: `127.0.0.1`
- Port: `5432`
- Database: `lims`
- Username: `ivorysql`
- Password: `123456`

> **提示：** 在 DBeaver 的 "Driver properties" 中无需修改驱动，标准 PostgreSQL JDBC 驱动完全兼容 IvorySQL。

### 十、启动项目

确保数据库和 Redis 容器均已运行后：

```bash
./mvnw spring-boot:run
```

首次启动会下载 Maven 依赖（约 1-2 分钟）。启动完成后访问：

- **API 文档（Swagger UI）：** `http://localhost:8080/swagger-ui.html`
- **调试登录：** 在 Swagger UI 中调用 `POST /auth/login`，请求体 `{"username":"admin","cipherPwd":"123456"}`（dev 环境下 `keyId` 留空时 `cipherPwd` 作为明文密码处理）。获取 token 后点击右上角 **Authorize** 按钮填入。
- **运行测试：** `./mvnw test`（需要 DB + Redis 运行中，测试直连 dev 环境数据库，通过 `@Transactional` 自动回滚）

### 十一、常见问题

**Q: Docker 容器启动后立即退出？**
```shell
docker logs ivorysql
docker logs redis
```
查看日志定位原因。常见原因：端口被占用（检查本机是否有其他 PostgreSQL/Redis 在运行）。

**Q: 项目启动报 "Connection refused"？**
确认两个容器都在运行：`docker ps`。如果容器在运行但仍连不上，检查 `application.yml` 中的连接信息是否与容器配置一致。

**Q: 如何完全重置环境？**
```shell
# 在 docker-compose.yml 所在目录
docker compose down -v   # 停止容器并删除数据卷（清空所有数据）
docker compose up -d      # 重新启动并初始化
# 然后重新执行第七步（创建 lims 库 + 导入 init.sql）
```

**Q: macOS / Linux 用户如何使用？**
WSL2 步骤可跳过。Docker Desktop 在 macOS 上原生运行。其余步骤（docker-compose.yml、数据库初始化、项目启动）完全相同。
