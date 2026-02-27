package com.slg.common.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yangxunan
 * @date 2026/1/29
 */
public class AndConsume<T> implements IConsume<T> {

    private List<IConsume<T>> consumes = new ArrayList<>();

    public void addConsume(IConsume<T> consume){
        consumes.add(consume);
    }

    @Override
    public boolean verify(T t){
        for (IConsume<T> consume : consumes) {
            if (!consume.verify(t)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void verifyThrow(T t){
        for (IConsume<T> consume : consumes) {
            consume.verifyThrow(t);
        }
    }

    @Override
    public void consume(T t){
        for (IConsume<T> consume : consumes) {
            consume.consume(t);
        }
    }
}
