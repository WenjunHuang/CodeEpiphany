# 使用不同IntelliJ版本构建

本项目支持使用不同版本的IntelliJ IDEA进行构建，以确保插件与不同版本的IntelliJ兼容。

## 支持的版本

- **233**: IntelliJ IDEA 2023.3 (Build 233.11799.241)
- **241**: IntelliJ IDEA 2024.1 (Build 241.14494.240)  
- **252**: IntelliJ IDEA 2025.2 (Build 252.13776.59) - 默认版本

## 使用方法

### 1. 通过系统属性指定版本

```bash
# 编译项目
sbt -Dintellij.version=241 compile

# 运行测试
sbt -Dintellij.version=233 test

# 构建插件
sbt -Dintellij.version=252 publishPlugin

# 执行多个任务
sbt -Dintellij.version=241 clean compile test
```

### 2. 通过环境变量指定版本

```bash
# 设置环境变量
export INTELLIJ_VERSION=241

# 然后正常运行sbt命令
sbt compile
sbt test
sbt clean compile test
```

### 3. 使用便捷脚本

#### Windows
```cmd
# 编译
scripts\build-with-version.bat 241 compile

# 测试
scripts\build-with-version.bat 233 test

# 构建插件
scripts\build-with-version.bat 252 publishPlugin

# 执行多个任务
scripts\build-with-version.bat 241 clean compile test

# 使用引号包含复杂命令
scripts\build-with-version.bat 252 "clean; compile; test"
```

#### Unix/Linux/macOS
```bash
# 给脚本执行权限
chmod +x scripts/build-with-version.sh

# 编译
./scripts/build-with-version.sh 241 compile

# 测试
./scripts/build-with-version.sh 233 test

# 构建插件
./scripts/build-with-version.sh 252 publishPlugin

# 执行多个任务
./scripts/build-with-version.sh 241 clean compile test

# 使用引号包含复杂命令
./scripts/build-with-version.sh 252 "clean; compile; test"
```

### 4. IDE中设置

如果你在IDE中运行sbt任务，可以在IDE的VM选项中添加：
```
-Dintellij.version=241
```

## 版本兼容性矩阵

| IntelliJ版本 | sinceBuild | untilBuild | 说明 |
|-------------|------------|------------|------|
| 233 | 233.11799.241 | 241.14494.240 | 支持到2024.1 |
| 241 | 241.14494.240 | 251.* | 支持到2025.1 |
| 252 | 252.13776.59 | null | 最新版本，无上限 |

## 构建流程说明

当你指定不同的IntelliJ版本时，系统会：

1. 根据版本选择对应的构建号
2. 设置正确的`sinceBuild`和`untilBuild`参数
3. 选择对应版本的源代码目录（如果存在）
4. 使用相应的API进行编译

## CI/CD 支持

项目的CI/CD流水线已配置为自动构建所有支持的IntelliJ版本：

### GitHub Actions

在每次推送到 `main` 或 `develop` 分支，以及每个Pull Request时，CI会：

1. **并行构建三个版本**: 233, 241, 252
2. **独立的构建矩阵**: 每个版本有独立的构建任务，互不影响
3. **完整的测试流程**: 包括编译、测试和打包
4. **版本验证**: 显示每个构建使用的具体IntelliJ版本

### CI流程步骤

每个IntelliJ版本的构建包含以下步骤：

1. **环境设置**: Java 17, SBT, Node.js
2. **依赖缓存**: 分版本缓存SBT和npm依赖
3. **版本确认**: 显示当前使用的IntelliJ版本
4. **编译测试**: 清理、编译、运行测试
5. **插件打包**: 生成插件zip文件

### 查看构建状态

- 访问项目的GitHub Actions页面查看所有版本的构建状态
- 每个版本的构建任务有清晰的标识：`Build with IntelliJ 233/241/252`
- 支持 `fail-fast: false`，即使某个版本构建失败，其他版本仍会继续构建

## 故障排除

如果遇到版本相关的问题：

1. **确认版本号正确**: 支持的版本只有 233, 241, 252
2. **检查系统属性**: 使用 `sbt 'show intellijBuild'` 查看当前使用的版本
3. **清理缓存**: 运行 `sbt clean` 清理之前的构建缓存
4. **查看日志**: 构建时会输出当前使用的IntelliJ版本信息

## 示例

### 单个任务
```bash
# 为IntelliJ 2024.1构建插件
sbt -Dintellij.version=241 publishPlugin

# 使用环境变量
export INTELLIJ_VERSION=233
sbt test

# 使用脚本
./scripts/build-with-version.sh 252 compile
```

### 多个任务
```bash
# 清理、编译、测试一条龙
sbt -Dintellij.version=241 clean compile test

# 使用脚本执行多个任务
./scripts/build-with-version.sh 241 clean compile test publishPlugin

# 使用引号包含复杂的sbt命令
./scripts/build-with-version.sh 252 "clean; compile; test; publishPlugin"

# Windows批处理脚本
scripts\build-with-version.bat 241 clean compile test
```

### 高级用法
```bash
# 重新生成代码，清理，编译，运行所有测试
./scripts/build-with-version.sh 241 jooqCodegen clean compile test

# 完整的CI流程
./scripts/build-with-version.sh 252 clean jooqCodegen compile test publishPlugin
``` 