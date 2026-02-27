package com.slg.common.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * 聚合条件
 *
 * @author yangxunan
 * @date 2026/1/29
 */
public class AndCondition<T> implements ICondition<T>{

    private List<ICondition<T>> conditions = new ArrayList<ICondition<T>>();

    public void addCondition(ICondition<T> condition){
        conditions.add(condition);
    }

    @Override
    public boolean verify(T t){

        for (ICondition<T> condition : conditions) {
            if (!condition.verify(t)) {
                return false;
            }
        }

        return true;
    }
}
