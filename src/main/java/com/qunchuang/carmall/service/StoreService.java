package com.qunchuang.carmall.service;

import com.qunchuang.carmall.domain.Store; /**
 * @author Curtain
 * @date 2019/1/16 11:11
 */
public interface StoreService {
    /**
     * 转单
     * @param store
     * @return
     */
    Store changeOrder(Store store);

    /**
     * 删除
     * @param store
     * @return
     */
    Store delete(Store store);

    /**
     * 修改
     * @param store
     * @return
     */
    Store modify(Store store);

    /**
     * 添加
     * @param store
     * @return
     */
    Store add(Store store);
}