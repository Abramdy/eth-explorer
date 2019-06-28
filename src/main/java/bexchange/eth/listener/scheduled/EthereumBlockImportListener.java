package bexchange.eth.listener.scheduled;

import bexchange.eth.config.RabbitMQConfiguration;
import bexchange.eth.models.EthereumBlock;
import bexchange.eth.models.EthereumTransaction;
import bexchange.eth.services.ExplorerService;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

@Component
public class EthereumBlockImportListener {

    private static Logger logger = LoggerFactory.getLogger(EthereumBlockImportListener.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ExplorerService explorerService;

    @Value("${explorer.geth.hostname:localhost}")
    private String gethHostname;

    private Web3j _web3;

    private Web3j web3() {
        if (_web3 == null) {
            _web3 = Web3j.build(new HttpService(gethHostname));
        }
        return _web3;
    }


    @PostConstruct
    public void post() throws IOException {

    }

    @RabbitListener(queues = "eth-process-block")
    public void handleMessage(String message) throws IOException {
        JSONObject json = new JSONObject(message);
        BigInteger blockNumber = BigInteger.ONE;
        try {
            blockNumber = json.getBigInteger("blockNumber");
        } catch (JSONException jex) {
            logger.info("Invalid blocknumber: " + json.toString());
            return;
        }

        logger.info("Importing block number: " + blockNumber.toString());

        EthereumBlock exist = explorerService.getBlockByNumber(blockNumber);
        if (exist == null) {
            EthBlock.Block ethereumBlock = getEthereumBlock(blockNumber);
            if (ethereumBlock != null) {
                processEthereumBlock(ethereumBlock);
            }
        } else {
            logger.info("Block already imported: " + blockNumber);
        }

    }

    private void processEthereumBlock(EthBlock.Block retrievedBlock) {
        EthereumBlock fromWeb3Block = EthereumBlock.fromWeb3Block(retrievedBlock);
        JSONObject json = new JSONObject(fromWeb3Block);

        List<EthereumTransaction> transactions = EthereumTransaction.fromWeb3Block(retrievedBlock);
        json.put("transactions", transactions);

        rabbitTemplate.convertAndSend(RabbitMQConfiguration.ethBlocksQueueName, json.toString());
    }

    private EthBlock.Block getEthereumBlock(BigInteger blockNumber) {

        try {
            DefaultBlockParameter blockParameter = DefaultBlockParameter.valueOf(blockNumber);
            Request<?, EthBlock> ethGetBlockByNumber = web3().ethGetBlockByNumber(blockParameter, true);
            EthBlock.Block retrievedBlock = null;

            retrievedBlock = ethGetBlockByNumber.send().getBlock();
            return retrievedBlock;

        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                EthBlock.Block ethereumBlock = getEthereumBlock(blockNumber);
                return ethereumBlock;
            } catch (Exception ex1) {
                ex1.printStackTrace();
            }

        }

        logger.info("Blocknumber not found: " + blockNumber);
        return null;
    }

}
