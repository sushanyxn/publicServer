package com.slg.frameworktest.hotreload;

/**
 * 热更测试目标类，测试中会动态编译此类的修改版本并热更
 *
 * @author framework-test
 */
public class HotReloadTestTarget {

    public static String getValue() {
        return "original";
    }
}
