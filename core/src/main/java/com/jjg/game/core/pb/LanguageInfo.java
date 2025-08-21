package com.jjg.game.core.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.data.LanguageData;
import com.jjg.game.core.data.LanguageParamData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11
 * @date 2025/8/20 15:38
 */
@ProtobufMessage
@ProtoDesc("多语言")
public class LanguageInfo {
    @ProtoDesc("0.原始展示  1.多语言参数匹配")
    public int type;
    @ProtoDesc("内容")
    public String content;
    @ProtoDesc("多语言id")
    public int langId;
    @ProtoDesc("参数")
    public List<LangParamInfo> params;


    public LanguageData toData(){
        LanguageData d = new LanguageData();
        d.setType(this.type);
        d.setContent(this.content);
        d.setLangId(this.langId);

        if(this.params != null && !this.params.isEmpty()){
            List<LanguageParamData> paramsData = new ArrayList<>(this.params.size());
            for(LangParamInfo p : this.params){
                LanguageParamData pd = new LanguageParamData();
                pd.setType(p.type);
                pd.setParam(p.param);
                paramsData.add(pd);
            }
            d.setParams(paramsData);
        }
        return d;
    }
}
