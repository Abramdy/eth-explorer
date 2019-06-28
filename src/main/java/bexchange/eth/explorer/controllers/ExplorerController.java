package bexchange.eth.explorer.controllers;

import bexchange.eth.config.RabbitMQConfiguration;
import bexchange.eth.models.EthereumTransaction;
import bexchange.eth.models.TokenDetail;
import bexchange.eth.services.EthereumWalletService;
import bexchange.eth.services.ExplorerService;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.web3j.protocol.core.methods.response.EthGetBalance;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

/***
 *
 * @author Roberto Santacroce Martins
 *
 */
@CrossOrigin
@Controller
@RequestMapping("/")
public class ExplorerController {

    private static Logger logger = org.slf4j.LoggerFactory.getLogger(ExplorerController.class);
    private ObjectMapper mapper = new ObjectMapper(new JsonFactory());

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ExplorerService explorerService;

    @Autowired
    private EthereumWalletService ethereumService;

    @RequestMapping(value = "/blocknumber", method = RequestMethod.GET)
    @ResponseBody
    public String blocknumber(HttpServletResponse response) throws IOException {
        Long ethBlockNumber = explorerService.getLastProcessedBlockNumber();
        JSONObject json = new JSONObject();
        json.put("lastBlockNumber", ethBlockNumber.toString());
        return json.toString();
    }

    @RequestMapping(value = "/blocknumber/{blockNumber}", method = RequestMethod.POST)
    @ResponseBody
    public String processBlockNumber(@PathVariable("blockNumber") String blockNumber, HttpServletResponse response) throws IOException {

        JSONObject json = new JSONObject();

        try {
            BigInteger b = new BigInteger(blockNumber);
            json.put("blockNumber", b);
            rabbitTemplate.convertAndSend(RabbitMQConfiguration.processBlockNumber, json.toString());

        } catch (Exception ex) {
            json.put("blockNumber", "Invalid block number.");
        }

        return json.toString();
    }

    @RequestMapping(value = "/token/{address}", method = RequestMethod.GET)
    @ResponseBody
    public String tokenDetail(@PathVariable("address") String address, HttpServletResponse response) throws Exception {
        TokenDetail tokenDetail = explorerService.getTokenDetails(address);
        JSONObject json = new JSONObject(tokenDetail);
        response.setHeader("Content-Type", "application/json");
        return json.toString(4);
    }

    @RequestMapping(value = "/balance/{address}", method = RequestMethod.GET)
    @ResponseBody
    public String balance(@PathVariable("address") String address, HttpServletResponse response) throws IOException {
        EthGetBalance ethereumBalance = explorerService.getEthereumBalance(address);
        return ethereumBalance.getBalance().toString();
    }

    @RequestMapping(value = "/balance/{tokenAddress}/{address}", method = RequestMethod.GET)
    @ResponseBody
    public String tokenBalance(@PathVariable("tokenAddress") String tokenAddress,
                               @PathVariable("address") String address, HttpServletResponse response) throws Exception {

        String balanceOf = ethereumService.balanceOf(tokenAddress, address);
        return balanceOf;
    }

    @RequestMapping(value = "/txs/{address}", method = RequestMethod.GET)
    @ResponseBody
    public String transactions(@PathVariable("address") String address, HttpServletResponse response)
            throws JsonProcessingException {

        List<EthereumTransaction> list = explorerService.listTransactionsByAddress(address);
        String writeValueAsString = mapper.writeValueAsString(list);
        return writeValueAsString;
    }

    @RequestMapping(value = "/txs/detail/{tx_id}", method = RequestMethod.GET)
    @ResponseBody
    public String transactionDetail(@PathVariable("tx_id") String txId, HttpServletResponse response)
            throws JsonProcessingException {
        Optional<EthereumTransaction> transaction = explorerService.getTransaction(txId);
        if (transaction.isPresent()) {
            String writeValueAsString = mapper.writeValueAsString(transaction.get());
            return writeValueAsString;
        } else {
            return null;
        }
    }

}
