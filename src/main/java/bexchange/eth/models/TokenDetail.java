package bexchange.eth.models;

public class TokenDetail {

    private String name;
    private String symbol;
    private String totalSupply;
    private String decimals;

    public TokenDetail(String name, String symbol, String totalSupply, String decimals) {
        this.name = name;
        this.symbol = symbol;
        this.totalSupply = totalSupply;
        this.decimals = decimals;
    }

    public TokenDetail() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getTotalSupply() {
        return totalSupply;
    }

    public void setTotalSupply(String totalSupply) {
        this.totalSupply = totalSupply;
    }

    public String getDecimals() {
        return decimals;
    }

    public void setDecimals(String decimals) {
        this.decimals = decimals;
    }
}

