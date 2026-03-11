# Docker Desktop 配置备份

本目录为**本机 Docker Desktop 配置的只读备份**，便于团队参考或在新环境还原。请勿直接覆盖系统配置使用。

## 文件来源与说明

| 文件 | 本机路径 | 说明 |
|------|----------|------|
| `settings-store.json` | `%APPDATA%\Docker\settings-store.json` | Docker Desktop 界面设置（开机自启、文件共享目录、Docker AI、containerd snapshotter 等） |
| `config.json` | `%USERPROFILE%\.docker\config.json` | Docker CLI 配置：认证存储、当前 context |
| `daemon.json` | `%USERPROFILE%\.docker\daemon.json` | Docker 引擎/构建配置：builder GC、experimental 等 |
| `windows-daemon.json` | `%USERPROFILE%\.docker\windows-daemon.json` | Windows 侧 daemon 覆盖配置 |

## 当前备份中的主要项

- **settings-store.json**：未开机自启、已展示过引导、启用 Docker AI、文件共享含 `C:\mongodata`、使用 containerd snapshotter、SettingsVersion 43。
- **config.json**：使用 `desktop` 凭据存储、当前 context 为 `desktop-linux`。
- **daemon.json**：构建缓存 GC 开启、默认保留 20GB、未开 experimental。

## 使用注意

- 备份时已去除敏感信息（如 `auths` 为空）；实际登录信息由 `credsStore: desktop` 管理。
- 若需在新机还原，请仅作参考，按需合并到对应路径，并注意 Windows 路径与权限。
- 修改本目录文件不会影响本机 Docker；修改本机配置后如需更新备份，请重新从上述路径复制。
