![로고](https://user-images.githubusercontent.com/87048674/131634949-6eaffd62-c54d-46ec-8d9f-2da9a2796eef.png)

# 쇼핑몰 (Funshop)

본 예제는 MSA/DDD/Event Storming/EDA 를 포괄하는 분석/설계/구현/운영 전단계를 커버하도록 구성한 예제입니다. 이는 클라우드 네이티브 애플리케이션의 개발에 요구되는 체크포인트들을 통과하기 위한 예시 답안을 포함합니다.

- 체크포인트 : https://workflowy.com/s/assessment-check-po/T5YrzcMewfo4J6LW

# Table of Contents

- 서비스 시나리오  
- 분석/설계  

- 구현  
     · DDD 의 적용       
     · CQRS  
     · Req/Resp  
     · Correlation       
     · Gateway  
     · Polyglot  

- 운영  
     · Deploy  
     · Circuit Breaker  
     · Autoscale (HPA)  
     · Zero-Downtime deploy (Readiness probe)  
     · Persistence Volume  
     · Self-healing(Liveness probe)  
     · ConfigMap   
     
     


# 서비스 시나리오

- 기능적 요구사항  

1. 고객이 상품을 선택하여 주문한다.  
2. 주문한 상품은 Cart에 담겨서 결제를 기다린다. 
3. 고객이 주문에 대한 결제를 한다.
4. 결제가 승인되면 Cart의 상품이 주문 완료된다 
5. 고객이 주문을 취소할 수 있다.  
6. 주문이 취소되면 결제가 취소된다.
7. 상품 정보 및 주문/Cart 상태를 조회 할 수 있다.(viewpage)  


- 비기능적 요구사항  
     · 장애격리 : 과도한 주문요청 Traffic 발생 시 Circuit breaker 를 통해 장애 확대를 회피한다.  
     · Autoscale : 과도한 주문요청 Traffic 발생 시, 추가 자원할당을 통해 서비스 안정성을 확보한다.  
     · 무정지배포 : 배포시 서비스 중단이 없도록 한다.  
     · Self-healing : 서비스의 장애여부를 지속적으로 확인하고, 장애 서비스는 제외한다.  



# 분석/설계  

## Event Storming

- MSAEZ에서 Event Storming 수행  
- Event 도출  
![m1](https://user-images.githubusercontent.com/87048674/131806066-b0ba60c6-bb55-4b60-9741-3a31bb8d4a21.png)

- Command 부착  
![m2](https://user-images.githubusercontent.com/87048674/131806070-1bc85317-1070-4789-9119-d053fb22eb75.png)

- Actor, Policy 부착  
![m3](https://user-images.githubusercontent.com/87048674/131806072-fcca7d4c-792e-4812-92dd-d8ebf16a4463.png)

- Aggregate 부착  
![m4](https://user-images.githubusercontent.com/87048674/131806074-bdb9419a-7724-4191-bad1-b26527186994.png)

- View 추가 및 Bounded Context 묶기  
![m6](https://user-images.githubusercontent.com/87048674/131864679-ea58f42a-8f5b-4990-ab3b-77c47c30eefd.png)

- 완성 모형: Pub/Sub, Req/Res 추가  
![m7](https://user-images.githubusercontent.com/87048674/131864674-65e51fa9-a8d1-4f58-a4e8-7383eaa98b5e.png)


## 기능적/비기능적 요구사항 커버 여부 검증
- 기능적 요구사항  

1. 고객이 상품을 선택하여 주문한다.(O)  
2. 주문한 상품은 Cart에 담겨서 결제를 기다린다.(O)  
3. 고객이 주문에 대한 결제를 한다.(O)  
4. 결제가 승인되면 Cart의 상품이 주문 완료된다(O)   
5. 고객이 주문을 취소할 수 있다.(O)    
6. 주문이 취소되면 결제가 취소된다.(O)  
7. 상품 정보 및 주문/Cart 상태를 조회 할 수 있다.(O)    


- 비기능적 요구사항  
     · 장애격리 : 과도한 주문요청 Traffic 발생 시 Circuit breaker 를 통해 장애 확대를 회피한다.(O)  
     · Autoscale : 과도한 주문요청 Traffic 발생 시, 추가 자원할당을 통해 서비스 안정성을 확보한다.(O)  
     · 무정지배포 : 배포시 서비스 중단이 없도록 한다.(O)  
     · Self-healing : 서비스의 장애여부를 지속적으로 확인하고, 장애 서비스는 제외한다.(O)  


## Hexagonal Architecture Diagram
![Hexa](https://user-images.githubusercontent.com/87048674/131859802-cc43e8d3-529b-40f7-97b3-9a2c5ebf3e18.png)
```
Inbound adaptor와 Outbound adaptor를 구분함  
호출관계에서 PubSub 과 Req/Resp 를 구분함  
서브 도메인과 바운디드 컨텍스트를 분리함  
```


# 구현
분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다  
(각 서비스의 포트넘버는 8081 ~ 808n 이다)
```
   mvn spring-boot:run
```

## DDD 의 적용  
- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다.
```
package funshop;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Order_table")
public class Order {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String name;
    private Long cardNo;
    private String status;

    @PostPersist
    public void onPostPersist(){
        Ordered ordered = new Ordered();
        BeanUtils.copyProperties(this, ordered);
        ordered.publishAfterCommit();

        funshop.external.PaymentList paymentList = new funshop.external.PaymentList();
                
        System.out.println("this.id() : " + this.id);
        paymentList.setOrderId(this.id);
        paymentList.setStatus("Payment Complete");
        paymentList.setCardNo(this.cardNo);      
        
        
        OrderApplication.applicationContext.getBean(funshop.external.PaymentListService.class)
            .pay(paymentList);

    }
    @PostUpdate
    public void onPostUpdate(){
        OrderCanceled orderCanceled = new OrderCanceled();
        BeanUtils.copyProperties(this, orderCanceled);
        orderCanceled.publishAfterCommit();

    }
    @PrePersist
    public void onPrePersist(){
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public Long getCardNo() {
        return cardNo;
    }

    public void setCardNo(Long cardNo) {
        this.cardNo = cardNo;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다  
```
package funshop;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="orders", path="orders")
public interface OrderRepository extends PagingAndSortingRepository<Order, Long>{

}

```
- 적용 후 REST API 의 테스트
```
ie98dh@LAPTOP-7QPQK9AV:~/project/funshop$ http http://localhost:8088/orders id=1 name=David cardNo=12345678999 status=ordered
HTTP/1.1 201 Created
Content-Type: application/json;charset=UTF-8
Date: Fri, 03 Sep 2021 00:40:34 GMT
Location: http://localhost:8081/orders/1
transfer-encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/1"
        },
        "self": {
            "href": "http://localhost:8081/orders/1"
        }
    },
    "cardNo": 12345678999,
    "name": "David",
    "status": "ordered"
}

ie98dh@LAPTOP-7QPQK9AV:~/project/funshop$ http http://localhost:8088/orders/1
HTTP/1.1 200 OK
Content-Type: application/hal+json;charset=UTF-8
Date: Fri, 03 Sep 2021 00:40:41 GMT
transfer-encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/1"
        },
        "self": {
            "href": "http://localhost:8081/orders/1"
        }
    },
    "cardNo": 12345678999,
    "name": "David",
    "status": "ordered"
}
```

## CQRS  
CQRS 구현을 위해 고객의 주문/결제 상황을 확인할 수 있는 MyPage를 구성
```
ie98dh@LAPTOP-7QPQK9AV:~/project/funshop$ http http://localhost:8088/mypages/1
HTTP/1.1 200 OK
Content-Type: application/hal+json;charset=UTF-8
Date: Fri, 03 Sep 2021 00:57:09 GMT
transfer-encoding: chunked

{
    "_links": {
        "mypage": {
            "href": "http://localhost:8084/mypages/1"
        },
        "self": {
            "href": "http://localhost:8084/mypages/1"
        }
    },
    "cancellationId": null,
    "cartId": 1,
    "name": "David",
    "orderId": 1,
    "status": "In your cart"
}
```

## Req/Resp (동기식 호출)
주문(Order)->결제(Payment) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.

결제서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현
```
#PaymentListService.java

@FeignClient(name="payment", url="${api.payment.url}")
public interface PaymentListService {
    @RequestMapping(method= RequestMethod.POST, path="/paymentLists")
    public void pay(@RequestBody PaymentList paymentList);

}
```

주문을 받은 직후(@PostPersist) 결제를 요청하도록 처리
```
# Order.java (Entity)

    @PostPersist
    public void onPostPersist(){
        Ordered ordered = new Ordered();
        BeanUtils.copyProperties(this, ordered);
        ordered.publishAfterCommit();

        funshop.external.PaymentList paymentList = new funshop.external.PaymentList();
                
        System.out.println("this.id() : " + this.id);
        paymentList.setOrderId(this.id);
        paymentList.setStatus("Into your cart");
        paymentList.setCardNo(this.cardNo);      
        
        
        OrderApplication.applicationContext.getBean(funshop.external.PaymentListService.class)
            .pay(paymentList);

    }
```

동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 결제 시스템이 장애가 나면 주문도 못받는다는 것을 확인:
```
# 결제 (payment) 서비스를 잠시 내려놓음 (ctrl+c)

# 주문요청
ie98dh@LAPTOP-7QPQK9AV:~/project/funshop$ http http://localhost:8088/orders id=2 name=JiSung.Park cardNo=12345678999 status=ordered
HTTP/1.1 500 Internal Server Error
Content-Type: application/json;charset=UTF-8
Date: Fri, 03 Sep 2021 01:10:48 GMT
transfer-encoding: chunked

{
    "error": "Internal Server Error",
    "message": "Could not commit JPA transaction; nested exception is javax.persistence.RollbackException: Error while committing the transaction",
    "path": "/orders",
    "status": 500,
    "timestamp": "2021-09-03T01:10:48.109+0000"
}

# 결제 (payment) 재기동
mvn spring-boot:run

#주문처리
ie98dh@LAPTOP-7QPQK9AV:~/project/funshop$ http http://localhost:8088/orders id=2 name=JiSung.Park cardNo=12345678999 status=ordered
HTTP/1.1 201 Created
Content-Type: application/json;charset=UTF-8
Date: Fri, 03 Sep 2021 01:12:33 GMT
Location: http://localhost:8081/orders/3
transfer-encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/3"
        },
        "self": {
            "href": "http://localhost:8081/orders/3"
        }
    },
    "cardNo": 12345678999,
    "name": "JiSung.Park",
    "status": "ordered"
}
```

## Pub/Sub (비동기식 호출)
결제가 이루어진 후에 Cart 서비스로 이를 알려주는 행위는 동기식이 아니라 비 동기식으로 처리하여 Cart 시스템의 처리를 위하여 결제주문이 블로킹 되지 않아도록 처리한다.

- 이를 위하여 결제이력에 기록을 남긴 후에 곧바로 결제승인이 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
```
package funshop;

 ...
    @PostPersist
    public void onPostPersist(){
        PaymentApproved paymentApproved = new PaymentApproved();
        paymentApproved.setStatus("Pay OK");
        BeanUtils.copyProperties(this, paymentApproved);
        paymentApproved.publishAfterCommit();
```

- Cart 서비스에서는 결제승인 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:
```
package funshop;

...
@Service
public class PolicyHandler{
    @Autowired CartRepository cartRepository;
    @Autowired CancellationRepository cancellationRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentApproved_AcceptCart(@Payload PaymentApproved paymentApproved){

        if(!paymentApproved.validate()) return;

        System.out.println("\n\n##### listener AcceptCart : " + paymentApproved.toJson() + "\n\n");

        Cart cart = new Cart();
        cart.setStatus("Payment Complete");
        cart.setOrderId(paymentApproved.getOrderId());
        cart.setId(paymentApproved.getOrderId());
        cartRepository.save(cart);
    }    
```

- Cart 서비스는 주문/결제와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, Cart 서비스가 유지보수로 인해 잠시 내려간 상태라도 주문을 받는데 문제가 없다:
```
#  Cart 서비스 (cart) 를 잠시 내려놓음 (ctrl+c)

#주문처리
ie98dh@LAPTOP-7QPQK9AV:~/project/funshop$ http http://localhost:8088/orders id=1 name=David cardNo=12345678999 status=ordered
HTTP/1.1 201 Created
Content-Type: application/json;charset=UTF-8
Date: Fri, 03 Sep 2021 01:46:47 GMT
Location: http://localhost:8081/orders/1
transfer-encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/1"
        },
        "self": {
            "href": "http://localhost:8081/orders/1"
        }
    },
    "cardNo": 12345678999,
    "name": "David",
    "status": "ordered"
}

