package funshop;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="paymentLists", path="paymentLists")
public interface PaymentListRepository extends PagingAndSortingRepository<PaymentList, Long>{


}
