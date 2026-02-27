// SLG MongoDB 初始化脚本
// 首次启动时自动创建项目所需的数据库

print("===> 开始初始化 SLG 数据库...");

// 创建 slg_game 数据库
db = db.getSiblingDB('slg_game');
db.createCollection('_init');
db._init.insertOne({ _id: 'init', createdAt: new Date(), description: 'slg-game 模块数据库' });
print("===> 已创建数据库: slg_game");

// 创建 slg_scene 数据库
db = db.getSiblingDB('slg_scene');
db.createCollection('_init');
db._init.insertOne({ _id: 'init', createdAt: new Date(), description: 'slg-scene 模块数据库' });
print("===> 已创建数据库: slg_scene");

// 创建 slg_singlestart 数据库（合并启动模式）
db = db.getSiblingDB('slg_singlestart');
db.createCollection('_init');
db._init.insertOne({ _id: 'init', createdAt: new Date(), description: 'slg-singlestart 合并启动模块数据库' });
print("===> 已创建数据库: slg_singlestart");

print("===> MongoDB 初始化完成!");
