package bexchange.eth.repositories;

import bexchange.eth.models.EthereumBlock;
import org.springframework.data.elasticsearch.repository.ElasticsearchCrudRepository;

import java.util.Optional;

public interface EthereumBlockRepository extends ElasticsearchCrudRepository<EthereumBlock, String> {

    Optional<EthereumBlock> findById(String hash);
}