#주문상태 확인
ie98dh@LAPTOP-7QPQK9AV:~/project/funshop$ http http://localhost:8088/orders/1
HTTP/1.1 200 OK
Content-Type: application/hal+json;charset=UTF-8
Date: Fri, 03 Sep 2021 01:47:22 GMT
transfer-encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/1"
        },
        "self": {
            "href": "http://localhost:8081/orders/1"
        }
    },
    "cardNo": 12345678999,
    "name": "David",
    "status": "ordered"
}
	    
#Cart 서비스 기동
mvn spring-boot:run

#Cart 상태 확인
ie98dh@LAPTOP-7QPQK9AV:~/project/funshop$ http http://localhost:8088/carts/1
HTTP/1.1 200 OK
Content-Type: application/hal+json;charset=UTF-8
Date: Fri, 03 Sep 2021 01:50:59 GMT
transfer-encoding: chunked

{
    "_links": {
        "cart": {
            "href": "http://localhost:8083/carts/1"
        },
        "self": {
            "href": "http://localhost:8083/carts/1"
        }
    },
    "orderId": 1,
    "status": "Payment Complete"
}
```

## Correlation
PolicyHandler에서 처리 시 어떤 건에 대한 처리인지를 구별하기 위한 Correlation-key 구현을 이벤트 클래스 안의 변수로 전달받아 서비스간 연관된 처리를 구현하였습니다.
주문을 하면 동시에 연관된 결제, Cart 서비스의 상태가 변경이 되고, 주문 취소를 수행하면 다시 연관된 서비스의 데이터가 변경되는 것을 확인할 수 있습니다.
```
# 주문
ie98dh@LAPTOP-7QPQK9AV:~/project/funshop$ http http://localhost:8088/orders id=2 name=TestGuy cardNo=000011119999 status=ordered
HTTP/1.1 201 Created
Content-Type: application/json;charset=UTF-8
Date: Fri, 03 Sep 2021 01:54:14 GMT
Location: http://localhost:8081/orders/2
transfer-encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/2"
        },
        "self": {
            "href": "http://localhost:8081/orders/2"
        }
    },
    "cardNo": 11119999,
    "name": "TestGuy",
    "status": "ordered"
}

