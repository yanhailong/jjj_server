可以，下面这版更适合直接放进运维手册。

# Kafka 服务器 IP 变更后故障处理记录

## 1. 事件说明

公司网络调整后，Kafka 服务器 IP 发生变化。
客户端已将 Kafka 连接地址修改为新 IP，但消费者仍无法正常连接和消费。

## 2. 故障现象

### 2.1 客户端连接异常

消费者启动后出现 Broker 连接断开现象，无法正常获取元数据。

### 2.2 端口连通性异常

Windows 客户端测试发现：

* Redis 端口可正常连接
* Kafka `9092` 端口无法连接

### 2.3 Kafka 启动异常

Kafka 容器日志出现目录写入失败，报错为：

```text id="a1"
AccessDeniedException: /tmp/kafka-logs/bootstrap.checkpoint.tmp
```

### 2.4 Topic 元数据异常

Kafka 恢复后，消费者继续报错：

```text id="a2"
UNKNOWN_TOPIC_OR_PARTITION
```

对应业务 Topic 为 `bind`。

---

## 3. 原因分析

本次问题由以下两项原因叠加导致。

### 3.1 Docker 网络与新办公网段冲突

原 Kafka Docker 网络为：

```text id="a3"
172.18.0.0/16
```

公司修网后：

* Kafka 宿主机 IP：`172.18.11.38`
* 客户端 IP：`172.18.11.23`

上述地址均落入 `172.18.0.0/16` 网段内，导致 Docker 容器网络与办公网络发生重叠，造成 Kafka 外部访问异常。

### 3.2 Kafka 挂载目录权限不足

Kafka 容器将宿主机目录挂载到：

```text id="a4"
/data/docker/volum/kafka01/data -> /tmp/kafka-logs
```

容器内 Kafka 进程用户为 `1000:1000`，宿主机目录权限及 SELinux 上下文不满足写入要求，导致 Kafka 启动时无法写入元数据文件。

### 3.3 SELinux 处于 Enforcing 模式

服务器启用了 SELinux，进一步限制了容器对宿主机挂载目录的访问。

---

## 4. 处理过程

### 4.1 检查 Docker 网络配置

执行以下命令确认 Kafka 网络信息：

```bash id="a5"
docker network inspect kafka-tier
```

确认 `kafka-tier` 网段为：

```text id="a6"
172.18.0.0/16
```

判定与公司新网段冲突。

### 4.2 停止并删除原 Kafka 容器

```bash id="a7"
docker stop kafka01
docker rm kafka01
```

### 4.3 删除旧 Docker 网络

```bash id="a8"
docker network rm kafka-tier
```

### 4.4 重建不冲突的新网络

重新创建独立网段，例如：

```bash id="a9"
docker network create --driver bridge --subnet 10.66.0.0/24 kafka-tier
```

### 4.5 修复 Kafka 数据目录权限

```bash id="a10"
mkdir -p /data/docker/volum/kafka01/data
chown -R 1000:1000 /data/docker/volum/kafka01/data
chmod -R 775 /data/docker/volum/kafka01/data
```

### 4.6 修复 SELinux 上下文

```bash id="a11"
chcon -Rt container_file_t /data/docker/volum/kafka01/data
```

### 4.7 重新启动 Kafka 容器

启动命令调整为：

```bash id="a12"
docker run -d --name kafka01 \
  --network kafka-tier \
  -p 9092:9092 -p 9093:9093 \
  -v /data/docker/volum/kafka01/data:/tmp/kafka-logs:Z \
  -e KAFKA_BROKER_ID=0 \
  -e KAFKA_PROCESS_ROLES=controller,broker \
  -e KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://172.18.11.38:9092 \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=0@172.18.11.38:9093 \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_NODE_ID=0 \
  apache/kafka:latest
```

说明：

* Docker 网络改为新网段，避免与办公网冲突
* 挂载目录增加 `:Z`，适配 SELinux
* `KAFKA_ADVERTISED_LISTENERS` 使用宿主机新 IP

---

## 5. 验证步骤

### 5.1 检查 Kafka 容器 IP

```bash id="a13"
docker inspect kafka01 --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}'
```

确认容器 IP 已变更为新子网地址，如 `10.66.0.x`，不再使用 `172.18.x.x`。

### 5.2 检查 Kafka 日志

```bash id="a14"
docker logs --tail 100 kafka01
```

确认不再出现以下报错：

* `AccessDeniedException`
* `bootstrap.checkpoint.tmp`
* 其他启动失败异常

### 5.3 检查宿主机端口监听

```bash id="a15"
ss -lntp | grep 9092
```

确认宿主机 `9092` 端口已正常监听。

### 5.4 Windows 客户端验证

```powershell id="a16"
Test-NetConnection 172.18.11.38 -Port 9092
```

确认结果为：

```text id="a17"
TcpTestSucceeded : True
```

---

## 6. Topic 异常处理

Kafka 网络与权限问题修复后，消费者仍报 `bind=UNKNOWN_TOPIC_OR_PARTITION`。
排查确认业务 Topic `bind` 不存在，需重新创建。

### 6.1 查看 Topic 列表

```bash id="a18"
docker exec -it kafka01 /opt/kafka/bin/kafka-topics.sh --bootstrap-server 172.18.11.38:9092 --list
```

### 6.2 查看指定 Topic

```bash id="a19"
docker exec -it kafka01 /opt/kafka/bin/kafka-topics.sh --bootstrap-server 172.18.11.38:9092 --describe --topic bind
```

### 6.3 创建业务 Topic

```bash id="a20"
docker exec -it kafka01 /opt/kafka/bin/kafka-topics.sh --bootstrap-server 172.18.11.38:9092 --create --topic bind --partitions 1 --replication-factor 1
```

---

## 7. 处理结论

本次 Kafka 故障原因如下：

1. Kafka 原 Docker 网络与公司新办公网段发生冲突，导致外部客户端无法正常访问 Kafka。
2. Kafka 挂载目录权限不足，且服务器启用 SELinux，导致 Kafka 启动失败。
3. Kafka 恢复后，原业务 Topic `bind` 不存在，需要重新创建。

---

## 8. 运维建议

1. Docker 自定义网络应避开公司现网常用网段，避免与办公网、服务器网段重叠。
2. Kafka 数据目录挂载到宿主机时，应提前设置好目录属主、权限及 SELinux 上下文。
3. 在 SELinux 为 `Enforcing` 的环境下，容器挂载建议统一使用 `:Z`。
4. Kafka 重建或迁移后，应检查业务 Topic 是否仍存在，避免消费者因 Topic 缺失报错。

你要的话，我也可以继续帮你整理成“故障处理步骤 + 回滚方案 + 验收结果”的正式模板。
