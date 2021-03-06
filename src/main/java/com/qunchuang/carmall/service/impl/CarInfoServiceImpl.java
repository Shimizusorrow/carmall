package com.qunchuang.carmall.service.impl;

import com.qunchuang.carmall.domain.CarBrandIcon;
import com.qunchuang.carmall.domain.CarInfo;
import com.qunchuang.carmall.enums.CarMallExceptionEnum;
import com.qunchuang.carmall.exception.CarMallException;
import com.qunchuang.carmall.repository.CarInfoRepository;
import com.qunchuang.carmall.service.CarBrandIconService;
import com.qunchuang.carmall.service.CarInfoService;
import com.qunchuang.carmall.utils.BeanCopyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Curtain
 * @date 2019/1/16 10:49
 */
@Service
@Slf4j
@Transactional
public class CarInfoServiceImpl implements CarInfoService {

    @Autowired
    private CarInfoRepository carInfoRepository;

    @Autowired
    private CarBrandIconService carBrandIconService;

    @Override
    @PreAuthorize("hasAuthority('PLATFORM_MANAGEMENT')")
    public CarInfo add(CarInfo carInfo) {

        //取型号做唯一性区分
        String model = carInfo.getModel();
        if (carInfoRepository.existsByModel(model)) {
            //相同则覆盖
            CarInfo old = carInfoRepository.findByModel(model);
            Set<String> filter = new HashSet<>();
            filter.add("upperShelf");
            filter.add("financialSchemes");
            BeanUtils.copyProperties(carInfo, old, BeanCopyUtil.filterProperty(carInfo, filter));

            return carInfoRepository.save(old);
        }


        return carInfoRepository.save(carInfo);
    }

    @Override
    @PreAuthorize("hasAuthority('PLATFORM_MANAGEMENT')")
    public List<CarInfo> addAll(List<CarInfo> carInfos) {

        Set<String> brandSet = new HashSet<>();
        String model;
        List<CarBrandIcon> carBrandIcons = new ArrayList<>();
        CarInfo carInfo;

        for (int i = 0; i < carInfos.size(); i++) {
            //取型号做唯一性区分
            carInfo = carInfos.get(i);
            model = carInfo.getModel();
            brandSet.add(carInfo.getBrand());
            if (carInfoRepository.existsByModel(model)) {
                //相同则覆盖
                CarInfo old = carInfoRepository.findByModel(model);
                //覆盖后重新启用
                carInfo.setDisabled(false);
                Set<String> filter = new HashSet<>();
                filter.add("upperShelf");
                filter.add("financialSchemes");
                BeanUtils.copyProperties(carInfo, old, BeanCopyUtil.filterProperty(carInfo, filter));

                carInfos.set(i, old);
            }

        }
        //过滤数据库中已存在的品牌信息 并添加新的到集合中
        Set<String> collect = brandSet.stream().filter(b -> !(carBrandIconService.existsByBrand(b))).collect(Collectors.toSet());
        collect.forEach(b -> carBrandIcons.add(new CarBrandIcon(b)));

        //保存品牌信息
        carBrandIconService.initAll(carBrandIcons);

        //保存车辆信息
        return carInfoRepository.saveAll(carInfos);
    }

    @Override
    @PreAuthorize("hasAuthority('PLATFORM_MANAGEMENT')")
    public CarInfo modify(CarInfo carInfo) {

        CarInfo result = findOne(carInfo.getId());
        Set<String> filter = new HashSet<>();
        filter.add("upperShelf");
        filter.add("financialSchemes");
        BeanUtils.copyProperties(carInfo, result, BeanCopyUtil.filterProperty(carInfo, filter));

        //拷贝金融方案
        result.getFinancialSchemes().clear();
        result.getFinancialSchemes().addAll(carInfo.getFinancialSchemes());

        return carInfoRepository.save(result);
    }

    @Override
    @PreAuthorize("hasAuthority('PLATFORM_MANAGEMENT')")
    public CarInfo delete(String id) {
        CarInfo carInfo = findOne(id);
        carInfo.isAble();
        carInfo.setUpperShelf(false);
        return carInfoRepository.save(carInfo);
    }

    @Override
    @PreAuthorize("hasAuthority('PLATFORM_MANAGEMENT')")
    public CarInfo upperDownShelf(String id) {
        CarInfo carInfo = findOne(id);
        carInfo.upperDownShelf();
        return carInfoRepository.save(carInfo);
    }

    @Override
    public CarInfo findOne(String id) {
        Optional<CarInfo> carInfo = carInfoRepository.findById(id);
        if (!carInfo.isPresent()) {
            log.error("车辆信息不存在 id = {}", id);
            throw new CarMallException(CarMallExceptionEnum.CAR_INFO_NOT_EXISTS);
        }
        return carInfo.get();
    }
}
