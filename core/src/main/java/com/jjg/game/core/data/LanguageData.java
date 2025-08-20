package com.jjg.game.core.data;


import com.jjg.game.core.pb.LangParamInfo;
import com.jjg.game.core.pb.LanguageInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11
 * @date 2025/8/20 16:48
 */
public class LanguageData {
    private int type;
    private String content;
    private int langId;
    private List<LanguageParamData> params;

    public LanguageData() {
    }

    public LanguageData(int type, String content) {
        this.type = type;
        this.content = content;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getLangId() {
        return langId;
    }

    public void setLangId(int langId) {
        this.langId = langId;
    }

    public List<LanguageParamData> getParams() {
        return params;
    }

    public void setParams(List<LanguageParamData> params) {
        this.params = params;
    }

    public LanguageInfo toPbInfo(){
        LanguageInfo info = new LanguageInfo();
        info.type = this.type;
        info.content = this.content;
        info.langId = this.langId;

        if(this.params != null && !this.params.isEmpty()){
            info.params = new ArrayList<>(this.params.size());
            for(LanguageParamData param : this.params){
                LangParamInfo paramInfo = new LangParamInfo();
                paramInfo.type = param.getType();
                paramInfo.param = param.getParam();
                info.params.add(paramInfo);
            }
        }
        return info;
    }
}
