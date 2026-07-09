package com.ewcp.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.message.WxCpXmlMessage;
import me.chanjar.weixin.cp.bean.message.WxCpXmlOutMessage;
import me.chanjar.weixin.cp.config.WxCpConfigStorage;
import me.chanjar.weixin.cp.message.WxCpMessageRouter;
import me.chanjar.weixin.cp.util.crypto.WxCpCryptUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * 企业微信回调接口
 * 用于配置"接收消息"服务器 URL 验证与消息接收
 *
 * 企微后台配置路径: 应用管理 -> 自建应用 -> 接收消息 -> 设置API接收
 * 回调 URL 格式: https://test.wizone.work/api/wx/callback
 */
@Slf4j
@RestController
@RequestMapping("/api/wx/callback")
@RequiredArgsConstructor
public class WxCallbackController {

    private final WxCpService wxCpService;

    /**
     * URL 验证（GET）
     * 企微配置接收消息服务器时发送验证请求：
     * 1. 对参数做 Urldecode（Spring @RequestParam 已自动处理）
     * 2. 通过 msg_signature 校验请求合法性
     * 3. 解密 echostr 得到消息内容明文（msg 字段）
     * 4. 在 1 秒内原样返回明文消息内容（不能加引号，不能带 BOM，不能带换行符）
     */
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public String verify(@RequestParam("msg_signature") String msgSignature,
                         @RequestParam("timestamp") String timestamp,
                         @RequestParam("nonce") String nonce,
                         @RequestParam("echostr") String echostr) {
        log.info("企微回调验证: msgSignature={}, timestamp={}, nonce={}", msgSignature, timestamp, nonce);

        try {
            String token = wxCpService.getWxCpConfigStorage().getToken();

            // 1. 签名校验：SHA1(sort(token, timestamp, nonce, echostr))
            String[] arr = {token, timestamp, nonce, echostr};
            Arrays.sort(arr);
            String joined = String.join("", arr);
            byte[] sha1 = MessageDigest.getInstance("SHA1")
                    .digest(joined.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : sha1) hex.append(String.format("%02x", b));
            if (!msgSignature.equals(hex.toString())) {
                log.error("签名校验失败: expected={}, computed={}", msgSignature, hex);
                return "signature verification failed";
            }

            // 2. 解密 echostr，直接返回 msg 明文
            WxCpCryptUtil cryptUtil = new WxCpCryptUtil(wxCpService.getWxCpConfigStorage());
            String msg = cryptUtil.decrypt(echostr);
            log.info("验证通过，返回 msg: {}", msg);

            return msg;
        } catch (Exception e) {
            log.error("企微回调验证失败", e);
            return "verification failed";
        }
    }

    /**
     * 接收消息与事件（POST）
     * 企微推送的消息/事件均为此入口
     */
    @PostMapping(produces = "application/xml;charset=UTF-8")
    public void receive(@RequestParam("msg_signature") String msgSignature,
                        @RequestParam("timestamp") String timestamp,
                        @RequestParam("nonce") String nonce,
                        HttpServletRequest request,
                        HttpServletResponse response) throws IOException {
        log.info("收到企微回调: msgSignature={}, timestamp={}, nonce={}", msgSignature, timestamp, nonce);

        WxCpConfigStorage configStorage = wxCpService.getWxCpConfigStorage();

        // 解密并解析消息
        WxCpXmlMessage inMessage = WxCpXmlMessage.fromEncryptedXml(
                request.getInputStream(), configStorage, timestamp, nonce, msgSignature);

        log.info("消息类型: {}, 内容: {}", inMessage.getMsgType(),
                inMessage.getContent() != null ? inMessage.getContent() : "(event)");

        // 路由消息并返回加密响应
        WxCpMessageRouter router = new WxCpMessageRouter(wxCpService);
        WxCpXmlOutMessage outMessage = router.route(inMessage);

        if (outMessage != null) {
            response.setContentType("application/xml;charset=UTF-8");
            response.getWriter().write(outMessage.toEncryptedXml(configStorage));
        }
    }

    /**
     * 控制器级异常处理 —— 企微回调必须返回纯文本，不能让 GlobalExceptionHandler 返回 JSON
     */
    @ExceptionHandler(Exception.class)
    public String handleCallbackException(Exception e) {
        log.error("企微回调处理异常", e);
        return "error";
    }
}
