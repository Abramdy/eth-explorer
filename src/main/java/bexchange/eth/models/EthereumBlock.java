package bexchange.eth.models;

import org.json.JSONObject;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.web3j.protocol.core.methods.response.EthBlock.Block;

import java.io.IOException;
import java.util.Calendar;

@Document(indexName = "eth_block", type = "eth_block")
public class EthereumBlock {

    @Id
    private String id;
    private String hash;
    @Field(type = FieldType.Long)
    private Long blockNumber;
    private String createdAt;

    public static EthereumBlock fromWeb3Block(Block retrievedBlock) {
        EthereumBlock block = new EthereumBlock();
        block.setId(retrievedBlock.getHash());
        block.setId(retrievedBlock.getHash());
        block.setBlockNumber(retrievedBlock.getNumber().longValue());
        block.setHash(retrievedBlock.getHash());
        block.setCreatedAt(Calendar.getInstance().getTime().toString());
        return block;
    }

    public static EthereumBlock fromJSON(JSONObject json) throws IOException {
        EthereumBlock block = new EthereumBlock();

        block.setId(json.getString("hash"));
        block.setHash(json.getString("hash"));
        block.setBlockNumber(json.getLong("blockNumber"));
        block.setCreatedAt(json.getString("createdAt"));

        return block;
    }

    public Long getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(Long blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
