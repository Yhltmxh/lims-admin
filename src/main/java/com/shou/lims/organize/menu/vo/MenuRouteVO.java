package com.shou.lims.organize.menu.vo;

import lombok.Data;

import java.util.List;

/**
 * 当前用户运行时菜单，字段与 Ant Design Pro MenuDataItem 对齐。
 */
@Data
public class MenuRouteVO {
    private String key;
    private String name;
    private String path;
    private String icon;
    private Boolean hideInMenu;
    private List<MenuRouteVO> children;
}
