package bexchange.eth.services;

import bexchange.eth.models.TokenDetail;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Collections;
import java.util.List;

@Component
public class EthereumWalletService {

    private static final int N_STANDARD = 1 << 18;
    private static final int P_STANDARD = 1;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private Logger logger = LoggerFactory.getLogger(EthereumWalletService.class);
    @Value("${explorer.geth.hostname:localhost}")
    private String gethHostname;

    private Web3j web3;

    @PostConstruct
    public void post() throws IOException {
        web3 = Web3j.build(new HttpService(gethHostname));
        Web3ClientVersion web3ClientVersion = web3.web3ClientVersion().send();
        logger.info(web3ClientVersion.getResult());
    }

    public String balanceOf(String tokenAddress, String address) throws Exception {
        return confirmBalance(address, tokenAddress);
    }

    public TokenDetail getERC20TokenDetails(String tokenAddress) throws Exception {

        TokenDetail td = new TokenDetail();
        String name = callSmartContractFunctionAndGetResult(tokenName(), tokenAddress);
        td.setName(name);
        String totalSupply = callSmartContractFunctionAndGetResult(totalSupply(), tokenAddress);
        td.setTotalSupply(totalSupply);
        String symbol = callSmartContractFunctionAndGetResult(tokenSymbol(), tokenAddress);
        td.setSymbol(symbol);

        String decimals = callSmartContractFunctionAndGetResult(tokenDecimals(), tokenAddress);
        td.setDecimals(decimals);

        return td;
    }

    private Function tokenDecimals() {
        return new Function("decimals", Collections.emptyList(),
                Collections.singletonList(new TypeReference<Uint256>() {
                }));
    }

    private String callSmartContractFunctionAndGetResult(Function function, String tokenAddress) throws Exception {
        String responseValue = callSmartContractFunction(function, tokenAddress, tokenAddress);
        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
        return response.get(0).getValue().toString();

    }

    private void createEthereumAddress() throws IOException, NoSuchAlgorithmException, NoSuchProviderException,
            InvalidAlgorithmParameterException, CipherException {

        // creates a new wallet
        ECKeyPair ecKeyPair = Keys.createEcKeyPair();
        BigInteger publicKey = ecKeyPair.getPublicKey();
        String publicKeyHex = Numeric.toHexStringWithPrefix(publicKey);
        BigInteger privateKey = ecKeyPair.getPrivateKey();
        String privateKeyHex = Numeric.toHexStringWithPrefix(privateKey);

        WalletFile walletFile = Wallet.createStandard("password", ecKeyPair);
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        objectMapper.writeValue(bas, walletFile);

        // convert to a json object
        JSONObject json = new JSONObject(new String(bas.toByteArray()));

        // load wallet from json wallet file
        WalletFile wf = objectMapper.readValue(json.toString(), WalletFile.class);
        ECKeyPair decrypt = Wallet.decrypt("password", wf);

        publicKey = decrypt.getPublicKey();
        publicKeyHex = Numeric.toHexStringWithPrefix(publicKey);

        privateKey = decrypt.getPrivateKey();
        privateKeyHex = Numeric.toHexStringWithPrefix(privateKey);

        Credentials c = Credentials.create(decrypt);

    }

    private Function totalSupply() {
        return new Function("totalSupply", Collections.emptyList(),
                Collections.singletonList(new TypeReference<Uint256>() {
                }));
    }

    private Function tokenName() {
        return new Function("name", Collections.emptyList(), Collections.singletonList(new TypeReference<Utf8String>() {
        }));
    }

    private Function tokenSymbol() {
        return new Function("symbol", Collections.emptyList(),
                Collections.singletonList(new TypeReference<Utf8String>() {
                }));
    }

    private Function balanceOf(String owner) {
        return new Function("balanceOf", Collections.singletonList(new Address(owner)),
                Collections.singletonList(new TypeReference<Uint256>() {
                }));
    }

    private String confirmBalance(String address, String contractAddress) throws Exception {
        Function function = balanceOf(address);
        String responseValue = callSmartContractFunction(function, address, contractAddress);
        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());

        return response.get(0).getValue().toString();
    }

    private String callSmartContractFunction(Function function, String address, String contractAddress)
            throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);

        org.web3j.protocol.core.methods.response.EthCall response = web3
                .ethCall(Transaction.createEthCallTransaction(address, contractAddress, encodedFunction),
                        DefaultBlockParameterName.LATEST)
                .sendAsync().get();

        return response.getValue();
    }

}
