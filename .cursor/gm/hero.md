# 英雄 GM 指令

> 来源类：`com.slg.game.gm.command.HeroGMCommand`

## 指令列表

### gainHero

- **用法**：`gainHero <heroId>`
- **参数**：
  - `heroId` (int) — 获得指定英雄（对应用法中的英雄 ID）
- **说明**：获得指定英雄
- **返回**：0=成功, 1=失败

### gainAllHero

- **用法**：`gainAllHero`
- **参数**：无
- **说明**：获得全部英雄
- **返回**：0=成功, 1=失败

### heroMaxLevel

- **用法**：`heroMaxLevel`
- **参数**：无
- **说明**：全部英雄满级
- **返回**：0=成功, 1=失败

### setHeroLevel

- **用法**：`setHeroLevel <heroId> <level>`
- **参数**：
  - `heroId` (int) — 要设置等级的英雄 ID
  - `level` (int) — 目标等级
- **说明**：设置指定英雄等级
- **返回**：0=成功, 1=失败
