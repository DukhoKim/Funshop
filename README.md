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
     · Gateway(완료)  
     · Polyglot  

- 운영  
     · Deploy(완료)  
     · Circuit Breaker  
     · Autoscale (HPA)  
     · Zero-Downtime deploy (Readiness probe)(완료)   
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
![gateway1](https://user-images.githubusercontent.com/87048674/131770319-e8ae49cf-06c0-47ff-9661-267f4bf62884.png)
![gateway2](https://user-images.githubusercontent.com/87048674/131770321-a1d71116-e402-46ba-8f9f-00650ebb4843.png)



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
컨테이너 실행 후 90초 동인은 정상이나 이후 /tmp/healthy 파일이 삭제되어 livenessProbe에서 실패를 리턴하게 됨
- watch kubectl get pod 로 모니터링

