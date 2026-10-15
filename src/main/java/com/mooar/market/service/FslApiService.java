package com.mooar.market.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooar.market.common.BizException;
import com.mooar.market.common.ErrorCode;
import com.mooar.market.config.MarketProperties;
import com.mooar.market.dto.FslUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * FSL 登录对接：authorization_code 换 access_token，再用 token 拉用户资料
 */
@Service
public class FslApiService {

    private static final Logger loginLog = LoggerFactory.getLogger("LOGIN_LOG");

    private final MarketProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FslApiService(MarketProperties properties) {
        this.properties = properties;
        MarketProperties.Fsl conf = properties.getFsl();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(conf.getTimeoutMs());
        factory.setReadTimeout(conf.getTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
        if (conf.getUrl() != null && conf.getUrl().startsWith("http://")) {
            loginLog.warn("FSL 地址配的是 http，服务端会 301 跳 https 且不会跟随重定向，" +
                    "会导致响应体为空；建议直接配 https: {}", conf.getUrl());
        }
    }

    /**
     * 用前端带过来的 code 换取用户资料
     */
    public FslUserInfo login(String code) {
        String accessToken = exchangeToken(code);
        JsonNode data = getUserInfo(accessToken);
        long uid = data.path("fslUid").asLong();
        if (uid <= 0) {
            loginLog.error("FSL not find fslUid, data={}", data);
            throw new BizException(ErrorCode.FSL_API_ERROR);
        }
        if (properties.getFsl().getAddressField() == null || properties.getFsl().getAddressField().isBlank()) {
            loginLog.info("FSL info: {}",
                    fieldNames(data));
        }
        return new FslUserInfo(uid,
                data.path("name").asText(""),
                data.path("email").asText(""),
                data.path("stepnEmail").asText(""),
                data.path("profileImage").asText(""),
                readAddress(data));
    }

    private String exchangeToken(String code) {
        MarketProperties.Fsl conf = properties.getFsl();
        String url = UriComponentsBuilder.fromHttpUrl(conf.getUrl())
                .path("/account/oauth/token")
                .queryParam("appkey", conf.getAppKey())
                .queryParam("appsecret", conf.getAppSecret())
                .queryParam("grant_type", "authorization_code")
                .queryParam("authorization_code", code)
                .build()
                .toUriString();
        String body = get(url);
        JsonNode json = parse(body);
        String accessToken = json == null ? "" : json.path("data").path("access_token").asText("");
        if (accessToken.isBlank()) {
            loginLog.error("FSL 换取 access_token 失败, resp={}", body);
            throw new BizException(ErrorCode.LOGIN_TOKEN_ERROR);
        }
        return accessToken;
    }

    private JsonNode getUserInfo(String accessToken) {
        MarketProperties.Fsl conf = properties.getFsl();
        String url = UriComponentsBuilder.fromHttpUrl(conf.getUrl())
                .path("/account/party/user")
                .build()
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        String body;
        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);
            body = resp.getBody();
        } catch (Exception e) {
            loginLog.error("FSL 拉取用户资料异常 err={}", e.getMessage());
            throw new BizException(ErrorCode.FSL_API_ERROR);
        }
        JsonNode json = parse(body);
        if (json == null) {
            loginLog.error("FSL 用户资料响应异常, resp={}", body);
            throw new BizException(ErrorCode.FSL_API_ERROR);
        }
        JsonNode data = json.path("data");
        if (data.isMissingNode() || data.isNull()) {
            loginLog.error("FSL 用户资料为空, resp={}", body);
            throw new BizException(ErrorCode.FSL_API_ERROR);
        }
        return data;
    }

    /**
     * FSL 各字段名不固定，按配置取地址；没配就留空
     */
    private String readAddress(JsonNode data) {
//        String field = properties.getFsl().getAddressField();
//        if (field == null || field.isBlank()) {
//            return null;
//        }
//        String address = data.path(field.trim()).asText("");
//        return address.isBlank() ? null : address.toLowerCase();
        String address = data.path("walletAddr").path("evm").asText();
        return address.isBlank() ? null : address.toLowerCase();
    }

    private String get(String url) {
        try {
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            loginLog.error("FSL 请求异常 url={} err={}", url, e.getMessage());
            throw new BizException(ErrorCode.FSL_API_ERROR);
        }
    }

    /**
     * 只取字段名用于排查，不打日志输出值
     */
    private String fieldNames(JsonNode data) {
        List<String> names = new ArrayList<>();
        data.fieldNames().forEachRemaining(names::add);
        return String.join(", ", names);
    }

    private JsonNode parse(String body) {
        if (body == null || !body.trim().startsWith("{")) {
            return null;
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            return null;
        }
    }
}
