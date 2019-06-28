package bexchange.eth.storage.scheduled;

import bexchange.eth.models.EthereumBlock;
import bexchange.eth.models.EthereumTransaction;
import bexchange.eth.services.ExplorerService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;

@Component
public class EthereumBlockListenerQueuesReceiver {

    private static Logger logger = LoggerFactory.getLogger(EthereumBlockListenerQueuesReceiver.class);

    @Autowired
    private ExplorerService explorerService;

    @Value("${explorer.geth.hostname:localhost}")
    private String gethHostname;

    private Web3j web3;

    @PostConstruct
    public void post() throws IOException {
        web3 = Web3j.build(new HttpService(gethHostname));
        Web3ClientVersion web3ClientVersion = web3.web3ClientVersion().send();
    }

    public void handleMessage(String message) throws IOException {
        JSONObject json = new JSONObject(message);
        JSONArray transactions = null;

        if (json.has("transactions"))
            transactions = json.getJSONArray("transactions");

        EthereumBlock block = EthereumBlock.fromJSON(json);
        EthereumBlock exist = explorerService.getBlockByNumber(BigInteger.valueOf(block.getBlockNumber()));
        if (exist == null) {
            explorerService.saveBlock(block);
        } else {
            logger.info("Block already imported: " + block.getBlockNumber());
        }

        if (transactions != null) {

            ArrayList<EthereumTransaction> transactions1 = EthereumTransaction.fromJSON(transactions);
            explorerService.saveTransactions(transactions1);
        }
    }

}
