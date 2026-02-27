package com.slg.scene.scene.aoi.model;

import lombok.Getter;

/**
 * 视野网格层级枚举
 * <p>定义不同缩放级别下的网格大小和屏幕范围</p>
 * 
 * <p><b>多层级设计：</b></p>
 * <ul>
 *   <li><b>DETAIL</b>：详细视图（网格8x8，屏幕16x16），用于近距离观察</li>
 *   <li><b>LAYER1</b>：标准视图（网格16x16，屏幕32x32），默认层级</li>
 *   <li><b>LAYER2</b>：中距离视图（网格24x24，屏幕48x48）</li>
 *   <li><b>LAYER3</b>：远距离视图（网格32x32，屏幕64x64），用于全局观察</li>
 * </ul>
 * 
 * <p><b>设计思路：</b></p>
 * <ul>
 *   <li>网格越大，单个网格包含的对象越多，计算效率越高</li>
 *   <li>屏幕范围越大，玩家能看见的对象越多</li>
 *   <li>根据客户端缩放级别动态选择合适的层级</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/2/3
 */
@Getter
public enum GridLayer {

    /** 详细视图层级（网格8x8，屏幕16x16） */
    DETAIL(8, 16, 16),

    /** 标准视图层级（网格16x16，屏幕32x32） */
    LAYER1(16, 32, 32),

    /** 中距离视图层级（网格24x24，屏幕48x48） */
    LAYER2(24, 48, 48),

    /** 远距离视图层级（网格32x32，屏幕64x64） */
    LAYER3(32, 64, 64),

    ;

    /** 网格长度（单个网格的边长） */
    private final int gridLength;

    /** 客户端屏幕宽度（视野范围） */
    private final int screenWidth;

    /** 客户端屏幕高度（视野范围） */
    private final int screenHeight;

    /** 屏幕半宽（用于计算视野边界） */
    private final int halfScreenWidth;
    
    /** 屏幕半高（用于计算视野边界） */
    private final int halfScreenHeight;

    GridLayer(int gridLength, int screenWidth, int screenHeight){
        this.gridLength = gridLength;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.halfScreenWidth = screenWidth / 2;
        this.halfScreenHeight = screenHeight / 2;
    }

    /** 缓存枚举值数组，避免重复调用 values() */
    public static GridLayer[] VALUES;

    static{
        VALUES = GridLayer.values();
    }

    /**
     * 根据层级索引获取对应的网格层级
     * <p><b>边界处理：</b></p>
     * <ul>
     *   <li>layer <= 1：返回最小层级（DETAIL）</li>
     *   <li>layer >= 最大索引：返回最大层级（LAYER3）</li>
     *   <li>其他：返回对应索引的层级</li>
     * </ul>
     *
     * @param layer 层级索引
     * @return 对应的网格层级
     */
    public static GridLayer getGridLayer(int layer){
        if (layer <= 1) {
            return GridLayer.DETAIL;
        } else if (layer >= VALUES.length - 1) {
            return VALUES[VALUES.length - 1];
        } else{
            return VALUES[layer];
        }
    }

}
