# 🌟 Lifestyle Selection (高并发生活服务平台)

<p align="center">
  <a href="https://github.com/itnmz/Lifestyle-Selection">
    <img src="https://img.shields.io/badge/Project-Lifestyle--Selection-blue?style=flat-square&logo=github" alt="Repository">
  </a>
  <img src="https://img.shields.io/badge/License-MIT-green.svg?style=flat-square" alt="License">
  <img src="https://img.shields.io/badge/PRs-Welcome-brightgreen.svg?style=flat-square" alt="PRs Welcome">
</p>

---

## 📖 目录

- [项目简介](#-项目简介)
- [核心功能](#-核心功能)
- [系统架构](#-系统架构)
- [技术选型](#-技术选型)
- [目录结构](#-目录结构)
- [快速开始](#-快速开始)
  - [前置依赖](#前置依赖)
  - [环境变量配置](#环境变量配置)
  - [本地运行](#本地运行)
- [API 接口说明](#-api-接口说明)
- [贡献指南](#-贡献指南)
- [开源协议](#-开源协议)

---

📌 项目简介

**生活优选** 是一款以用户点评与商户信息为主的高并发生活服务平台[cite: 1]。

系统涵盖了用户认证、商户信息与评价浏览、优惠券高并发秒杀、社交关注及博主动态推送等核心业务场景[cite: 1]。项目基于 Spring Boot 与 Redis/RabbitMQ 构建[cite: 1]，针对高并发下的数据一致性、缓存击穿/穿透、秒杀超卖以及分布式会话共享等常见痛点，提供了完整的高性能后端解决方案[cite: 1]。

---

## ✨ 核心功能

* **🔐 用户身份认证与权限管理**
  * 支持安全登录校验、基于 Redis 的集群 Session 会话保持与动态续期。

* **🏪 商家详情与探店评价**
  * 支持多分类商户查询、商户详情浏览以及用户发布探店笔记与评价。

* **⚡ 高并发优惠券秒杀**
  * 支持优惠券秒杀抢购，结合 Redis 分布式锁与 Lua 脚本防超卖，并基于 RabbitMQ 实现订单异步下单。

* **👥 好友关注与动态 Feed 流**
  * 支持用户互关，采用基于 Timeline 的推模式（Push）将博主更新的探店动态实时投喂至粉丝收件箱。

* **📱 响应式视图 / 规范化 API**
  * 提供结构清晰的 RESTful API 接口，结合 ThreadLocal 维护请求上下文，保证交互体验。

---

## 🏗️ 系统架构

```text
========================================================================
                          前端客户端 (Web / H5)                         
========================================================================
                                   |
                                   | HTTP / RESTful API
                                   v
========================================================================
                         核心安全与请求拦截器                           
             (HandlerInterceptor + ThreadLocal 上下文传递)              
========================================================================
                                   |
                                   v
========================================================================
                     业务逻辑服务层 (Spring Boot)                        
                                                                        
  [用户与Session]    [商户查询与缓存]    [秒杀与分布式锁]    [好友Feed流]   
========================================================================
         |                         |                         |
         | 分布式会话/缓存/Feed       | 消息队列异步解耦           | 数据持久化
         v                         v                         v
+-----------------+       +-----------------+       +-----------------+
|      Redis      |       |    RabbitMQ     |       |      MySQL      |
|  (分布式锁/缓存)  |        |  (秒杀订单削峰)  |        |  (MyBatis-Plus) |
+-----------------+       +-----------------+       +-----------------+
```

---

## 🛠️ 技术选型

### 后端 / 核心服务
* **核心语言 / 框架**：Java 17 / Spring Boot 3.x
* **ORM 框架**：MyBatis-Plus
* **数据库**：MySQL 8.0
* **缓存与分布式锁**：Redis / Redisson
* **消息中间件**：RabbitMQ
* **安全 / 认证**：Redis Session + 拦截器 + ThreadLocal

---

## 📁 目录结构

```text
Lifestyle-Selection/
├── docs/                   # 项目设计文档及 API 说明
├── src/
│   ├── main/
│   │   ├── java/           # 后端源码目录
│   │   │   └── com/
│   │   │       └── lifestyle/
│   │   │           ├── controller/   # 控制层 (API)
│   │   │           ├── service/      # 业务逻辑层
│   │   │           ├── mapper/       # 数据持久层
│   │   │           └── model/        # 数据模型实体
│   │   └── resources/      # 配置文件与 SQL 脚本
│   └── test/               # 单元测试
├── .gitignore              # Git 忽略配置
├── pom.xml                 # Maven 依赖管理
├── LICENSE                 # 开源协议
└── README.md               # 项目说明文档

```

---

## 🚀 快速开始

### 前置依赖

运行本项目前，请确保本地配置了以下环境：

* **JDK**: >= 17
* **Maven**: >= 3.8
* **MySQL**: >= 8.0
* **Redis**: >= 6.0

### 环境变量配置

1. 在 `src/main/resources/` 目录下复制配置文件模版：
```bash
cp application-example.yml application.yml

```


2. 修改 `application.yml` 中的数据库及 Redis 连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lifestyle_selection?useSSL=false&serverTimezone=UTC
    username: YOUR_DB_USERNAME
    password: YOUR_DB_PASSWORD
  data:
    redis:
      host: localhost
      port: 6379

```



### 本地运行

1. **克隆项目仓库**
```bash
git clone [https://github.com/itnmz/Lifestyle-Selection.git](https://github.com/itnmz/Lifestyle-Selection.git)
cd Lifestyle-Selection

```


2. **初始化数据库**
执行 `src/main/resources/sql/schema.sql` 脚本，创建对应的表结构与初始数据。
3. **构建并启动服务**
```bash
mvn clean package
mvn spring-boot:run

```


4. 启动成功后，默认服务端口为 `8080`，可访问 `http://localhost:8080` 进行测试。

---



## 📄 API 接口说明

| HTTP方法 | 接口路径 | 功能描述 |
| :--- | :--- | :--- |
| `POST` | `/user/code` | 发送手机验证码 |
| `POST` | `/user/login` | 用户短信验证码登录/注册 |
| `GET` | `/shop/{id}` | 查询商户详情（带 Redis 缓存） |
| `GET` | `/shop/of/type` | 根据商户类型分页查询商户列表 |
| `POST` | `/voucher-order/seckill/{id}` | 抢购优惠券（秒杀接口） |
| `POST` | `/blog` | 发布探店笔记/博客 |
| `PUT` | `/follow/{followUserId}/{isFollow}` | 关注 / 取消关注博主 |
| `GET` | `/blog/of/follow` | 关注 Feed 流滚动分页查询 |
---

## 🤝 贡献指南

我们非常欢迎来自社区的贡献！如果你有好的建议或发现了 Bug：

1. **Fork** 本仓库。
2. 创建你的特性分支 (`git checkout -b feature/YourFeature`)。
3. 提交你的更改 (`git commit -m 'Add some YourFeature'`)。
4. 推送到该分支 (`git push origin feature/YourFeature`)。
5. 提交一个 **Pull Request**。

---

## 📄 开源协议

本项目遵守 [MIT License](https://www.google.com/search?q=LICENSE) 开源协议。
