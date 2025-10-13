### 架构图

![](.\server.png)

```
account：账号服务器，负责账号的注册和登录。client 通过http进行注册或者登录成功后，返回token和游戏服务器的地址
Nginx:负载均衡，将流量进行分发到 gate 服务
gate：网关服务器，负责与 client 保持连接，并且根据不同的业务场景将 client 的请求数据转发到其他服务
hall：大厅服务器，负责大厅业务场景逻辑
game：子游戏
redis：缓存一些热数据，比如在线玩家信息等等
mongodb：存储不常用数据，比如很久没有上线的玩家信息等
es: 游戏服务生产日志到kafka，然后由logstash或者由后台管理服务器消费
gm：gm服务器，负责接受后台请求，处理与游戏相关的业务逻辑
```



### 项目工程模块

```
common: 基础设施，比如创建nettyserver，集群节点发现与分配，网络连接，数据库连接等等
core: hall和game共有的模块，比如player对象
room: 房间模块
activity: 活动模块
sampledata: 配置表模块

gate：网关服务器
account：账号服务器
hall：大厅
gm：gm服务器
recharge: 充值服务器(接收充值回调)

slots: slots类型游戏集合
table: 桌游类型游戏集合
poker: 扑克类型游戏集合
```



### 环境

```
java：JDK21
zookeeper: 3.8.4
redis: 7.4.2 
mongodb: 8.0.5
kafka：4.0.0
```



### 配置

#### application

首先看`application.yaml` 中使用的哪个配置文件，比如配置`dev`表示正在使用`application-dev.yaml`

```yaml
spring.profiles.active:
  dev
```



`application-dev.yaml` 配置文件示例，不可直接拷贝，在这里只做解释说明

```yaml
logging:
 config: config/logback.xml  #日志配置文件
 level.*: debug
  
server:
 port: 9002  #http端口
 
gate:
  wsAddress:
   port: 9001  #gate服务器对外提供的端口
   host: 0.0.0.0 

zookeeper:
 connects: 192.168.3.31:2181  #zookeeper地址
 marsRoot: jjg  #节点根目录

cluster:
 type: HALL   #该节点类型
 name: HALL_SHIYI  #该节点名称，在整个集群中不可重复, 格式: ${type}_xxxx
 weight: 2
 tcpAddress:
  port: 13006
  host: 192.168.3.18   #本节点ip(内网)
 masterSelect: true  #是否参与主节点竞选


spring:
 main:
  web-application-type: none  
 data:
  redis:
   host: 192.168.3.31
   port: 6379
   database: 11
  mongodb:
   uri: mongodb://admin:jjg123456@192.168.3.31:27017/vegasnight_game_dev?authSource=admin
 kafka:
   bootstrap-servers: 192.168.3.31:9092
   producer:
     key-serializer: org.apache.kafka.common.serialization.StringSerializer
     value-serializer: org.apache.kafka.common.serialization.StringSerializer
     acks: 1  #leader 写入成功即返回确认
     enable-idempotence: true  #启用幂等性，确保消息不会重复发送（即使重试）
     retries: 3  #发送失败时的重试次数
     batch-size: 16384  #批次大小（16KB），Producer 会累积消息到这个大小后批量发送
     compression-type: gzip
     max-request-size: 1048576
     linger-ms: 50  #批次等待时间（50ms），即使未达到 batch-size也会在等待这个时间后发送
     request.timeout.ms: 30000    # 单次请求 30s 超时
     retry.backoff.ms: 500        # 重试间隔 500ms
     delivery.timeout.ms: 120000  # 总超时 2 分钟
     max.in.flight.requests.per.connection: 1  #每个连接的最大未确认请求数,设为 1 可保证消息顺序（即使重试）
```

启动新节点修改内容如下：

```yaml
cluster:
 name: xxxx  #修改
tcpAddress:
  port: 13006  #端口在本机不冲突就行
  host: 192.168.3.18   #ip要修改成本节点ip(内网)
  
#如果是gate节点
gate:
  wsAddress:
   port: 9001  #保证端口在本机不冲突就行
   
#如果有http端口也要修改
server:
 port: 9002  #保证端口在本机不冲突就行
```



#### excel

在 resources/sample 目录下的 excel 文件是由策划管理的游戏配置表

```
1.该目录下的每个excel文件与同名java文件一一对应
2.系统启动或者文件变化时，会自动加载
```



#### nodeConfig

`nodeConfig.json`配置说明

```json
{
  "weight": 1,  //节点权重，如果设置为0，在没有连接的情况下会自动结束进程(gate节点除外)
  "whiteIpList":["192.168.3.18"] //白名单ip
}
```



#### 其他json配置

`*.json` 配置说明

```
1.该json文件在 config 目录下(非 nodeConfig.json 文件)
2.该配置会在系统启动或者文件变化时加载到spring管理的同名bean

例：
在 hall 模块下的 config 目录下有 hallConfig.json 配置
该配置与代码中的 HallConfig.java 对应
```



### 协议导出

 导出工具类：com.jjg.game.common.proto.ToOneFile2

