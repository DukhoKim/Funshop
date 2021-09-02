![로고](https://user-images.githubusercontent.com/87048674/131634949-6eaffd62-c54d-46ec-8d9f-2da9a2796eef.png)

# 쇼핑몰 (Funshop)

본 예제는 MSA/DDD/Event Storming/EDA 를 포괄하는 분석/설계/구현/운영 전단계를 커버하도록 구성한 예제입니다. 이는 클라우드 네이티브 애플리케이션의 개발에 요구되는 체크포인트들을 통과하기 위한 예시 답안을 포함합니다.

- 체크포인트 : https://workflowy.com/s/assessment-check-po/T5YrzcMewfo4J6LW

# Table of Contents

- 서비스 시나리오  
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
     · Deploy  
     · Circuit Breaker  
     · Autoscale (HPA)  
     · Zero-Downtime deploy (Readiness probe)  
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


- 비기능적 요구사항  
     · 장애격리 : 과도한 주문요청 Traffic 발생 시 Circuit breaker 를 통해 장애 확대를 회피한다.  
     · Autoscale : 과도한 주문요청 Traffic 발생 시, 추가 자원할당을 통해 서비스 안정성을 확보한다.  
     · 무정지배포 : 배포시 서비스 중단이 없도록 한다.  
     · Self-healing : 서비스의 장애여부를 지속적으로 확인하고, 장애 서비스는 제외한다.  



# 분석/설계  

## Event Storming

- MSAEZ에서 Event Storming 수행  
- Event 도출  
(이미지)  

- Actor, Command 부착  
(이미지)  

- Policy 부착  
(이미지)  

- Aggregate 부착  
(이미지)  

- View 추가 및 Bounded Context 묶기  
(이미지)  

- 완성 모형: Pub/Sub, Req/Res 추가  
(이미지)   


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
(이미지)
```
Inbound adaptor와 Outbound adaptor를 구분함  
호출관계에서 PubSub 과 Req/Resp 를 구분함  
서브 도메인과 바운디드 컨텍스트를 분리함  
```


# 구현

## DDD 의 적용  

## Saga  

## CQRS  

## Correlation  

## Req/Resp  

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

- df-k로 EFS mount 
![PVC2](https://user-images.githubusercontent.com/87048674/131782814-193e8b28-2f52-42bc-a0bd-a793961e2d2b.png)
