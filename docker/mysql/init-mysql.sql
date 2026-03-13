-- SLG MySQL 初始化脚本
-- 首次启动时自动创建项目所需的数据库

-- 创建 slg_log 数据库（slg-log 告警日志模块）
CREATE DATABASE IF NOT EXISTS `slg_log`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
