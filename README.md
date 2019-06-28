## ETHEREUM EXPLORER

Desenvolvido por Roberto Santacroce Martins - 2016

Esse projeto foi desenvolvido para ser um guia inicial ao desenvolvimento de ferramentas que de alguma forma facam interacoes com a rede do Ethereum.

```

MIT License

Copyright (c) 2016 Roberto Santacroce Martins

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

```

## OVERVIEW

Em 2016 quando o código que segue esse documento foi escrito a decisão pelo conhecimento
do time. nesse stack nos levaram a decidir pelas seguintes ferramentas, no entanto, vale ressaltar
que o objetivo principal servir de guia inicial para integração de uma plataforma de ativos digitais
com a rede do blockchain do Ethereum.

### Java + Spring Boot

Camada com o banco de dados standard jpa (facilitou os diversos testes com data
sources distintos)
Pacote para deploy gerado em build com dependências builtin (teoricamente pronto pro
cloud)

### Elasticsearch

Servidor de indexado de dados
Facilmente escalavel
Suporta cargas pesadas
Nao pode ser tido como “verdade” todos os dados sempre serão voláteis aqui

#### RabbitMQ

Facilita a integração de outros componentes para ouvir a rede do Ethereum em diversas
linguagens
Gerencia blocos a serem importados através das filas, com isso nossa aplicação java
pode ir e vir sempre

#### web3j

