package com.mooar.market.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NftMetadataService {

    private static final Logger log = LoggerFactory.getLogger(NftMetadataService.class);

    private final Web3j web3j;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 查 721 tokenURI 并抓取 metadata JSON
     */
    public String fetchMetadata(String contract, long tokenId) {
        String uri = callTokenUri(contract, tokenId);
        if (uri == null || uri.isBlank()) {
            return "";
        }
        String httpUrl = normalize(uri);
        if (httpUrl == null) {
            return uri;
        }
        try {
            return restTemplate.getForObject(httpUrl, String.class);
        } catch (Exception e) {
            log.warn("fetch nft metadata fail uri={}", httpUrl);
            return "";
        }
    }

    private String callTokenUri(String contract, long tokenId) {
        try {
            List<Type> inputParams = new ArrayList<>();
            inputParams.add(new Uint256(tokenId));
            List<TypeReference<?>> outputParams = new ArrayList<>();
            outputParams.add(new TypeReference<Utf8String>() {
            });
            Function function = new Function("tokenURI", inputParams, outputParams);
            String data = FunctionEncoder.encode(function);
            EthCall call = web3j.ethCall(
                    Transaction.createEthCallTransaction(null, contract, data),
                    DefaultBlockParameterName.LATEST).send();
            List<Type> decoded = FunctionReturnDecoder.decode(call.getValue(), function.getOutputParameters());
            if (decoded.isEmpty()) {
                return null;
            }
            return decoded.get(0).getValue().toString();
        } catch (Exception e) {
            log.warn("call tokenURI fail contract={} tokenId={}", contract, tokenId);
            return null;
        }
    }

    private String normalize(String uri) {
        if (uri.startsWith("ipfs://")) {
            return "https://ipfs.io/ipfs/" + uri.substring(7);
        }
        if (uri.startsWith("https://") || uri.startsWith("http://")) {
            return uri;
        }
        return null;
    }

    public String parseImage(String metadata) {
        return null;
    }
}
