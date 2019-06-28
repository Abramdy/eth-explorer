package bexchange.eth.models;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.web3j.protocol.core.methods.response.EthBlock.Block;
import org.web3j.protocol.core.methods.response.EthBlock.TransactionObject;
import org.web3j.protocol.core.methods.response.EthBlock.TransactionResult;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Document(indexName = "eth_tx", type = "eth_tx")
public class EthereumTransaction {

    @Id
    private String txId;
    private BigInteger blockNumber;
    private String fromAddress;
    private String toAddress;
    private String createdAt;

    public static List<EthereumTransaction> fromWeb3Block(Block block) {

        ArrayList<EthereumTransaction> list = new ArrayList<>();
        for (TransactionResult result : block.getTransactions()) {
            TransactionObject t = (TransactionObject) result.get();

            EthereumTransaction et = new EthereumTransaction();
            et.setBlockNumber(t.getBlockNumber());
            et.setToAddress(t.getTo());
            et.setFromAddress(t.getFrom());
            et.setTxId(t.getHash());
            et.setCreatedAt(Calendar.getInstance().getTime().toString());

            list.add(et);

        }
        return list;
    }

    public static ArrayList<EthereumTransaction> fromJSON(JSONArray transactions)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper(new JsonFactory());
        ArrayList<EthereumTransaction> list = new ArrayList<>();
        int size = transactions.length();
        for (int x = 0; x < size; x++) {
            JSONObject jsonObject = transactions.getJSONObject(x);

            EthereumTransaction et = new EthereumTransaction();
            et.setBlockNumber(jsonObject.getBigInteger("blockNumber"));
            et.setFromAddress(jsonObject.getString("fromAddress"));
            et.setToAddress(jsonObject.optString("toAddress"));
            et.setTxId(jsonObject.getString("txId"));
            et.setCreatedAt(Calendar.getInstance().getTime().toString());

            list.add(et);

        }
        return list;

    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public BigInteger getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(BigInteger blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

}
