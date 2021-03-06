package com.qunchuang.carmall.domain;

import cn.wzvtcsoft.bosdomain.BosEntity;
import cn.wzvtcsoft.bosdomain.annotations.Bostype;
import com.qunchuang.carmall.enums.CarMallExceptionEnum;
import com.qunchuang.carmall.exception.CarMallException;
import com.qunchuang.carmall.graphql.annotation.SchemaDocumentation;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Curtain
 * @date 2019/1/15 10:30
 */

@Entity
@Bostype("C02")
@SchemaDocumentation("车辆信息")
@Getter
@Setter
public class CarInfo extends BosEntity{

//    @SchemaDocumentation("汽车名称")
//    private String name;

    @SchemaDocumentation("品牌")
    private String brand;

    @SchemaDocumentation("型号")
    private String model;

    @SchemaDocumentation("详细信息文件")
    String filename;

    @SchemaDocumentation("厂商指导价")
    private String price;

    @SchemaDocumentation("权重(越高越前面)")
    private Integer sort = 0;

    @SchemaDocumentation("上架")
    private boolean upperShelf = false;

    @SchemaDocumentation("图片")
    private String img;

    @SchemaDocumentation("金融方案")
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", orphanRemoval = true, fetch = FetchType.EAGER)
//    @OrderBy(clause = "periods desc")
        private Set<FinancialScheme> financialSchemes = new HashSet<>();

    /**
     * 上下架车辆
     */
    public void upperDownShelf(){
        if (this.upperShelf){
            //下架
            this.upperShelf = false;
        }else {
            //上架
            if (this.financialSchemes==null || StringUtils.isEmpty(img)){
                throw new CarMallException(CarMallExceptionEnum.CAR_INFO_NOT_IMPERFECT);
            }
            this.upperShelf = true;
        }
    }

}
