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

## 기능적 요구사항  

호스트가 임대할 숙소를 등록/수정/삭제한다.  
고객이 숙소를 선택하여 예약한다.  
예약과 동시에 결제가 진행된다.  
예약이 되면 예약 내역(Message)이 전달된다.  
고객이 예약을 취소할 수 있다.  
예약 사항이 취소될 경우 취소 내역(Message)이 전달된다.  
숙소에 후기(review)를 남길 수 있다.  
전체적인 숙소에 대한 정보 및 예약 상태 등을 한 화면에서 확인 할 수 있다.(viewpage)  


비기능적 요구사항

트랜잭션  
결제가 되지 않은 예약 건은 성립되지 않아야 한다. (Sync 호출)  

장애격리  
숙소 등록 및 메시지 전송 기능이 수행되지 않더라도 예약은 365일 24시간 받을 수 있어야 한다 Async (event-driven), Eventual Consistency
예약 시스템이 과중되면 사용자를 잠시동안 받지 않고 잠시 후에 하도록 유도한다 Circuit breaker, fallback  

성능  
모든 방에 대한 정보 및 예약 상태 등을 한번에 확인할 수 있어야 한다 (CQRS)  
예약의 상태가 바뀔 때마다 메시지로 알림을 줄 수 있어야 한다 (Event driven)


