package com.jjg.game.sampledata.bean;

import javax.annotation.processing.Generated;
import java.util.List;
/**
 * 配置bean
 *
 * @excelName popUpGetWay.xlsx
 * @sheetName popUpGetWay
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PopUpGetWayCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "popUpGetWay.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "popUpGetWay";

  /** 图标ICON */
  protected String icon;
  /** 道具ID */
  protected int itemId;
  /** 跳转名称 */
  protected int name;
  /** 是否开启 */
  protected boolean open;
  /** 打开界面需要导入的脚本 */
  protected String require;
  /** 描述文本 */
  protected int strDes;
  /** 跳转分类 */
  protected int type;
  /** 打开界面名称 */
  protected List<String> uiName;

  /** 返回图标ICON */
  public String getIcon() {
    return icon;
  }

  /** 返回道具ID */
  public int getItemId() {
    return itemId;
  }

  /** 返回跳转名称 */
  public int getName() {
    return name;
  }

  /** 返回是否开启 */
  public boolean getOpen() {
    return open;
  }

  /** 返回打开界面需要导入的脚本 */
  public String getRequire() {
    return require;
  }

  /** 返回描述文本 */
  public int getStrDes() {
    return strDes;
  }

  /** 返回跳转分类 */
  public int getType() {
    return type;
  }

  /** 返回打开界面名称 */
  public List<String> getUiName() {
    return uiName;
  }

  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