# 주문 확인
ie98dh@LAPTOP-7QPQK9AV:~/project/funshop$ http http://localhost:8088/carts/2
HTTP/1.1 200 OK
Content-Type: application/hal+json;charset=UTF-8
Date: Fri, 03 Sep 2021 01:55:03 GMT
transfer-encoding: chunked

{
    "_links": {
        "cart": {
            "href": "http://localhost:8083/carts/2"
        },
        "self": {
            "href": "http://localhost:8083/carts/2"
        }
    },
    "orderId": 2,
    "status": "Payment Complete"
}

# 주문 취소
ie98dh@LAPTOP-7QPQK9AV:~/project/funshop$ http http://localhost:8088/orders id=2 name=TestGuy cardNo=000011119999 status=canceled
HTTP/1.1 201 Created
Content-Type: application/json;charset=UTF-8
Date: Fri, 03 Sep 2021 01:55:48 GMT
Location: http://localhost:8081/orders/2
transfer-encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/2"
        },
        "self": {
            "href": "http://localhost:8081/orders/2"
        }
    },
    "cardNo": 11119999,
    "name": "TestGuy",
    "status": "canceled"
}

# 주문 취소 확인
ie98dh@LAPTOP-7QPQK9AV:~/project/funshop$ http http://localhost:8088/mypages/2
HTTP/1.1 200 OK
Content-Type: application/hal+json;charset=UTF-8
Date: Fri, 03 Sep 2021 01:57:02 GMT
transfer-encoding: chunked