Lightweight Java and Android library for integration with Ethereum clients [https://github.com/web3j/web3j]

**Como criar transacoes de saque para wallets de clientes (saques)?**

FALTANTE

**Como monitorar as contas em uma rede do Ethereum:**

Vejo algumas alternativas:

1 - Contratar um serviço de Block Explorer como ethscan.io
2 - Usar um servico gratuito de Block Explorer como ethscan.io
3 - Criar um micro serviço que faz a leitura dos blocks a partir de um endereço
4 - Manter infra para rodar o geth e encontrar uma outra forma de indexar o seu blockchain

As duas primeiras opções podem ser usadas para acelerar o processo de desenvolvimento e
testes, no entanto, para termos qualidade penso que o 3 item pode ser o caminho para o
momento, por conta disso, segue um breve descritivo da solução inicial:

## ETHEREUM API BLOCK EXPLORER

```
/balance/{address}
JSON RPC para o saldo de um endereço de ETH

/balance/token/{token_id}/{address}
JSON RPC para o saldo de um endereço de ETH para um token especifico

/txs/{address}
Retorna todas as transações para o endereço

/txs/detail/{tx_id}
Retorna os detalhes de uma transação
```

## SCHEDULED: SERVICOS EM BATCH

Microserviço que roda em tempo especifico, usando o JSON RPC (getBlock) do Ethereum para
recuperar todos os blocos, fazendo um log de todas as transações presentes no bloco que
pertencerem a uma lista especifica de endereços previamente cadastrados, alguns exemplos de
mensagens:

GetBlock (Número em hexadecimal, eth_getBlockByNumber)
```
{"jsonrpc":"2.0",
"method":"eth_getBlockByNumber",
"params": [ "0x1b4", true],
"id":67}
```
GetTransaction (eth_getTransactionByHash)
```
{"jsonrpc":"2.0",
"method":"eth_getTransactionByHash",
"params":
[ "0x06f1dd4bef8c647bb549fb1a767e4863f32564ed0f5fafdc5648e3d083a37052"],
"id":67}
```
## MICROSERVIÇO PARA LER OS BLOCOS

Block Inicial sempre deve ser informado como parametro.

Um exemplo usando a biblioteca web3j para pegar 100 blocos a partir do block inicial 1550000
```
BigInteger blockNumber = BigInteger.valueOf(1550000);
for (int x = 0; x < 100; x++) {
blockNumber = blockNumber.add(BigInteger.ONE);
System.out.println("BlockNumber: " + blockNumber);
DefaultBlockParameter blockParameter =
DefaultBlockParameter.valueOf(blockNumber);
Request<?, EthBlock> ethGetBlockByNumber =
web3.ethGetBlockByNumber(blockParameter, true);
EthBlock send = ethGetBlockByNumber.send();

if (send.getBlock().getTransactions() != null) {
for (TransactionResult t : send.getBlock().getTransactions()) {
TransactionObject transaction = (TransactionObject) t.get();
String from = transaction.getFrom();
String to = transaction.getTo();
String valueRaw = transaction.getValueRaw();
BigInteger value = transaction.getValue();
BigInteger gas = transaction.getGas();
BigInteger gasPrice = transaction.getGasPrice();
String txHash = transaction.getHash();
String blockHash = transaction.getBlockHash();
BigInteger blockNumber2 = transaction.getBlockNumber();

System.out.println(String.format(
"from: %s, to: %s, valueRaw: %s, value: %s,
gas: %s, gasPrice: %s, txHash: %s, blockHash: %s, blockNumber: %s, currentBlockNumber:
%s",
from, to, valueRaw, value, gas, gasPrice,
txHash, blockHash, blockNumber2, blockNumber));
}
}
}
```

## INTERAGINDO COM OS TOKENS

```
private Function totalSupply() {
return new Function("totalSupply", Collections.emptyList(),
Collections.singletonList(new TypeReference<Uint256>() {
}));
}

private Function tokenName() {
return new Function("name", Collections.emptyList(), Collections.singletonList(new
TypeReference<Utf8String>() {
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

```
## COMO CHAMAR FUNÇÃO DE UM SMART CONTRACT

```
private String callSmartContractFunction(Function function, String address, String
contractAddress)
throws Exception {
String encodedFunction = FunctionEncoder.encode(function);

org.web3j.protocol.core.methods.response.EthCall response = web
.ethCall(Transaction.createEthCallTransaction(address,
contractAddress, encodedFunction),
DefaultBlockParameterName.LATEST)
.sendAsync().get();

return response.getValue();
}
```

## CRIANDO UM ENDEREÇO NOVO NO ETHEREUM

```
// creates a new wallet
ECKeyPair ecKeyPair = Keys.createEcKeyPair();
BigInteger publicKey = ecKeyPair.getPublicKey();
String publicKeyHex = Numeric.toHexStringWithPrefix(publicKey);
BigInteger privateKey = ecKeyPair.getPrivateKey();
String privateKeyHex = Numeric.toHexStringWithPrefix(privateKey);
System.out.println("PublicKey: " + publicKeyHex);
System.out.println("PrivateKey: " + privateKeyHex);

WalletFile walletFile = Wallet.createStandard("password", ecKeyPair);
ByteArrayOutputStream bas = new ByteArrayOutputStream();
objectMapper.writeValue(bas, walletFile);


// convert to a json object
JSONObject json = new JSONObject(new String(bas.toByteArray()));
System.out.println(json.toString(4));

// load wallet from json wallet file
WalletFile wf = objectMapper.readValue(json.toString(), WalletFile.class);
ECKeyPair decrypt = Wallet.decrypt("password", wf);

publicKey = decrypt.getPublicKey();
publicKeyHex = Numeric.toHexStringWithPrefix(publicKey);

privateKey = decrypt.getPrivateKey();
privateKeyHex = Numeric.toHexStringWithPrefix(privateKey);

System.out.println("PublicKey: " + publicKeyHex);
System.out.println("PrivateKey: " + privateKeyHex);

Credentials c = Credentials.create(decrypt);
System.out.println("Address: " + c.getAddress());

```

## FAZENDO A CONFIRMACAO DE TRANSACOES PARA UM ERC-

Para confirmar transacoes de um token erc-20 voce deve ler os logs e eventos do smart contract,
para isso basta solicitar o TransactionReceipt para o node, segue alguns exemplos de código que
fazem a tarefa de processar as transacoes:

```
**private EthereumTransaction processTransaction(Transaction t) {**

final EthereumTransaction e = new EthereumTransaction();
String txId = t.getTxId();
logger.info(t.getTxId());
try {

Optional<org.web3j.protocol.core.methods.response.Transaction> tx =
web3().ethGetTransactionByHash(txId).send().getTransaction();

EthGetTransactionReceipt transactionReceipt =
web3().ethGetTransactionReceipt(txId).send();

TransactionReceipt r = transactionReceipt.getTransactionReceipt().get();
BigInteger confirmations =
web3().ethBlockNumber().send().getBlockNumber().subtract(tx.get().getBlockNumber());

e.setTxId(txId);
e.setBlockNumber(r.getBlockNumber());
e.setFromAddress(r.getFrom());
e.setToAddress(r.getTo());
e.setTokenSymbol(Currencies.ETH.getSymbol());
e.setConfirmations(confirmations.intValue());
BigDecimal amount = new BigDecimal(tx.get().getValue()).scaleByPowerOfTen(-
Currencies.ETH.getDecimals());
e.setValue(amount);

} catch (IOException ex) {
ex.printStackTrace();
}


return e;
}

**private EthereumTransaction processERC20Transaction(Transaction t, String
contractAddress) {**

final EthereumTransaction e = new EthereumTransaction();
String txId = t.getTxId();
logger.info(t.getTxId());
try {

Optional<org.web3j.protocol.core.methods.response.Transaction> tx =
web3().ethGetTransactionByHash(txId).send().getTransaction();

EthGetTransactionReceipt transactionReceipt =
web3().ethGetTransactionReceipt(txId).send();

TransactionReceipt r = transactionReceipt.getTransactionReceipt().get();

Currency currency = Currencies.getCurrencyFromContractAddress(contractAddress);
BigInteger confirmations =
web3().ethBlockNumber().send().getBlockNumber().subtract(tx.get().getBlockNumber());

r.getLogs().parallelStream().forEach(l -> {
processEthereumERC20Logs(l, currency, confirmations, txId, contractAddress, e, r);
});

} catch (IOException ex) {
ex.printStackTrace();
}

return e;
}

```

## QUANTIDADE DE ERC-20 DA TX

```

**private void processEthereumERC20Logs(Log l, Currency currency, BigInteger
confirmations, String txId, String contractAddress, EthereumTransaction e,
TransactionReceipt r) {**
List<String> topics = l.getTopics();
String data = l.getData();

Event transferEvent = transferEvent();
String encodedEventSignature = EventEncoder.encode(transferEvent);

List<Type> results = FunctionReturnDecoder.decode(
data, transferEvent.getNonIndexedParameters());
results.forEach(x -> {

BigDecimal bValue = new BigDecimal(x.getValue().toString());

e.setValue(bValue);
});

Address from = new Address(topics.get(1));
Address to = new Address(topics.get(2));


e.setTxId(txId);
e.setBlockNumber(r.getBlockNumber());
e.setFromAddress(from.getValue());

```


## BUILD AND RUN

```

./gradlew clean build
./gradlew bootRun

cd deploy

docker-compose -f rabbitmq.yml up -d
docker-compose -f elasticsearch.yml up -d
docker-compose -f eth-explorer.yml up -d --build

```

## ARQUIVO DE CONFIGURACAO DE EXEMPLO

```

explorer.geth.hostname=PODE SER UMA URL DO INFURA COM SUA API KEY
spring.main.web-environment=true
server.port=8000

spring.data.elasticsearch.cluster-name=app-monitor
spring.data.elasticsearch.cluster-nodes=localhost:9300
spring.data.elasticsearch.repositories.enabled=true

spring.rabbitmq.dynamic=true
spring.rabbitmq.addresses=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

```


