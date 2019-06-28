package bexchange.eth.repositories;

import bexchange.eth.models.EthereumTransaction;
import org.springframework.data.elasticsearch.repository.ElasticsearchCrudRepository;

import java.util.List;
import java.util.Optional;

public interface EthereumTransactionRepository extends ElasticsearchCrudRepository<EthereumTransaction, String> {

    List<EthereumTransaction> findAllByFromAddressEqualsOrToAddressEquals(String address, String address2);

    Optional<EthereumTransaction> findById(String txId);
}
