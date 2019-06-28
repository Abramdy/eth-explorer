package bexchange.eth.models;

import java.math.BigInteger;

public class QueryResultField {
    private BigInteger blockNumber;

    public QueryResultField() {

    }

    public BigInteger getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(BigInteger blockNumber) {
        this.blockNumber = blockNumber;
    }

}
