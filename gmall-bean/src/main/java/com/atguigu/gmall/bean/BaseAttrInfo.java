package com.atguigu.gmall.bean;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
public class BaseAttrInfo implements Serializable {
    @Id
    @Column
    //获取主键自增的值
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;
    @Column
    private String attrName;
    @Column
    private String catalog3Id;

    @Transient  //该字段不是数据库字段，只是业务需要
    private List<BaseAttrValue> attrValueList;
}

