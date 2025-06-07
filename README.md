### 架构图

![](.\server.png)

```
account：账号服务器，负责账号的注册和登录。client 通过http进行注册或者登录成功后，返回token和游戏服务器的地址
Nginx:负载均衡，将流量进行分发到 gate 服务
gate：网关服务器，负责与 client 保持连接，并且根据不同的业务场景将 client 的请求数据转发到其他服务
hall：大厅服务器，负责大厅业务场景逻辑
game：子游戏
logserver：日志服务器，接收在游戏过程中产生的行为数据并存储到数据库
redis：缓存一些热数据，比如在线玩家信息等等
mongodb：存储不常用数据，比如很久没有上线的玩家信息等
es: 采集日志后发送到es
gm：gm服务器，负责接受后台请求，处理与游戏相关的业务逻辑
```



### 项目工程模块

```
common: 基础设施，比如创建nettyserver，集群节点发现与分配，配置等等
core: 游戏共有的模块，比如player对象
logdata: 由logserver提供的dubbo接口

gate：网关服务器
account：账号服务器
hall：大厅
gm：gm服务器
```



### 环境

```
java：JDK21
zookeeper: 3.8.4
redis: 7.4.2 
mongodb: 8.0.5
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

dubbo:
 application:
  name: ${cluster.name}  #该服务在dubbo中的节点名称，不可重复
  qos-enable: false
 registry:
  address: zookeeper://${zookeeper.connects}  #zookeeper地址
  parameters:
   listener:
    manage: false
 scan:
  base-packages: com.vegasnight.game
 shutdown:
  graceful: true  #设置dubbo延迟关机
  timeout: 3000 

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



#### nodeConfig

`nodeConfig.json`配置说明

```
{
  "weight": 1,  //节点权重，如果设置为0，在没有连接的情况下会自动结束进程(gate节点除外)
  "whiteIpList":["192.168.3.18"] //白名单ip
}
```







