package com.ewcp.config;

import lombok.Data;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.api.impl.WxCpServiceImpl;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "wx.cp")
public class WxCpConfig {

    private String corpId;
    private String corpSecret;
    private String token;
    private String aesKey;
    private Integer agentId;

    @Bean
    public WxCpService wxCpService() {
        WxCpDefaultConfigImpl config = new WxCpDefaultConfigImpl();
        config.setCorpId(corpId);
        config.setCorpSecret(corpSecret);
        config.setToken(token);
        config.setAesKey(aesKey);
        config.setAgentId(agentId);

        WxCpService service = new WxCpServiceImpl();
        service.setWxCpConfigStorage(config);
        return service;
    }
}
