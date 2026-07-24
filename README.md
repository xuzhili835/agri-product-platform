# 农产品融销一体平台

> 打通「农产品销售 + 融资服务 + 专家助力」的全栈 Web 平台，面向农户、买家、银行、技术专家与管理员五类角色，实现融销一体、产销闭环。

## ✨ 功能特性

- **👤 用户中心**：注册（集成 Cloudflare Turnstile 人机验证）/ 登录（JWT）/ 角色升级申请（农户·买家 → 专家·银行，待管理员审核）
- **🛒 买家**：浏览 / 关键词搜索商品、购物车、下单、支付宝沙箱模拟支付、订单管理、确认收货
- **🧑‍🌾 农户**：发布与管理商品、订单发货、销售额概览、申请融资、智能匹配、向专家提问 / 预约专家
- **🏦 银行**：融资产品管理、融资审批、信用分维护、还贷管理、智能匹配、联合贷款人（融资数据按银行隔离，仅可见本行业务）
- **🎓 技术专家**：农业知识库、多轮追问问答、预约处理
- **🛠 管理员**：用户管理、角色申请审核、信用分维护、轮播图与内容管理

## 🛠 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3.2 · Java 17 · MyBatis-Plus · MySQL 8 · JWT · BCrypt · 支付宝沙箱 · Cloudflare Turnstile |
| 前端 | Vue 3 · Vite 5 · Element Plus · Pinia · Vue Router · Axios |
| 部署 | Nginx（静态托管 + `/api` 反向代理） |

## 📁 项目结构

```
backend/     Spring Boot 后端（端口 8080，上下文 /api）
frontend/    Vue 3 前端（Vite 开发端口 5173）
```

## 🚀 快速开始

### 环境要求
JDK 17、Maven、Node.js 18+、MySQL 8

### 1. 数据库
```sql
CREATE DATABASE agri_platform DEFAULT CHARSET utf8mb4;
```
导入初始结构与数据：`backend/src/main/resources/sql/agri_platform.sql`

### 2. 后端
在 `backend/` 下新建 `application-local.yml`（已被 `.gitignore` 忽略、不会提交），填入**本机**密钥：
```yaml
spring:
  datasource:
    password: 你的MySQL密码
jwt:
  secret: 你的JWT密钥
```
启动：
```bash
cd backend
mvn spring-boot:run      # 默认 8080，上下文 /api
```

### 3. 前端
```bash
cd frontend
npm install
npm run dev              # 默认 5173
```
前端通过相对路径 `/api` 访问后端，开发时可用 Vite 代理或配合 Nginx。

## 📸 界面截图

（待补充）

## 协议

本项目暂未声明开源协议。
