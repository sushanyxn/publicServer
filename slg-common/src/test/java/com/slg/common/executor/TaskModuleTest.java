package com.slg.common.executor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskModule 单元测试
 */
class TaskModuleTest {

    @Test
    void toKey_singleChain_returnsCachedSameInstance() {
        TaskKey k1 = TaskModule.SYSTEM.toKey();
        TaskKey k2 = TaskModule.SYSTEM.toKey();
        assertSame(k1, k2);
        assertEquals(TaskModule.SYSTEM, k1.module());
        assertEquals(0L, k1.id());
    }

    @Test
    void toKey_multiChain_returnsNewKeyWithZeroId() {
        TaskKey k = TaskModule.PLAYER.toKey();
        assertNotNull(k);
        assertEquals(TaskModule.PLAYER, k.module());
        assertEquals(0L, k.id());
    }

    @Test
    void toKeyLong_multiChain_returnsNewKeyWithGivenId() {
        TaskKey k1 = TaskModule.PLAYER.toKey(100L);
        TaskKey k2 = TaskModule.PLAYER.toKey(100L);
        assertEquals(TaskModule.PLAYER, k1.module());
        assertEquals(100L, k1.id());
        assertNotSame(k1, k2);
        assertEquals(k1.module(), k2.module());
        assertEquals(k1.id(), k2.id());
    }

    @Test
    void toKeyLong_singleChain_ignoresId_returnsCachedKey() {
        TaskKey k1 = TaskModule.SYSTEM.toKey(999L);
        TaskKey k2 = TaskModule.SYSTEM.toKey();
        assertSame(k1, k2);
    }

    @Test
    void isMultiChain_playersAndPersistence_true() {
        assertTrue(TaskModule.PLAYER.isMultiChain());
        assertTrue(TaskModule.PERSISTENCE.isMultiChain());
        assertTrue(TaskModule.ROBOT.isMultiChain());
    }

    @Test
    void isMultiChain_systemAndLogin_false() {
        assertFalse(TaskModule.SYSTEM.isMultiChain());
        assertFalse(TaskModule.LOGIN.isMultiChain());
        assertFalse(TaskModule.SCENE.isMultiChain());
    }

    @Test
    void getName_returnsDisplayName() {
        assertEquals("Player", TaskModule.PLAYER.getName());
        assertEquals("System", TaskModule.SYSTEM.getName());
    }
}
