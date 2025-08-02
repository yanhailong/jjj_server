package com.jjg.game.table.betsample.sample.bean;

import javax.annotation.processing.Generated;
import java.util.Objects;
/**
* 配置表基类
*
* @author CCL
*/
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BaseCfgBean {

  /** id */
  protected int id;

  /** 返回id */
  public int getId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    BaseCfgBean that = (BaseCfgBean) o;
    return id == that.id;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
