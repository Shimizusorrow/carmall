package com.qunchuang.carmall.controller;

import com.qunchuang.carmall.domain.CarInfo;
import com.qunchuang.carmall.service.CarInfoService;
import com.qunchuang.carmall.graphql.annotation.GraphqlController;
import com.qunchuang.carmall.graphql.annotation.GraphqlMutation;
import com.qunchuang.carmall.graphql.annotation.SchemaDocumentation;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Curtain
 * @date 2019/1/16 10:21
 */
@GraphqlController("carInfo")
public class CarInfoController {

    @Autowired
    private CarInfoService carInfoService;

    @SchemaDocumentation("添加车辆")
    @GraphqlMutation(path = "/add")
    public CarInfo add(CarInfo carInfo){
        return carInfoService.add(carInfo);
    }

    @SchemaDocumentation("修改车辆")
    @GraphqlMutation(path = "/modify")
    public CarInfo modify(CarInfo carInfo){
        return carInfoService.modify(carInfo);
    }

    @SchemaDocumentation("删除车辆")
    @GraphqlMutation(path = "/delete")
    public CarInfo delete(String id){
        return carInfoService.delete(id);
    }
}