{
    "_links": {
        "mypage": {
            "href": "http://localhost:8084/mypages/2"
        },
        "self": {
            "href": "http://localhost:8084/mypages/2"
        }
    },
    "cancellationId": null,
    "cartId": 2,
    "name": "TestGuy",
    "orderId": 2,
    "status": "Cart Canceled"
}
```

## Gateway
gateway 스프링부트 App을 추가 후 application.yaml내에 각 마이크로 서비스의 routes 를 추가하고 gateway 서버의 포트를 8080 으로 설정함
```
spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: order
          uri: http://order:8080
          predicates:
            - Path=/orders/** 
        - id: payment
          uri: http://payment:8080
          predicates:
            - Path=/paymentLists/** 
        - id: cart
          uri: http://cart:8080
          predicates:
            - Path=/carts/**/cancellations/** 
        - id: customer
          uri: http://customer:8080
          predicates:
            - Path= /mypages/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080

```
![gateway1](https://user-images.githubusercontent.com/87048674/131770319-e8ae49cf-06c0-47ff-9661-267f4bf62884.png)
![gateway2](https://user-images.githubusercontent.com/87048674/131770321-a1d71116-e402-46ba-8f9f-00650ebb4843.png)


## Polyglot  

Polyglot Persistence를 위해 h2datase를 hsqldb로 변경
```

		<dependency>
			<groupId>org.hsqldb</groupId>
			<artifactId>hsqldb</artifactId>
			<scope>runtime</scope>
		</dependency>
<!--
		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>runtime</scope>
		</dependency>
-->

# 변경/재기동 후 예약 주문
http localhost:8081/orders name="lee" cardNo=1 status="order started"

HTTP/1.1 201 
Content-Type: application/json;charset=UTF-8
Date: Wed, 18 Aug 2021 09:41:30 GMT
Location: http://localhost:8081/orders/1
Transfer-Encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/1"
        },
        "self": {
            "href": "http://localhost:8081/orders/1"
        }
    },
    "cardNo": 1,
    "name": "lee",
    "status": "order started"
}


# 저장이 잘 되었는지 조회
http localhost:8081/orders/1

HTTP/1.1 200
Content-Type: application/hal+json;charset=UTF-8    
Date: Wed, 18 Aug 2021 09:42:25 GMT
Transfer-Encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/1"
        },
        "self": {
            "href": "http://localhost:8081/orders/1"
        }
    },
    "cardNo": 1,
    "name": "lee",
    "status": "order started"
}
```


# 운영  

## Deploy

각자의 source 위치에서 mvn package 적용 > Docker build > ECR 생성 > Docker push 순으로 진행 후, 배포하여 서비스를 생성한다.
```
(1) order
mvn package
docker build -t 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-order:v1 .
aws ecr create-repository --repository-name user02-order --region ap-northeast-2
docker push 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-order:v1

(2) cart
mvn package
docker build -t 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-cart:v1 .
aws ecr create-repository --repository-name user02-cart --region ap-northeast-2
docker push 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-cart:v1

(3) payment
mvn package
docker build -t 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-payment:v1 .
aws ecr create-repository --repository-name user02-payment --region ap-northeast-2
docker push 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-payment:v1

(4) customer
mvn package
docker build -t 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-customer:v1 .
aws ecr create-repository --repository-name user02-customer --region ap-northeast-2
docker push 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-customer:v1

(5) gateway
mvn package
docker build -t 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-gateway:v1 .
aws ecr create-repository --repository-name user02-gateway --region ap-northeast-2
docker push 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-gateway:v1

(6) 배포
kubectl create deploy order --image=052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-order:v1 -n funshop
kubectl create deploy cart --image=052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-cart:v1 -n funshop
kubectl create deploy payment --image=052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-payment:v1 -n funshop
kubectl create deploy customer --image=052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-customer:v1 -n funshop
kubectl create deploy gateway --image=052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-gateway:v1 -n funshop

kubectl expose deploy order --type=ClusterIP --port=8080 -n funshop
kubectl expose deploy cart --type=ClusterIP --port=8080 -n funshop
kubectl expose deploy payment --type=ClusterIP --port=8080 -n funshop
kubectl expose deploy customer --type=ClusterIP --port=8080 -n funshop
kubectl expose deploy gateway --type=LoadBalancer --port=8080 -n funshop
```
![deploy](https://user-images.githubusercontent.com/87048674/131770323-3e28b703-3005-49f1-913e-19045f1a79c7.png)


## Circuit Breaker  
Circuit Breaker 프레임워크: istio 사용하여 구현.  
주문(order) → 결제(payment) 시의 연결이 Request/Response 로 연동하여 구현이 되어있고, 주문 요청이 과도할 경우 CB 를 통하여 장애격리.

- DestinationRule 를 생성하여 circuit break 가 발생할 수 있도록 설정 최소 connection pool 설정  
```
# destination-rule.yaml

apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: order
spec:
  host: order
  trafficPolicy:
    connectionPool:
      http:
        http1MaxPendingRequests: 1
        maxRequestsPerConnection: 1
```        

- istio-injection 활성화  
![CB1](https://user-images.githubusercontent.com/87048674/131787482-2c549667-0ec1-4c79-b081-3bf80e71e159.png)


- 임계치 이하로 부하 발생시 100% 정상처리 확인  
```
siege -c1 -t10S -v --content-type "application/json" 'http://order:8080/orders POST {"name": "TEST1", "status": "put myCart", "cardNo":"123456789999"}'
```
![CB2](https://user-images.githubusercontent.com/87048674/131787485-fa948f75-6b23-4b60-a6dc-9ddad73864a0.png)


- 임계치 초과하여 부하 발생시, 772건중 179건 실패하여, Availability 80.13% 확인  
```
siege -c10 -t10S -v --content-type "application/json" 'http://order:8080/orders POST {"name": "TEST1", "status": "put myCart", "cardNo":"123456789999"}'
```
![CB3](https://user-images.githubusercontent.com/87048674/131787487-c97832ed-75de-405a-af53-3ca61b2b335a.png)

주문 요청이 과도할 경우 Circuit Breaker에 의하여 적절히 회로가 열리고 닫히면서, Payment 등으로 장애가 확대되지 않도록 자원을 보호하고 있음을 확인.



## Autoscale (HPA)  
주문요청이 증가하여 Cart에 주문상품이 지속적으로 쌓일 경우, 자원을 동적으로 추가 할당 할 수 있도록 Autoscale을 설정한다.
- autoscaleout_cart.yaml에 resources 설정 추가
```
..(생략)..
    spec:
      containers:
        - name: cart
          image: 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-cart:v1          
          ports:
            - containerPort: 8080
          resources:
            requests:
              cpu: "200m"
            limits:
              cpu: "500m"  

..(생략)..
```
- cart 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. (CPU 사용량이 50프로를 넘어서면 replica 를 10개까지 확장)
```
kubectl apply -f kubernetes/autoscaleout_cart.yaml
kubectl autoscale deployment cart --cpu-percent=50 --min=1 --max=10
```

- Order 서비스를 통해서 Cart 에 부하를 주어, 스케일 아웃 정상작동을 확인.
![autoscale1](https://user-images.githubusercontent.com/87048674/131791502-15bbef50-c400-4e18-8dec-ca73b61f2fb9.png)



## Zero-Downtime deploy (readiness probe)

무정지 배포 TEST 비교를 위하여 readiness probe 설정이 없는 버전(deployment_readiness_v2.yml)의 .yml과 readiness probe 설정이 있는 버전(deployment_readiness_v3.yml)의 .yml을 준비한다.

- deployment_readiness_v2.yml : customer 배포(readiness 설정없음, image:v2) 
```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: customer
  labels:
    app: customer
spec:
  replicas: 1
  selector:
    matchLabels:
      app: customer
  template:
    metadata:
      labels:
        app: customer
    spec:
      containers:
        - name: customer
          image: 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-customer:v2
          ports:
            - containerPort: 8080

---

apiVersion: v1
kind: Service
metadata:
  name: customer
  labels:
    app: customer
spec:
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: customer
```

- deployment_readiness_v3.yml : customer 배포(readiness 설정있음, image:v2) 
```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: customer
  labels:
    app: customer
spec:
  replicas: 1
  selector:
    matchLabels:
      app: customer
  template:
    metadata:
      labels:
        app: customer
    spec:
      containers:
        - name: customer
          image: 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-customer:v2
          ports:
            - containerPort: 8080
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10

---

apiVersion: v1
kind: Service
metadata:
  name: customer
  labels:
    app: customer
spec:
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: customer
```

- Readiness probe 설정이 없는 버전(deployment_readiness_v2.yml) 배포시 Error 발생 확인
![readiness1](https://user-images.githubusercontent.com/87048674/131773313-13c01eaa-81fa-4fd0-bebe-f109599fd9e6.png)

- Readiness probe 설정이 있는 버전(deployment_readiness_v3.yml) 배포시 무중단 확인
![readiness2](https://user-images.githubusercontent.com/87048674/131773317-4ec97bee-0afd-400f-92dc-09068c537215.png)
배포기간 동안 Availability 가 변화없기 때문에 무정지 재배포가 성공한 것으로 확인.


## Self-healing(Liveness probe)  

Liveness TEST를 위한 .yml 준비하여 콘테이너 실행 후 /tmp/healthy 파일을 만들고, 90초 후 삭제하여 livenessProbe에 'cat /tmp/healthy'으로 검증하도록 함
```
deployment_liveness.yml : customer 배포
..(생략)..

      containers:
        - name: customer
          image: 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-customer:v1
          args:
          - /bin/sh
          - -c
          - touch /tmp/healthy; sleep 90; rm -rf /tmp/healthy; sleep 600
          ports:
            - containerPort: 8080
          livenessProbe:
            exec:
              command:
              - cat
              - /tmp/healthy
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5

..(생략)..
```
컨테이너 실행 후 90초 동은 정상이나 이후 /tmp/healthy 파일이 삭제되어 livenessProbe에서 실패를 리턴하게 됨
- watch kubectl get pod 로 모니터링
![liveness1](https://user-images.githubusercontent.com/87048674/131775735-2d31b802-29bb-4ce5-b2cc-8f4b4489ea8f.png)



## Persistence Volume
신규로 생성한 EFS Storage에 Pod가 접근할 수 있도록 권한 및 서비스 설정.  

1. EFS 생성: ClusterSharedNodeSecurityGroup 선택
![PVC1](https://user-images.githubusercontent.com/87048674/131782813-9ce75f3f-fd30-4e58-833d-4f48746992d0.png)

2. EFS계정 생성 및 Role 바인딩
```
- ServerAccount 생성
kubectl apply -f efs-sa.yml
kubectl get ServiceAccount efs-provisioner -n yanolza


-SA(efs-provisioner)에 권한(rbac) 설정
kubectl apply -f efs-rbac.yaml

# efs-provisioner-deploy.yml 파일 수정
value: fs-a638b6c6
value: ap-northeast-2
server: fs-a638b6c6.efs.ap-northeast-2.amazonaws.com
```

3. EFS provisioner 설치
```
kubectl apply -f efs-provisioner-deploy.yml
kubectl get Deployment efs-provisioner -n funshop
```

4. EFS storageclass 생성
```
kubectl apply -f efs-storageclass.yaml
kubectl get sc aws-efs -n funshop
```

5. PVC 생성
```
kubectl apply -f volume-pvc.yml
kubectl get pvc -n funshop
```

6. Create Pod with PersistentVolumeClaim
```
kubectl apply -f pod-with-pvc.yaml
```

- replicas 2 적용하여 Pod 생성  
![PVC4](https://user-images.githubusercontent.com/87048674/131805276-4613be77-1100-4c62-ba45-2db8fbe5a444.png)  

- 한쪽 Pod에서 생성한 파일을 다른쪽 Pod에서 확인     
![PVC3](https://user-images.githubusercontent.com/87048674/131805271-7c184b5d-03fc-4c4c-9587-24969d97de5a.png)

## ConfigMap

- ConfigMap 생성
```
kubectl create configmap funshop-mod --from-literal=mode=debug
```

- yaml 파일 적용
```
# autoscaleout_cart.yaml 에 설정 추가
...(생략)...

          env:
            - name: debug
              valueFrom:
                configMapKeyRef:
                  name: funshop-mod
                  key: mode

...(생략)...
```
![configmap1](https://user-images.githubusercontent.com/87048674/131821360-17fc8b47-0686-4cad-8c04-15270cef7eb8.png)

