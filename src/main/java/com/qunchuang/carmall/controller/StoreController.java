package com.qunchuang.carmall.controller;

import com.qunchuang.carmall.domain.CarInfo;
import com.qunchuang.carmall.domain.Store;
import com.qunchuang.carmall.service.StoreService;
import graphql.annotation.GraphqlController;
import graphql.annotation.GraphqlMutation;
import graphql.annotation.SchemaDocumentation;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Curtain
 * @date 2019/1/16 10:22
 */
@GraphqlController("store")
public class StoreController {

    @Autowired
    private StoreService storeService;

    @SchemaDocumentation("添加门店")
    @GraphqlMutation(path = "/add")
    public Store add(Store store){
        return storeService.add(store);
    }

    @SchemaDocumentation("修改门店")
    @GraphqlMutation(path = "/modify")
    public Store modify(Store store){
        return storeService.modify(store);
    }

    @SchemaDocumentation("删除门店")
    @GraphqlMutation(path = "/delete")
    public Store delete(Store store){
        return storeService.delete(store);
    }

    @SchemaDocumentation("转单")
    @GraphqlMutation(path = "/changeOrder")
    public Store changeOrder(Store store){
        return storeService.changeOrder(store);
    }
}
