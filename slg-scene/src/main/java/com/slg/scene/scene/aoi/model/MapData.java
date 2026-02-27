package com.slg.scene.scene.aoi.model;

import lombok.Getter;

/**
 * 地图数据
 * <p>定义场景地图的基础信息</p>
 * 
 * <p><b>坐标系说明：</b></p>
 * <ul>
 *   <li>坐标范围：[0, width-1] x [0, height-1]</li>
 *   <li>原点在左下角</li>
 *   <li>X 轴向右，Y 轴向上</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/2/3
 */
@Getter
public class MapData {

    /** 地图宽度（单位：格子） */
    private int width;
    
    /** 地图高度（单位：格子） */
    private int height;

    /**
     * 地图阻挡数据
     */
    private byte[] blocks;

}
