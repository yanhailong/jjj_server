package com.jjg.game.common.data;

/**
 * 数据回存回调,只更新单实体的对象
 *
 * @author 2CL
 */
public interface DataSaveCallback<D> {
    /**
     * 执行数据更新
     *
     * @param dataEntity 需要更新的数据实体
     */
    void updateData(D dataEntity);

    /**
     * 带更新结果的方法
     *
     * @param dataEntity 数据实体
     * @param <R>        更新后的结果
     * @return 更新结果
     */
    default <R> R updateDataWithRes(D dataEntity) {
        return null;
    }
}
