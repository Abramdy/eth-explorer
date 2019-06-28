package bexchange.eth.services;

import bexchange.eth.models.EthereumBlock;
import bexchange.eth.models.EthereumTransaction;
import bexchange.eth.models.QueryResultField;
import bexchange.eth.models.TokenDetail;
import bexchange.eth.repositories.EthereumBlockRepository;
import bexchange.eth.repositories.EthereumTransactionRepository;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.elasticsearch.search.sort.SortBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ResultsExtractor;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.elasticsearch.index.query.QueryBuilders.*;

@Component
public class ExplorerService {

    private static final String INFINITY = "-Infinity";
    @Autowired
    public ElasticsearchTemplate template;
    private Logger logger = LoggerFactory.getLogger(ExplorerService.class);
    @Autowired
    private EthereumBlockRepository blockRepository;

    @Autowired
    private EthereumTransactionRepository transactionRepository;

    @Autowired
    private EthereumWalletService ethereumWalletService;

    @Value("${explorer.geth.hostname:localhost}")
    private String gethHostname;

    private Web3j web3;

    @PostConstruct
    public void post() throws IOException {
        web3 = Web3j.build(new HttpService(gethHostname));
        Web3ClientVersion web3ClientVersion = web3.web3ClientVersion().send();
        logger.info(web3ClientVersion.getResult());
    }

    /**
     * Returns Ethereum current Block Number
     *
     * @return
     * @throws IOException
     */
    public BigInteger blockNumber() throws IOException {
        return web3.ethBlockNumber().send().getBlockNumber();
    }

    public List<BigInteger> findPendingImportedBlock() {
        List<BigInteger> result = new ArrayList<>();

        // given
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
                .withSearchType(SearchType.DEFAULT).withIndices("eth_block").withTypes("eth_block")
                .withFields("blockNumber").withSort(SortBuilders.fieldSort("blockNumber")).build();

        List<QueryResultField> queryForList = template.queryForList(searchQuery, QueryResultField.class);

        int size = queryForList.size();
        for (int x = 0; x < size; x++) {
            BigInteger cur = queryForList.get(x).getBlockNumber();

            int iNext = x + 1;
            if (iNext >= size) {
                continue;
            }
            BigInteger next = queryForList.get(iNext).getBlockNumber();

            BigInteger r = cur.subtract(next);
            if (r.compareTo(BigInteger.ZERO) < 0) {
                for (int y = 1; y <= Math.abs(r.intValue()); y++) {
                    result.add(cur.add(BigInteger.valueOf(y)));
                }
            }
        }

        return result;
    }

    public Long getLastProcessedBlockNumber() throws IOException {

        // given
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
                .withSearchType(SearchType.DEFAULT).withIndices("eth_block").withTypes("eth_block")
                .addAggregation(AggregationBuilders.max("max_blocknumber").field("blockNumber")).build();
        // when
        Aggregations aggregations = template.query(searchQuery, new ResultsExtractor<Aggregations>() {
            @Override
            public Aggregations extract(SearchResponse response) {
                return response.getAggregations();
            }
        });

        // then
        Max aggregation = aggregations.get("max_blocknumber");

        if (aggregation.getValueAsString().equals(INFINITY)) {
            return 0L;
        }

        Double number = aggregation.getValue();
        return number.longValue();
    }

    public Iterable<EthereumBlock> listAllBlocks() {
        return blockRepository.findAll();
    }

    @Transactional
    public void saveBlock(EthereumBlock block) {
        Optional<EthereumBlock> findOne = blockRepository.findById(block.getHash());
        if (findOne.isPresent()) {
            block.setId(findOne.get().getId());
        }

        EthereumBlock save = blockRepository.save(block);
        logger.info("Block " + block.getBlockNumber() + " imported " + save.getId());
    }

    public EthGetBalance getEthereumBalance(String address) throws IOException {
        return web3.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
    }

    public List<EthereumTransaction> listTransactionsByAddress(String address) {
        return transactionRepository.findAllByFromAddressEqualsOrToAddressEquals(address, address);
    }

    public Optional<EthereumTransaction> getTransaction(String txId) {
        return transactionRepository.findById(txId);
    }

    public void saveTransactions(ArrayList<EthereumTransaction> transactions) {

        for (EthereumTransaction t : transactions) {
            Optional<EthereumTransaction> findOne = transactionRepository.findById(t.getTxId());
            if (findOne.isPresent()) {
                return;
            }

            transactionRepository.save(t);
        }
    }

    public EthereumBlock getBlockByNumber(BigInteger blockNumber) {

        SearchQuery searchQuery = new NativeSearchQueryBuilder().withSearchType(SearchType.DEFAULT)
                .withIndices("eth_block").withTypes("eth_block")
                .withFilter(boolQuery().must(matchQuery("blockNumber", blockNumber.longValueExact()))).build();

        List<EthereumBlock> blocks = template.queryForList(searchQuery, EthereumBlock.class);

        if (blocks == null || blocks.size() == 0)
            return null;

        return blocks.get(0);
    }

    public TokenDetail getTokenDetails(String tokenAddress) throws Exception {
        return ethereumWalletService.getERC20TokenDetails(tokenAddress);
    }


}
