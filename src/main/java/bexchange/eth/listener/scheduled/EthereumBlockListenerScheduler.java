package bexchange.eth.listener.scheduled;

import bexchange.eth.config.RabbitMQConfiguration;
import bexchange.eth.models.EthereumBlock;
import bexchange.eth.models.EthereumTransaction;
import bexchange.eth.services.ExplorerService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthBlock.Block;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;


@Service
public class EthereumBlockListenerScheduler {

    private static Logger logger = LoggerFactory.getLogger(EthereumBlockListenerScheduler.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ExplorerService explorerService;

    @Value("${explorer.geth.hostname:localhost}")
    private String gethHostname;

    private ConcurrentSkipListSet<BigInteger> pendingBlocks = new ConcurrentSkipListSet<BigInteger>();

    private long lastBlockNumber = 0;

    private Web3j _web3;

    private Web3j web3() {
        if (_web3 == null) {
            _web3 = Web3j.build(new HttpService(gethHostname));
        }
        return _web3;
    }

    @PostConstruct
    public void post() {

    }

    @Scheduled(fixedDelay = 10000)
    public void run() throws IOException {

        EthBlockNumber bn = web3().ethBlockNumber().send();
        lastBlockNumber = bn.getBlockNumber().longValue();

        Block retrievedBlock = getEthereumBlock(BigInteger.valueOf(lastBlockNumber));

        if (retrievedBlock != null) {
            processEthereumBlock(retrievedBlock);
        } else {
            pendingBlocks.add(bn.getBlockNumber());
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void processPendingBlocks() throws IOException {

        pendingBlocks.stream().forEach(x -> {
            Block ethereumBlock = getEthereumBlock(x);
            if (ethereumBlock != null) {
                processEthereumBlock(ethereumBlock);
                pendingBlocks.remove(x);
            }
        });

    }

    private void processEthereumBlock(Block retrievedBlock) {
        logger.info("Processing block number: " + retrievedBlock.getNumber());

        EthereumBlock fromWeb3Block = EthereumBlock.fromWeb3Block(retrievedBlock);
        JSONObject json = new JSONObject(fromWeb3Block);

        List<EthereumTransaction> transactions = EthereumTransaction.fromWeb3Block(retrievedBlock);
        json.put("transactions", transactions);

        rabbitTemplate.convertAndSend(RabbitMQConfiguration.ethBlocksQueueName, json.toString());

        pendingBlocks.remove(retrievedBlock.getNumber());

    }

    private Block getEthereumBlock(BigInteger blockNumber) {

        try {
            DefaultBlockParameter blockParameter = DefaultBlockParameter.valueOf(blockNumber);
            Request<?, EthBlock> ethGetBlockByNumber = web3().ethGetBlockByNumber(blockParameter, true);
            Block retrievedBlock = null;

            retrievedBlock = ethGetBlockByNumber.send().getBlock();
            return retrievedBlock;

        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                Block ethereumBlock = getEthereumBlock(blockNumber);
                pendingBlocks.remove(blockNumber);
                return ethereumBlock;
            } catch (Exception ex1) {
                pendingBlocks.add(blockNumber);
                ex1.printStackTrace();
            }

        }

        return null;
    }

    public void addPendingBlock(BigInteger blockNumber) {
        this.pendingBlocks.add(blockNumber);
    }
}
