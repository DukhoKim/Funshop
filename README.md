![로고](https://user-images.githubusercontent.com/87048674/131634949-6eaffd62-c54d-46ec-8d9f-2da9a2796eef.png)

# 쇼핑몰 (Funshop)

본 예제는 MSA/DDD/Event Storming/EDA 를 포괄하는 분석/설계/구현/운영 전단계를 커버하도록 구성한 예제입니다. 이는 클라우드 네이티브 애플리케이션의 개발에 요구되는 체크포인트들을 통과하기 위한 예시 답안을 포함합니다.

- 체크포인트 : https://workflowy.com/s/assessment-check-po/T5YrzcMewfo4J6LW

# Table of Contents

- 서비스 시나리오  
- 체크포인트  
- 분석/설계  

- 구현  
     · DDD 의 적용  
     · Saga  
     · CQRS  
     · Correlation  
     · Req/Resp  
     · Gateway  
     · Polyglot  

- 운영  
     · Deply  
     · Circuit Breaker  
     · Autoscale (HPA)  
     · Zero-Downtime deploy (readiness probe)  
     · Persistence Volume  
     · Self-healing(Liveness probe)  
     
     


# 서비스 시나리오

- 기능적 요구사항  

1. 고객이 상품을 선택하여 주문한다.  
2. 주문한 상품은 Cart에 담겨서 결제를 기다린다. 
3. 고객이 주문에 대한 결제를 한다.
4. 결제가 승인되면 Cart의 상품이 주문 완료된다 
5. 고객이 주문을 취소할 수 있다.  
6. 주문이 취소되면 결제가 취소된다.
7. 상품 정보 및 주문/Cart 상태를 조회 할 수 있다.(viewpage)  


비기능적 요구사항

트랜잭션  
결제가 되지 않은 예약 건은 성립되지 않아야 한다. (Sync 호출)  

장애격리  
숙소 등록 및 메시지 전송 기능이 수행되지 않더라도 예약은 365일 24시간 받을 수 있어야 한다 Async (event-driven), Eventual Consistency
예약 시스템이 과중되면 사용자를 잠시동안 받지 않고 잠시 후에 하도록 유도한다 Circuit breaker, fallback  

성능  
모든 방에 대한 정보 및 예약 상태 등을 한번에 확인할 수 있어야 한다 (CQRS)  
예약의 상태가 바뀔 때마다 메시지로 알림을 줄 수 있어야 한다 (Event driven)


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
(gateway1.PNG)
(gateway2.PNG)
