# NeoFlow

基于Neo4j + Spring Boot 实现的流程引擎

- [开发环境](#开发环境)
- [主要功能](#主要功能)
- [代码结构](#代码结构)
- [数据建模](#数据建模)
- [配置](#配置)
- [自定义注解](#自定义注解)
- [组件](#组件)
- [扩展接口](#扩展接口)
- [接口文档](#接口文档)
  - [ProcessController 流程](#ProcessController-流程)
    - [新建流程](#新建流程)
    - [查询流程列表](#查询流程列表)
    - [变更流程启用状态](#变更流程启用状态)
    - [变更流程启用版本](#变更流程启用版本)
    - [查询流程版本启用历史](#查询流程版本启用历史)
    - [流程统计查询](#流程统计查询)
    - [查询流程名称列表](#查询流程名称列表)
  - [VersionController 版本](#VersionController-版本)
    - [查询流程版本列表](#查询流程版本列表)
    - [查询流程版本视图](#查询流程版本视图)
    - [查询流程版本迭代](#查询流程版本迭代)
    - [创建版本时获取候选人选择列表](#创建版本时获取候选人选择列表)
    - [创建流程版本模型](#创建流程版本模型)
  - [ExecuteController 执行](#ExecuteController-执行)
    - [执行流程](#执行流程)
    - [批量执行流程](#批量执行流程)
    - [流程实例移植版本](#流程实例移植版本)
    - [批量移植流程实例版本](#批量移植流程实例版本)
    - [手动执行自动节点](#手动执行自动节点)
  - [QueryController 泛用查询](#QueryController-泛用查询)
    - [查询当前用户在各流程的待办数量](#查询当前用户在各流程的待办数量)
    - [查询当前用户 发起/待办/已办 列表](#查询当前用户-发起待办已办-列表)
    - [通过节点 名称/身份 查询节点状态](#通过节点-名称身份-查询节点状态)
    - [查询流程实例操作历史](#查询流程实例操作历史) 
  - [CacheController 缓存](#CacheController-缓存)
    - [缓存统计](#缓存统计) 

## 开发环境

- jdk 21
- Spring Boot 3.2.4
- Neo4j 5.18.1

## 主要功能

- 流程类型的创建和查询、版本管理、数据统计
- 流程版本模型的定义查询、迭代树查询
- 流程实例节点的自动流转；可配置的扫描执行自动节点的定时任务；未完结流程实例的模型版本切换
- 基于用户的 发起、待办、已办 列表查询；基于用户和节点名称/标识的 节点状态信息查询
- 支持配置和扩展的锁实现（默认NewKeySet）；支持配置和扩展的缓存实现（默认caffeine）
- 自定义扩展的候选人选择接口，方便贴合业务需求
- 支持配置更换Cypher脚本

## 代码结构

- java
  - org.nf.neoflow
    - annotation：自定义注解
    - component：主要逻辑组件
    - config：配置
    - constants：常量
    - controller：对外接口
    - dto：提交/返回 数据结构
    - enums：枚举
    - exception：自定义异常
      - handler：全局异常处理器
    - interfaces：需实现的扩展接口
    - models：节点实体
    - repository：数据层
    - service：业务方法
    - utils：工具类
    - NeoFlowApplication：启动类
- resources
  - cypher：Cypher脚本
  - META_INF：自动装配文件
  - application.yml： openapi配置
 
## 数据建模


![NeoFlow数据建模](https://github.com/PC8650/NeoFlow/assets/70273864/7db207ce-dbee-48cb-9716-4086a80e6f2f)


## 配置

- NeoFlowConfig：主要配置，包括 基础（部署方式、扫描路径、接口路径前缀、当前用户获取方式 等）、锁（是否自定义锁实现）、缓存（是否自定义缓存实现、默认缓存实现相关参数）、自动节点（定时任务cron，线程池参数）
- TransactionConfig：注入TransactionTemplate到容器，用于编程式事务
- OpenApiConfig：openapi基础配置
- NeoFlowAutoConfig：自动装配入口

## 自定义注解

- @ProcessOperator：标注在类上，表示用来进行流程业务操作
  - string name：流程类型名称，全局唯一
- @ProcessMethod：标注在@ProcessOperator所标注类的方法上，表示流程节点具体执行的方法
  - string name：方法名称，用于节点方法和终止方法配置，一个类中应该唯一
 
## 组件

- CypherScriptExecutor：在程序启动完成后，获取执行neo4j的约束、索引脚本
- OperatorManager：用于集成业务部署时，在程序启动后扫描配置路径中的@ProcessOperator和@ProcessMethod，获取流程名称和方法，封装成function函数map，方便后续调用
- FlowExecutor：完成流程的所有执行操作（发起、同意、拒绝、转发、终止）、批量操作、扫描执行自动节点定时任务，以及其后的流程状态更新
- BaseUserChoose：获取当前用户信息，候选人选择与校验操作
- NeoLockManager：锁管理
- NeoCacheManager：缓存管理
  
## 扩展接口

- UserChoose：用户信息获取、候选人选择和校验。除了（getUser()、getCandidateList()根据配置和需求选择是否重写，其余方法都需要重写）
- CustomizationLock：自定义锁的获取和释放方式，在配置开启自定义锁方式时需要实现和重写方法
- CustomizationCache：自定义缓存的读写方式，在配置开启自定义缓存方式时需要实现和重写方法

## 接口文档

响应参数总体分为两部分

| 名称   | 类型     | 含义                |
| ---- | ------ | ----------------- |
| date | json   | 数据（数据 / 异常 内容）    |
| msg  | string | 消息（sucess / fail） |

下列文档中的响应参数，主要指**data**中可能有值的参数(不包含分页数据)

### ProcessController 流程

#### 新建流程

**请求地址**：${neo.prefix:/neo}/process/create

**请求方式**：post

**请求参数**

| 名称       | 类型     | 必填  | 含义    | 备注                                 |
| -------- | ------ | --- | ----- | ---------------------------------- |
| name     | string | Y   | 流程名称  |                                    |
| createBy | string | N   | 创建人标识 | 根据配置NeoFlowConfig.baseUserChoose决定 |

**响应参数**

| 名称            | 类型      | 含义     | 备注         |
| ------------- | ------- | ------ | ---------- |
| id            | number  | id     |            |
| name          | string  | 流程名称   |            |
| active        | boolean | 是否激活   |            |
| createBy      | string  | 创建人标识  |            |
| updateBy      | string  | 更新人标识  |            |
| createTime    | string  | 创建时间   |            |
| updateTime    | string  | 更新时间   |            |

**请求示例**

`/neo/process/create`

```json
{
    "name":"划款",
    "createBy":"2"
}
```

**响应示例**

```json
{
	"data": {
		"id": 0,
		"name": "划款",
		"active": false,
		"activeVersion": null,
		"activeHistory": null,
		"createBy": "2",
		"updateBy": "2",
		"createTime": "2024-05-19 14:40:35",
		"updateTime": "2024-05-19 14:40:35"
	},
	"msg": "success"
}
```

#### 查询流程列表

**请求地址**：${neo.prefix:/neo}/process/list

**请求方式**：get

**请求参数**

| 名称         | 类型      | 必填  | 含义          | 备注       |
| ---------- | ------- | --- | ----------- | -------- |
| name       | string  | N   | 流程名称        |          |
| createBy   | string  | N   | 创建人标识       |          |
| desc       | boolean | N   | 是否按创建时间降序排列 | 默认 false |
| pageNumber | number  | N   | 页码          | 默认 1     |
| pageSize   | number  | N   | 每页显示数量      | 默认 15    |

**响应参数**

| 名称      | 类型    | 含义   | 备注  |
| ------- | ----- | ---- | --- |
| content | array | 分页列表 |     |

<font color=red>content</font>

| 名称         | 类型      | 含义   | 备注  |
| ---------- | ------- | ---- | --- |
| name       | string  | 流程名称 |     |
| active     | boolean | 是否激活 |     |
| createTime | string  | 创建时间 |     |
| updateTime | string  | 更新时间 |     |

**请求示例**

`/neo/process/list?desc=true&name=划款`

**响应示例**

```json
{
	"data": {
		"content": [
			{
				"id": null,
				"name": "划款",
				"active": false,
				"activeVersion": null,
				"activeHistory": null,
				"createBy": null,
				"updateBy": null,
				"createTime": "2024-05-19 14:40:35",
				"updateTime": "2024-05-19 14:40:35"
			}
		],
		"pageable": {
			"pageNumber": 0,
			"pageSize": 15,
			"sort": {
				"empty": false,
				"sorted": true,
				"unsorted": false
			},
			"offset": 0,
			"paged": true,
			"unpaged": false
		},
		"last": true,
		"totalPages": 1,
		"totalElements": 1,
		"numberOfElements": 1,
		"first": true,
		"size": 15,
		"number": 0,
		"sort": {
			"empty": false,
			"sorted": true,
			"unsorted": false
		},
		"empty": false
	},
	"msg": "success"
}
```

#### 变更流程启用状态

**请求地址**：${neo.prefix:/neo}/process/changeActive

**请求方式**：post

**请求参数**

| 名称       | 类型      | 必填  | 含义    | 备注                                 |
| -------- | ------- | --- | ----- | ---------------------------------- |
| name     | string  | Y   | 流程名称  |                                    |
| active   | boolean | Y   | 更改的状态 | true-启用，false-关闭                   |
| updateBy | string  | N   | 更新人标识 | 根据配置NeoFlowConfig.baseUserChoose决定 |

**响应参数**

| 名称            | 类型      | 含义     | 备注         |
| ------------- | ------- | ------ | ---------- |
| name          | string  | 流程名称   |            |
| active        | boolean | 激活状态   |            |
| activeVersion | number  | 流程启用版本 | 变更时的流程启用版本 |
| remark        | string  | 备注     |            |

**请求示例**

`/neo/process/changeActive`

```json
{
    "name":"划款",
    "active":true,
    "updateBy":"2"
}
```

**响应示例**

```json
{
	"data": {
		"name": "划款",
		"active": true,
		"activeVersion": null,
		"remark": "流程已激活"
	},
	"msg": "success"
}
```

#### 变更流程启用版本

**请求地址**：${neo.prefix:/neo}/process/changeVersion

**请求方式**：post

**请求参数**

| 名称            | 类型     | 必填  | 含义     | 备注                                 |
| ------------- | ------ | --- | ------ | ---------------------------------- |
| name          | string | Y   | 流程名称   |                                    |
| activeVersion | number | Y   | 变更的版本号 | 不能小于1                              |
| updateBy      | string | N   | 更新人标识  | 根据配置NeoFlowConfig.baseUserChoose决定 |
| updateByName  | string | N   | 更新人名称  | 根据配置NeoFlowConfig.baseUserChoose决定 |

**响应参数**

null

**请求示例**

`/neo/process/changeVersion`

```json
{
    "name": "划款",
    "activeVersion": 1,
    "updateBy":"2",
    "updateByName":"张三"
}
```

**响应示例**

```json
{
	"data": null,
	"msg": "success"
}
```

#### 查询流程版本启用历史

**请求地址**：${neo.prefix:/neo}/process/activeHistory

**请求方式**：get

**请求参数**

| 名称   | 类型     | 必填  | 含义   | 备注  |
| ---- | ------ | --- | ---- | --- |
| name | string | Y   | 流程名称 |     |

**响应参数**

| 名称  | 类型    | 含义   | 备注  |
| --- | ----- | ---- | --- |
|     | array | 历史列表 |     |

| 名称         | 类型     | 含义    | 备注  |
| ---------- | ------ | ----- | --- |
| version    | number | 启用版本  |     |
| activeId   | string | 启用人标识 |     |
| activeName | string | 启用人名称 |     |
| activeTime | string | 启用时间  |     |

**请求示例**

`/neo/process/activeHistory?name=划款`

**响应示例**

```json
{
	"data": [
		{
			"version": 1,
			"activeId": "2",
			"activeName": "张三",
			"activeTime": "2024-05-19 15:29:38"
		}
	],
	"msg": "success"
}
```

#### 流程统计查询

**请求地址**：${neo.prefix:/neo}/process/statistics

**请求方式**：post

**请求参数**

| 名称         | 类型     | 必填  | 含义       | 备注         |
| ---------- | ------ | --- | -------- | ---------- |
| name       | string | N   | 流程名称     |            |
| version    | number | N   | 流程版本     | 不能小于1      |
| beginStart | string | N   | 流程开始时间起始 | yyyy-MM-dd |
| beginEnd   | string | N   | 流程开始时间结束 | yyyy-MM-dd |
| endStart   | string | N   | 流程结束时间起始 | yyyy-MM-dd |
| endEnd     | string | N   | 流程结束时间结束 | yyyy-MM-dd |
| pending    | number | N   | 最低流程进行数  | 不能小于0      |
| complete   | number | N   | 最低流程完成数  | 不能小于0      |
| rejected   | number | N   | 最低流程拒绝数  | 不能小于0      |
| terminated | umber  | N   | 最低流程终止数  | 不能小于0      |
| total      | number | N   | 最低流程总数   | 不能小于0      |

**响应参数**

| 名称  | 类型    | 含义   | 备注  |
| --- | ----- | ---- | --- |
|     | array | 统计信息 |     |

| 名称         | 类型     | 含义       | 备注  |
| ---------- | ------ | -------- | --- |
| name       | string | 流程名称     |     |
| pending    | number | 进行中的流程数量 |     |
| complete   | number | 已完成的流程数量 |     |
| rejected   | number | 已拒绝的流程数量 |     |
| terminated | number | 已终止的流程数量 |     |
| total      | number | 流程总数     |     |
| version    | array  | 版本信息     |     |

<font color=red>version</font>

| 名称         | 类型     | 含义       | 备注  |
| ---------- | ------ | -------- | --- |
| pending    | number | 进行中的流程数量 |     |
| complete   | number | 已完成的流程数量 |     |
| rejected   | number | 已拒绝的流程数量 |     |
| terminated | number | 已终止的流程数量 |     |
| total      | number | 流程总数     |     |
| version    | number | 版本       |     |

**请求示例**

`/neo/process/statistics`

```json
{
    "name": "划款"
}
```

**响应示例**

```json
{
	"data": [
		{
			"name": "划款",
			"pending": 0,
			"complete": 0,
			"rejected": 0,
			"terminated": 0,
			"total": 0,
			"version": [
				{
					"total": 0,
					"rejected": 0,
					"pending": 0,
					"complete": 0,
					"version": 1,
					"terminated": 0
				}
			]
		}
	],
	"msg": "success"
}
```

#### 查询流程名称列表

**请求地址**：${neo.prefix:/neo}/process/name

**请求方式**：get

**请求参数**

null

**响应参数**

| 名称  | 类型             | 含义     | 备注  |
| --- | -------------- | ------ | --- |
|     | array\<string> | 流程名称列表 |     |

**请求示例**

`/neo/process/name`

**响应示例**

```json
{
	"data": [
		"划款"
	],
	"msg": "success"
}
```

### VersionController 版本

#### 查询流程版本列表

**请求地址**：${neo.prefix:/neo}/version/list

**请求方式**：get

**请求参数**

| 名称          | 类型      | 必填  | 含义       | 备注       |
| ----------- | ------- | --- | -------- | -------- |
| processName | string  | Y   | 流程名称     |          |
| desc        | boolean | N   | 是否创建时间倒序 | 默认 false |
| pageNumber  | number  | N   | 页码       | 默认 1     |
| pageSize    | number  | N   | 每页显示数量   | 默认 15    |

**响应参数**

| 名称      | 类型    | 含义   | 备注  |
| ------- | ----- | ---- | --- |
| content | array | 分页列表 |     |

<font color=red>content</font>

| 名称              | 类型     | 含义        | 备注  |
| --------------- | ------ | --------- | --- |
| processName     | string | 流程名称      |     |
| version         | number | 流程版本      |     |
| iterateFrom     | number | 迭代自版本号    |     |
| cycle           | number | 允许退回发起的次数 |     |
| terminateMethod | string | 终止方法      |     |
| createBy        | string | 创建人标识     |     |
| createByName    | string | 创建人名称     |     |
| createTime      | string | 创建时间      |     |

**请求示例**

`/neo/version/list?processName=划款`

**响应示例**

```json
{
	"data": {
		"content": [
			{
				"processName": "划款",
				"version": 1,
				"iterateFrom": null,
				"cycle": 1,
				"terminateMethod": null,
				"createBy": "2",
				"createByName": "张三",
				"createTime": "2024-05-19 15:29:22"
			}
		],
		"pageable": {
			"pageNumber": 0,
			"pageSize": 15,
			"sort": {
				"empty": false,
				"sorted": true,
				"unsorted": false
			},
			"offset": 0,
			"paged": true,
			"unpaged": false
		},
		"last": true,
		"totalElements": 1,
		"totalPages": 1,
		"size": 15,
		"number": 0,
		"sort": {
			"empty": false,
			"sorted": true,
			"unsorted": false
		},
		"first": true,
		"numberOfElements": 1,
		"empty": false
	},
	"msg": "success"
}
```

#### 查询流程版本视图

**请求地址**：${neo.prefix:/neo}/version/view

**请求方式**：get

**请求参数**

| 名称          | 类型     | 必填  | 含义   | 备注    |
| ----------- | ------ | --- | ---- | ----- |
| processName | string | Y   | 流程名称 |       |
| version     | number | Y   | 版本号  | 不能小于1 |

**响应参数**

| 名称               | 类型     | 含义        | 备注   |
| ---------------- | ------ | --------- | ---- |
| processName      | string | 流程名称      |      |
| iterateFrom      | number | 迭代自版本号    |      |
| version          | number | 版本号       |      |
| cycle            | number | 允许退回发起的次数 |      |
| terminatedMethod | string | 终止方法      |      |
| componentModel   | map    | 组件模型      | 展开即可 |
| versionNodes     | array  | 版本节点      |      |
| versionEdges     | array  | 版本边       |      |

<font color=red>versionNodes</font>

| 名称                     | 类型      | 含义           | 备注                  |
| ---------------------- | ------- | ------------ | ------------------- |
| nodeUid                | string  | 节点uid        |                     |
| name                   | string  | 节点名称         |                     |
| identity               | string  | 节点标识         |                     |
| operationType          | number  | 节点操作类型       |                     |
| operationCandidateInfo | array   | 节点操作候选人      |                     |
| operationMethod        | string  | 节点操作方法       |                     |
| onlyPassExecute        | boolean | 是否只同意通过才执行方法 |                     |
| autoInterval           | num     | 自动执行间隔       |                     |
| defaultPassCondition   | num     | 默认通过时的跳转条件   |                     |
| location               | num     | 节点位置         | 1-开始，2-中间，3-完成，4-终止 |
| x                      | num     | 显示位置x坐标      |                     |
| y                      | num     | 显示位置y坐标      |                     |

<font color=red>operationCandidateInfo</font>

| 名称   | 类型     | 含义  | 备注  |
| ---- | ------ | --- | --- |
| id   | string | id  |     |
| name | string | 名称  |     |

<font color=red>versionEdges</font>

| 名称            | 类型             | 含义      | 备注     |
| ------------- | -------------- | ------- | ------ |
| startNode     | number         | 开始节点uid |        |
| endNode       | number         | 结束节点uid |        |
| condition     | number         | 条件      |        |
| startLocation | array\<number> | 起始坐标    | \[x,y] |
| endLocation   | array\<number> | 结束坐标    | \[x,y] |

**请求示例**

`/neo/version/view?processName=划款&version=1`


**响应示例**

```json
{
	"data": {
		"processName": "划款",
		"iterateFrom": null,
		"version": 1,
		"cycle": 1,
		"terminatedMethod": null,
		"componentModel": {
			"发起节点": {
				"nodeUid": null,
				"name": "发起",
				"identity": null,
				"operationType": null,
				"operationCandidateInfo": [],
				"operationMethod": null,
				"onlyPassExecute": true,
				"autoInterval": 0,
				"defaultPassCondition": null,
				"location": 1,
				"x": null,
				"y": null
			},
			"中间节点": {
				"nodeUid": null,
				"name": null,
				"identity": null,
				"operationType": null,
				"operationCandidateInfo": [],
				"operationMethod": null,
				"onlyPassExecute": true,
				"autoInterval": null,
				"defaultPassCondition": null,
				"location": 2,
				"x": null,
				"y": null
			},
			"终止节点": {
				"nodeUid": null,
				"name": "终止",
				"identity": null,
				"operationType": null,
				"operationCandidateInfo": [],
				"operationMethod": null,
				"onlyPassExecute": true,
				"autoInterval": 0,
				"defaultPassCondition": null,
				"location": 4,
				"x": null,
				"y": null
			},
			"条件关系": {
				"startNode": null,
				"endNode": null,
				"condition": null,
				"startLocation": null,
				"endLocation": null
			},
			"完成节点": {
				"nodeUid": null,
				"name": "完成",
				"identity": null,
				"operationType": null,
				"operationCandidateInfo": [],
				"operationMethod": null,
				"onlyPassExecute": true,
				"autoInterval": 0,
				"defaultPassCondition": null,
				"location": 3,
				"x": null,
				"y": null
			}
		},
		"versionNodes": [
			{
				"id": null,
				"nodeUid": "1",
				"name": "发起",
				"identity": "Start",
				"operationType": 0,
				"operationCandidate": null,
				"operationCandidateInfo": null,
				"operationMethod": "begin",
				"onlyPassExecute": true,
				"autoInterval": 0,
				"defaultPassCondition": null,
				"location": 1,
				"x": 1.1,
				"y": 1.1
			},
			{
				"id": null,
				"nodeUid": "2",
				"name": "中间节点1",
				"identity": "Middle1",
				"operationType": 2,
				"operationCandidate": null,
				"operationCandidateInfo": [
					{
						"name": "张三",
						"id": "2"
					}
				],
				"operationMethod": "verify",
				"onlyPassExecute": true,
				"autoInterval": null,
				"defaultPassCondition": null,
				"location": 2,
				"x": 1.1,
				"y": 1.1
			},
			{
				"id": null,
				"nodeUid": "3",
				"name": "中间节点2",
				"identity": "Middle2",
				"operationType": 2,
				"operationCandidate": null,
				"operationCandidateInfo": [
					{
						"name": "张三",
						"id": "2"
					}
				],
				"operationMethod": "verify",
				"onlyPassExecute": true,
				"autoInterval": null,
				"defaultPassCondition": null,
				"location": 2,
				"x": 1.1,
				"y": 1.1
			},
			{
				"id": null,
				"nodeUid": "7",
				"name": "终止",
				"identity": "Terminate",
				"operationType": 0,
				"operationCandidate": null,
				"operationCandidateInfo": null,
				"operationMethod": "",
				"onlyPassExecute": true,
				"autoInterval": 0,
				"defaultPassCondition": null,
				"location": 4,
				"x": 1.1,
				"y": 1.1
			},
			{
				"id": null,
				"nodeUid": "4",
				"name": "中间节点3",
				"identity": "Middle3",
				"operationType": 2,
				"operationCandidate": null,
				"operationCandidateInfo": [
					{
						"name": "张三",
						"id": "2"
					}
				],
				"operationMethod": "verify",
				"onlyPassExecute": true,
				"autoInterval": null,
				"defaultPassCondition": null,
				"location": 2,
				"x": 1.1,
				"y": 1.1
			},
			{
				"id": null,
				"nodeUid": "5",
				"name": "中间节点4",
				"identity": "Middle4",
				"operationType": 2,
				"operationCandidate": null,
				"operationCandidateInfo": [
					{
						"name": "张三",
						"id": "2"
					}
				],
				"operationMethod": "verify",
				"onlyPassExecute": true,
				"autoInterval": null,
				"defaultPassCondition": null,
				"location": 2,
				"x": 1.1,
				"y": 1.1
			},
			{
				"id": null,
				"nodeUid": "6",
				"name": "完成",
				"identity": "Complete",
				"operationType": 0,
				"operationCandidate": null,
				"operationCandidateInfo": null,
				"operationMethod": "",
				"onlyPassExecute": true,
				"autoInterval": 0,
				"defaultPassCondition": null,
				"location": 3,
				"x": 1.1,
				"y": 1.1
			}
		],
		"versionEdges": [
			{
				"startNode": "1",
				"endNode": "2",
				"condition": 1,
				"startLocation": [
					1.1,
					1.1
				],
				"endLocation": [
					1.1,
					1.1
				]
			},
			{
				"startNode": "2",
				"endNode": "3",
				"condition": 1,
				"startLocation": [
					1.1,
					1.1
				],
				"endLocation": [
					1.1,
					1.1
				]
			},
			{
				"startNode": "3",
				"endNode": "7",
				"condition": 2,
				"startLocation": [
					1.1,
					1.1
				],
				"endLocation": [
					1.1,
					1.1
				]
			},
			{
				"startNode": "3",
				"endNode": "4",
				"condition": 1,
				"startLocation": [
					1.1,
					1.1
				],
				"endLocation": [
					1.1,
					1.1
				]
			},
			{
				"startNode": "4",
				"endNode": "7",
				"condition": 2,
				"startLocation": [
					1.1,
					1.1
				],
				"endLocation": [
					1.1,
					1.1
				]
			},
			{
				"startNode": "4",
				"endNode": "5",
				"condition": 1,
				"startLocation": [
					1.1,
					1.1
				],
				"endLocation": [
					1.1,
					1.1
				]
			},
			{
				"startNode": "5",
				"endNode": "6",
				"condition": 1,
				"startLocation": [
					1.1,
					1.1
				],
				"endLocation": [
					1.1,
					1.1
				]
			},
			{
				"startNode": "5",
				"endNode": "7",
				"condition": 2,
				"startLocation": [
					1.1,
					1.1
				],
				"endLocation": [
					1.1,
					1.1
				]
			},
			{
				"startNode": "2",
				"endNode": "7",
				"condition": 2,
				"startLocation": [
					1.1,
					1.1
				],
				"endLocation": [
					1.1,
					1.1
				]
			}
		]
	},
	"msg": "success"
}
```


#### 查询流程版本迭代

**请求地址**：${neo.prefix:/neo}/version/iterate

**请求方式**：get

**请求参数**

| 名称          | 类型     | 必填  | 含义   | 备注                                                                            |
| ----------- | ------ | --- | ---- | ----------------------------------------------------------------------------- |
| processName | string | Y   | 流程名称 |                                                                               |
| type        | number | N   | 查询类型 | 1-非嵌套，迭代列表只包含直系迭代；  <br>2-嵌套，迭代列表包含非直系迭代；  <br>3-图，返回节点和关系（不包含坐标）<br><br>默认 1 |

**响应参数**

**type=1、type=2**

| 名称  | 类型    | 含义   | 备注  |
| --- | ----- | ---- | --- |
|     | array | 迭代列表 |     |

| 名称               | 类型             | 含义        | 备注      |
| ---------------- | -------------- | --------- | ------- |
| version          | number         | 版本号       |         |
| cycle            | number         | 允许退回发起的次数 |         |
| terminatedMethod | string         | 终止方法      |         |
| createBy         | string         | 创建人标识     |         |
| createByName     | string         | 创建人名称     |         |
| createTime       | string         | 创建时间      |         |
| iterateCount     | number         | 直系迭代数量    |         |
| iterateVersion   | array\<number> | 直系迭代版本号   |         |
| iterate          | array          | 迭代列表      | 与当前表格相同 |

**type=3**

| 名称    | 类型    | 含义   | 备注  |
| ----- | ----- | ---- | --- |
| nodes | array | 版本节点 |     |
| edges | array | 迭代关系 |     |

<font color=red>nodes</font>

| 名称               | 类型             | 含义        | 备注  |
| ---------------- | -------------- | --------- | --- |
| version          | number         | 版本号       |     |
| cycle            | number         | 允许退回发起的次数 |     |
| terminatedMethod | string         | 终止方法      |     |
| createBy         | string         | 创建人标识     |     |
| createByName     | string         | 创建人名称     |     |
| createTime       | string         | 创建时间      |     |
| iterateCount     | number         | 直系迭代数量    |     |
| iterateVersion   | array\<number> | 直系迭代版本号   |     |

<font color=red>edges</font>

| 名称        | 类型     | 含义     | 备注  |
| --------- | ------ | ------ | --- |
| startNode | string | 开始节点版本 |     |
| endNode   | string | 结束节点版本 |     |

**请求示例**

`/neo/version/iterate?processName=划款&type=1`

**响应示例**

```json
{
	"data": [
		{
			"version": 1,
			"cycle": 1,
			"terminatedMethod": null,
			"createBy": "2",
			"createByName": "张三",
			"createTime": "2024-05-19 15:29:22",
			"iterateCount": 1,
			"iterateVersion": [
				2
			],
			"iterate": [
				{
					"version": 2,
					"cycle": 1,
					"terminatedMethod": null,
					"createBy": "2",
					"createByName": "张三",
					"createTime": "2024-05-19 16:58:26",
					"iterateCount": null,
					"iterateVersion": null,
					"iterate": []
				}
			]
		},
		{
			"version": 2,
			"cycle": 1,
			"terminatedMethod": null,
			"createBy": "2",
			"createByName": "张三",
			"createTime": "2024-05-19 16:58:26",
			"iterateCount": 1,
			"iterateVersion": [
				3
			],
			"iterate": [
				{
					"version": 3,
					"cycle": 1,
					"terminatedMethod": null,
					"createBy": "2",
					"createByName": "张三",
					"createTime": "2024-05-19 16:58:31",
					"iterateCount": null,
					"iterateVersion": null,
					"iterate": []
				}
			]
		},
		{
			"version": 3,
			"cycle": 1,
			"terminatedMethod": null,
			"createBy": "2",
			"createByName": "张三",
			"createTime": "2024-05-19 16:58:31",
			"iterateCount": 0,
			"iterateVersion": [],
			"iterate": []
		}
	],
	"msg": "success"
}
```

**请求示例**

`/neo/version/iterate?processName=划款&type=2`

**响应示例**

```json
{
	"data": [
		{
			"version": 1,
			"cycle": 1,
			"terminatedMethod": null,
			"createBy": "2",
			"createByName": "张三",
			"createTime": "2024-05-19 15:29:22",
			"iterateCount": 1,
			"iterateVersion": [
				2
			],
			"iterate": [
				{
					"version": 2,
					"cycle": 1,
					"terminatedMethod": null,
					"createBy": "2",
					"createByName": "张三",
					"createTime": "2024-05-19 16:58:26",
					"iterateCount": 1,
					"iterateVersion": [
						3
					],
					"iterate": [
						{
							"version": 3,
							"cycle": 1,
							"terminatedMethod": null,
							"createBy": "2",
							"createByName": "张三",
							"createTime": "2024-05-19 16:58:31",
							"iterateCount": 0,
							"iterateVersion": [],
							"iterate": []
						}
					]
				}
			]
		}
	],
	"msg": "success"
}
```

**请求示例**

`/neo/version/iterate?processName=划款&type=3`

**响应示例**

```json
{
	"data": {
		"nodes": [
			{
				"version": 1,
				"cycle": 1,
				"terminatedMethod": null,
				"createBy": "2",
				"createByName": "张三",
				"createTime": "2024-05-19 15:29:22",
				"iterateCount": 1,
				"iterateVersion": [
					2
				],
				"iterate": []
			},
			{
				"version": 2,
				"cycle": 1,
				"terminatedMethod": null,
				"createBy": "2",
				"createByName": "张三",
				"createTime": "2024-05-19 16:58:26",
				"iterateCount": 1,
				"iterateVersion": [
					3
				],
				"iterate": []
			},
			{
				"version": 3,
				"cycle": 1,
				"terminatedMethod": null,
				"createBy": "2",
				"createByName": "张三",
				"createTime": "2024-05-19 16:58:31",
				"iterateCount": 0,
				"iterateVersion": [],
				"iterate": []
			}
		],
		"edges": [
			{
				"startNode": "1",
				"endNode": "2",
				"condition": null,
				"startLocation": null,
				"endLocation": null
			},
			{
				"startNode": "2",
				"endNode": "3",
				"condition": null,
				"startLocation": null,
				"endLocation": null
			}
		]
	},
	"msg": "success"
}
```

#### 创建版本时获取候选人选择列表

**请求地址**：${neo.prefix:/neo}/version/candidate

**请求方式**：get

**请求参数**

null

**响应参数**

根据`UserChoose.getCandidateList()`具体实现决定

#### 创建流程版本模型

**请求地址**：${neo.prefix:/neo}/version/create

**请求方式**：post

**请求参数**

| 名称               | 类型     | 必填  | 含义          | 备注                                 |
| ---------------- | ------ | --- | ----------- | ---------------------------------- |
| processName      | string | Y   | 流程名称        |                                    |
| iterateFrom      | number | N   | 迭代自版本号      | 必须是已存在的版本                          |
| cycle            | number | N   | 拒绝后退回至发起的次数 | 默认 0                               |
| terminatedMethod | string | N   | 终止方法        |                                    |
| createBy         | string | N   | 创建人标识       | 根据配置NeoFlowConfig.baseUserChoose决定 |
| createByName     | string | N   | 创建人名称       | 根据配置NeoFlowConfig.baseUserChoose决定 |
| nodes            | array  | Y   | 流程模型节点       |                                    |
| edges            | array  | Y   | 流程模型边       |                                    |

<font color=red>nodes</font>

| 名称                     | 类型      | 必填  | 含义           | 备注                                           |
| ---------------------- | ------- | --- | ------------ | -------------------------------------------- |
| nodeUid                | string  | Y   | uid          | 一次创建中不能重复                                    |
| name                   | sting   | Y   | 节点名称         |                                              |
| identity               | string  | N   | 节点标识         |                                              |
| operationType          | number  | Y   | 节点操作类型       |                                              |
| operationCandidateInfo | array   | N   | 节点操作候选人      |                                              |
| operationMethod        | string  | N   | 节点操作方法       |                                              |
| onlyPassExecute        | boolean | N   | 是否只同意通过才执行方法 | 默认 true                                      |
| autoInterval           | number  | N   | 自动执行间隔       | >=0,，只精确到日期（x 天后，x = 0 立即自动执行），有值将忽略操作类型和候选人 |
| defaultPassCondition   | number  | N   | 默认通过时的跳转条件   | 自动执行的中间节点默认通过跳转条件不能为空                        |
| location               | number  | Y   | 节点位置         | 1-开始，2-中间，3-完成，4-终止                          |
| x                      | number  | N   | 展示位置的x坐标     |                                              |
| y                      | number  | N   | 展示位置的y坐标     |                                              |

<font color=red>edges</font>

| 名称            | 类型             | 必填  | 含义        | 备注  |
| ------------- | -------------- | --- | --------- | --- |
| startNode     | string         | Y   | 开始节点uid   |     |
| endNode       | string         | Y   | 结束节点uid   |     |
| condition     | number         | Y   | 条件        |     |
| startLocation | array\<number> | N   | 展示位置的起始坐标 |     |
| endLocation   | array\<number> | N   | 展示位置的结束坐标 |     |

**响应参数**

null

**请求示例**

`/neo/version/create`

```json
{
    "processName": "划款",
    "iterateFrom": 1,
    "cycle": 1,
    "createBy": "2",
    "createByName": "张三",
    "nodes": [
        {
            "nodeUid": "6",
            "name": "完成",
            "identity": "Complete",
            "operationType": 0,
            "operationCandidateInfo": null,
            "operationMethod": "",
            "onlyPassExecute": true,
            "autoInterval": 0,
            "location": 3,
            "x": 1.1,
            "y": 1.1
        },
        {
            "nodeUid": "7",
            "name": "终止",
            "identity": "Terminate",
            "operationType": 0,
            "operationCandidateInfo": null,
            "operationMethod": "",
            "onlyPassExecute": true,
            "autoInterval": 0,
            "location": 4,
            "x": 1.1,
            "y": 1.1
        },
        {
            "nodeUid": "1",
            "name": "发起",
            "identity": "Start",
            "operationType": 0,
            "operationCandidateInfo": null,
            "operationMethod": "begin",
            "onlyPassExecute": true,
            "autoInterval": 0,
            "location": 1,
            "x": 1.1,
            "y": 1.1
        },
        {
            "nodeUid": "5",
            "name": "中间节点4",
            "identity": "Middle4",
            "operationType": 2,
            "operationCandidateInfo": [
                {
                    "id": "2",
                    "name": "张三"
                }
            ],
            "operationMethod": "verify",
            "onlyPassExecute": true,
            "autoInterval": null,
            "location": 2,
            "x": 1.1,
            "y": 1.1
        },
        {
            "nodeUid": "3",
            "name": "中间节点2",
            "identity": "Middle2",
            "operationType": 2,
            "operationCandidateInfo": [
                {
                    "id": "2",
                    "name": "张三"
                }
            ],
            "operationMethod": "verify",
            "onlyPassExecute": true,
            "autoInterval": null,
            "location": 2,
            "x": 1.1,
            "y": 1.1
        },
        {
            "nodeUid": "4",
            "name": "中间节点3",
            "identity": "Middle3",
            "operationType": 2,
            "operationCandidateInfo": [
                {
                    "id": "2",
                    "name": "张三"
                }
            ],
            "operationMethod": "verify",
            "onlyPassExecute": true,
            "autoInterval": null,
            "location": 2,
            "x": 1.1,
            "y": 1.1
        },
        {
            "nodeUid": "2",
            "name": "中间节点1",
            "identity": "Middle1",
            "operationType": 2,
            "operationCandidateInfo": [
                {
                    "id": "2",
                    "name": "张三"
                }
            ],
            "operationMethod": "verify",
            "onlyPassExecute": true,
            "autoInterval": null,
            "location": 2,
            "x": 1.1,
            "y": 1.1
        }
    ],
    "edges": [
        {
            "startNode": "2",
            "endNode": "7",
            "condition": 2,
            "startLocation": [
                1.1,
                1.1
            ],
            "endLocation": [
                1.1,
                1.1
            ]
        },
        {
            "startNode": "4",
            "endNode": "7",
            "condition": 2,
            "startLocation": [
                1.1,
                1.1
            ],
            "endLocation": [
                1.1,
                1.1
            ]
        },
        {
            "startNode": "5",
            "endNode": "6",
            "condition": 1,
            "startLocation": [
                1.1,
                1.1
            ],
            "endLocation": [
                1.1,
                1.1
            ]
        },
        {
            "startNode": "3",
            "endNode": "7",
            "condition": 2,
            "startLocation": [
                1.1,
                1.1
            ],
            "endLocation": [
                1.1,
                1.1
            ]
        },
        {
            "startNode": "3",
            "endNode": "4",
            "condition": 1,
            "startLocation": [
                1.1,
                1.1
            ],
            "endLocation": [
                1.1,
                1.1
            ]
        },
        {
            "startNode": "4",
            "endNode": "5",
            "condition": 1,
            "startLocation": [
                1.1,
                1.1
            ],
            "endLocation": [
                1.1,
                1.1
            ]
        },
        {
            "startNode": "1",
            "endNode": "2",
            "condition": 1,
            "startLocation": [
                1.1,
                1.1
            ],
            "endLocation": [
                1.1,
                1.1
            ]
        },
        {
            "startNode": "2",
            "endNode": "3",
            "condition": 1,
            "startLocation": [
                1.1,
                1.1
            ],
            "endLocation": [
                1.1,
                1.1
            ]
        },
        {
            "startNode": "5",
            "endNode": "7",
            "condition": 2,
            "startLocation": [
                1.1,
                1.1
            ],
            "endLocation": [
                1.1,
                1.1
            ]
        }
    ]
}
```

**响应示例**

```json
{
	"data": null,
	"msg": "success"
}
```

### ExecuteController 执行

#### 执行流程

**请求地址**：${neo.prefix:/neo}/execute

**请求方式**：post

**请求参数**

**发起 operationType=1**

| 名称              | 类型     | 必填  | 含义        | 备注                                                |
| --------------- | ------ | --- | --------- | ------------------------------------------------- |
| processName     | string | Y   | 流程名称      |                                                   |
| operationType   | number | Y   | 操作类型      | 1                                                 |
| businessKey     | string | N   | 流程实例业务key | 若不执行节点方法或节点方法不填充表单businessKey，则必填                 |
| condition       | number | N   | 跳转条件      | 若不执行节点方法或节点方法不填充表单跳转条件，则必填（业务约定的通过条件）             |
| operator        | map    | N   | 操作用户信息    | 根据配置NeoFlowConfig.baseUserChoose决定                |
| params          | map    | N   | 节点方法业务参数  | 根据节点模型配置的方法具体实现决定                                 |
| operationRemark | string | N   | 操作备注      |                                                   |
| listData        | string | N   | 流程业务列表数据  | 序列化的业务数据，用于在流程列表查询时带出。建议是不变或随节点变动的数据。为null则不会更新数据 |

<font color=red>operator</font>

| 名称   | 类型     | 必填  | 含义  | 备注  |
| ---- | ------ | --- | --- | --- |
| id   | string | Y   | id  |     |
| name | string | Y   | 名称  |     |

**通过 operationType=2**

| 名称              | 类型     | 必填  | 含义        | 备注                                                |
| --------------- | ------ | --- | --------- | ------------------------------------------------- |
| processName     | string | Y   | 流程名称      |                                                   |
| operationType   | number | Y   | 操作类型      | 2                                                 |
| num             | number | Y   | 实例节点位置    | >=2                                               |
| nodeId          | number | Y   | 节点id      | >=1                                               |
| businessKey     | string | Y   | 流程实例业务key |                                                   |
| version         | number | Y   | 版本        | >=1                                               |
| condition       | number | N   | 跳转条件      | 若不执行节点方法或节点方法不填充表单跳转条件，则必填（业务约定的通过条件）             |
| operator        | map    | N   | 操作用户信息    | 参考 **发起 operationType=1**                         |
| params          | map    | N   | 节点方法业务参数  | 根据节点模型配置的方法具体实现决定                                 |
| operationRemark | string | N   | 操作备注      |                                                   |
| listData        | string | N   | 流程业务列表数据  | 序列化的业务数据，用于在流程列表查询时带出。建议是不变或随节点变动的数据。为null则不会更新数据 |

**拒绝 operationType=3**

| 名称              | 类型     | 必填  | 含义        | 备注                                                |
| --------------- | ------ | --- | --------- | ------------------------------------------------- |
| processName     | string | Y   | 流程名称      |                                                   |
| operationType   | number | Y   | 操作类型      | 3                                                 |
| num             | number | Y   | 实例节点位置    | >=2                                               |
| nodeId          | number | Y   | 节点id      | >=1                                               |
| businessKey     | string | Y   | 流程实例业务key |                                                   |
| version         | number | Y   | 版本        | >=1                                               |
| condition       | number | N   | 跳转条件      | 如果拒绝并不是执行”退回/结束 流程“的操作，而是作为路径筛选条件，则必填             |
| operator        | map    | N   | 操作用户信息    | 参考 **发起 operationType=1**                         |
| params          | map    | N   | 节点方法业务参数  | 根据节点模型配置的方法具体实现决定                                 |
| operationRemark | string | N   | 操作备注      |                                                   |
| listData        | string | N   | 流程业务列表数据  | 序列化的业务数据，用于在流程列表查询时带出。建议是不变或随节点变动的数据。为null则不会更新数据 |

**转发 operationType=4**

| 名称                   | 类型     | 必填  | 含义        | 备注                                                |
| -------------------- | ------ | --- | --------- | ------------------------------------------------- |
| processName          | string | Y   | 流程名称      |                                                   |
| operationType        | number | Y   | 操作类型      | 4                                                 |
| num                  | number | Y   | 实例节点位置    | >=2                                               |
| nodeId               | number | Y   | 节点id      | >=1                                               |
| businessKey          | string | Y   | 流程实例业务key |                                                   |
| version              | number | Y   | 版本        | >=1                                               |
| operator             | map    | N   | 操作用户信息    | 参考 **发起 operationType=1**                         |
| forwardOperationType | number | Y   | 转发类型      | 业务约定的节点操作类型                                       |
| forwardOperator      | array  | Y   | 转发对象      |                                                   |
| operationRemark      | string | N   | 操作备注      |                                                   |
| listData             | string | N   | 流程业务列表数据  | 序列化的业务数据，用于在流程列表查询时带出。建议是不变或随节点变动的数据。为null则不会更新数据 |

<font color=red>forwardOperator</font> 参考 **发起 operationType=1**

**强行终止 operationType=5**

| 名称              | 类型     | 必填  | 含义        | 备注                                                |
| --------------- | ------ | --- | --------- | ------------------------------------------------- |
| processName     | string | Y   | 流程名称      |                                                   |
| operationType   | number | Y   | 操作类型      | 5                                                 |
| num             | number | Y   | 实例节点位置    | >=2                                               |
| nodeId          | number | Y   | 节点id      | >=1                                               |
| businessKey     | string | Y   | 流程实例业务key |                                                   |
| version         | number | Y   | 版本        | >=1                                               |
| operator        | map    | N   | 操作用户信息    | 参考 **发起 operationType=1**                         |
| params          | map    | N   | 节点方法业务参数  | 根据版本配置的终止方法具体实现决定                                 |
| operationRemark | string | N   | 操作备注      |                                                   |
| listData        | string | N   | 流程业务列表数据  | 序列化的业务数据，用于在流程列表查询时带出。建议是不变或随节点变动的数据。为null则不会更新数据 |

**响应参数**

null

**请求示例**

`/neo/execute`

**发起 operationType=1**

```json
{
    "processName": "划款",
    "operationType": 1,
    "operator": {
        "id": "2",
        "name": "张三"
    }
}
```

**同感 operationType=2**

```json
{
    "processName": "划款",
    "version": 2,
    "nodeId": 59,
    "num":4,
    "businessKey": "8d91af28-59fd-4246-96a2-e36dc73f3855",
    "operationType": 2,
    "condition": 1,
    "operator": {
        "id": "2",
        "name": "张三"
    }
}
```

**拒绝 operationType=3**

```json
{
    "processName": "划款",
    "version": 4,
    "nodeId": 128,
    "num":2,
    "businessKey": "1a71addd-5e5c-4dcd-9074-f2aa1f23951a",
    "operationType": 3,
    "condition": 2,
    "operator": {
        "id": "2",
        "name": "张三"
    }
}
```

**转发 operationType=4**

```json
{
	"processName": "划款",
	"version": 4,
	"nodeId": 129,
	"num": 3,
	"businessKey": "1a71addd-5e5c-4dcd-9074-f2aa1f23951a",
	"operationType": 4,
	"operator": {
		"id": "2",
		"name": "张三"
	},
	"forwardOperationType": 2,
	"forwardOperator": [
		{
			"id": "4",
			"name": "李四"
		}
	]
}
```

**强行终止 operationType=5**

```json
{
    "processName": "划款",
    "version": 2,
    "nodeId": 29,
    "num":5,
    "businessKey": "a319c8b2-2914-4743-8ae6-c8b3b3dbe5a7",
    "operationType": 5,
    "operator": {
        "id": "2",
        "name": "张三"
    }
}
```

**响应示例**

```json
{
	"data": null,
	"msg": "success"
}
```

#### 批量执行流程

**请求地址**：${neo.prefix:/neo}/batch_execute

**请求方式**：post

**请求参数**

| 名称  | 类型    | 必填  | 含义  | 备注  |
| --- | ----- | --- | --- | --- |
|     | array |     |     |     |

<font color=red>array</fount> 参考 **执行流程**

**响应参数**

| 名称          | 类型             | 含义   | 备注               |
| ----------- | -------------- | ---- | ---------------- |
| size        | number         | 总数   |                  |
| success     | number         | 成功数  |                  |
| fail        | number         | 失败数  |                  |
| successList | array\<string> | 成功列表 | businessKey      |
| failList    | array\<string> | 失败列表 | businessKey：失败原因 |

**请求示例**

`/neo/batch_execute`

```json
[
	{
		"processName": "划款",
		"version": 4,
		"nodeId": 131,
		"num": 3,
		"businessKey": "ba9ce92f-83d9-4ceb-8f90-4ee68bc5c024",
		"operationType": 2,
		"condition": 3,
		"operator": {
			"id": "2",
			"name": "张三"
		}
	}
]
```

**响应示例**

```json
{
	"data": {
		"size": 1,
		"success": 0,
		"fail": 1,
		"successList": [],
		"failList": [
			"ba9ce92f-83d9-4ceb-8f90-4ee68bc5c024：流程执行失败，未找到当前实例节点"
		]
	},
	"msg": "success"
}
```

#### 流程实例移植版本

**请求地址**：${neo.prefix:/neo}/graft

**请求方式**：post

**请求参数**

| 名称              | 类型      | 必填  | 含义         | 备注                                                               |
| --------------- | ------- | --- | ---------- | ---------------------------------------------------------------- |
| processName     | string  | Y   | 流程名称       |                                                                  |
| version         | number  | Y   | 版本         | >=1                                                              |
| num             | number  | Y   | 实例节点位置     | >=2                                                              |
| nodeId          | number  | Y   | 节点id       |                                                                  |
| businessKey     | string  | Y   | 业务key      |                                                                  |
| graftVersion    | number  | Y   | 移植版本       | >=1 & != version                                                 |
| graftNodeUid    | string  | N   | 移植节点位置     | 为空时，会以当前实例节点对应的模型节点uid去查找，若移植版本模型没有对应节点则异常<br><br>移植节点为发起节点，也会异常 |
| executeMethod   | boolean | Y   | 是否执行当前节点方法 |                                                                  |
| operationRemark | string  | N   | 操作备注       |                                                                  |
| operator        | map     | N   | 操作用户信息     | 根据配置NeoFlowConfig.baseUserChoose决定                               |

<font color=red>operator</font>

| 名称   | 类型     | 必填  | 含义  | 备注  |
| ---- | ------ | --- | --- | --- |
| id   | string | Y   | id  |     |
| name | string | Y   | 名称  |     |

**响应参数**

| 名称          | 类型     | 含义     | 备注  |
| ----------- | ------ | ------ | --- |
| processName | string | 流程名称   |     |
| version     | number | 移植的版本  |     |
| num         | number | 新的节点位置 |     |
| nodeId      | number | 新的节点id |     |
| businessKey | string | 业务key  |     |

**请求示例**

`/neo/graft`

```json
{
    "processName": "划款",
    "version": 1,
    "nodeId": 29,
    "num": 2,
    "businessKey": "07c0819b-bfa5-4530-b8a8-476402afbec5",
    "graftVersion": 2,
    "executeMethod": false,
    "operator": {
        "id": "2",
        "name": "张三"
    }
}
```

**响应示例**

```json
{
	"data": {
		"processName": "划款",
		"version": 2,
		"num": 3,
		"nodeId": 30,
		"businessKey": "07c0819b-bfa5-4530-b8a8-476402afbec5"
	},
	"msg": "success"
}
```

#### 批量移植流程实例版本

**请求地址**：${neo.prefix:/neo}/batch_graft

**请求方式**：post

**请求参数**

| 名称  | 类型    | 必填  | 含义  | 备注  |
| --- | ----- | --- | --- | --- |
|     | array |     |     |     |

<font color=red>array</fount> 参考 **流程实例移植版本**

**响应参数**

| 名称          | 类型             | 含义   | 备注               |
| ----------- | -------------- | ---- | ---------------- |
| size        | number         | 总数   |                  |
| success     | number         | 成功数  |                  |
| fail        | number         | 失败数  |                  |
| successList | array\<string> | 成功列表 | businessKey      |
| failList    | array\<string> | 失败列表 | businessKey：失败原因 |

**请求示例**

`/neo/batch_graft`

```json
[
	{
		"processName": "划款",
		"version": 2,
		"nodeId": 30,
		"num": 3,
		"businessKey": "07c0819b-bfa5-4530-b8a8-476402afbec5",
		"graftVersion": 2,
		"graftNum": 6,
		"executeMethod": true,
		"operator": {
			"id": "2",
			"name": "张三"
		}
	}
]
```

**响应示例**

```json
{
	"data": {
		"size": 1,
		"success": 0,
		"fail": 1,
		"successList": [],
		"failList": [
			"07c0819b-bfa5-4530-b8a8-476402afbec5：不能移植到同一个版本"
		]
	},
	"msg": "success"
}
```


#### 手动执行自动节点

**请求地址**：${neo.prefix:/neo}/auto_execute

**请求方式**：get

**请求参数**

| 名称   | 类型     | 必填  | 含义  | 备注               |
| ---- | ------ | --- | --- | ---------------- |
| date | string | N   | 日期  | yyyy-MM-dd，默认 今日 |

**响应参数**

null

**请求示例**

`/neo/auto_execute?date=2024-05-14`

**响应示例**

```json
{
	"data": null,
	"msg": "success"
}
```

### QueryController 泛用查询

#### 查询当前用户在各流程的待办数量

**请求地址**：${neo.prefix:/neo}/query/pending

**请求方式**：get

**请求参数**

| 名称       | 类型     | 必填  | 含义     | 备注                                 |
| -------- | ------ | --- | ------ | ---------------------------------- |
| name     | string | N   | 流程名称   |                                    |
| version  | number | N   | 流程版本   | >=1                                |
| userId   | string | N   | 当前用户id | 根据配置NeoFlowConfig.baseUserChoose决定 |
| username | string | N   | 当前用户名称 | 根据配置NeoFlowConfig.baseUserChoose决定 |

**响应参数**

| 名称  | 类型    | 含义  | 备注  |
| --- | ----- | --- | --- |
|     | array |     |     |

<font color=red>array</font>

| 名称      | 类型     | 含义   | 备注  |
| ------- | ------ | ---- | --- |
| name    | string | 流程名称 |     |
| count   | number | 待办数量 |     |
| version | array  | 版本信息 |     |

<font color=red>version</font>

| 名称      | 类型     | 含义   | 备注  |
| ------- | ------ | ---- | --- |
| version | number | 版本   |     |
| count   | number | 待办数量 |     |

**请求示例**

`/neo/query/pending?userId=2&username=张三`

**响应示例**

```json
{
	"data": [
		{
			"name": "划款",
			"count": 1,
			"version": [
				{
					"count": 1,
					"version": 2
				}
			]
		}
	],
	"msg": "success"
}
```

#### 查询当前用户 发起/待办/已办 列表

**请求地址**：${neo.prefix:/neo}/query/list

**请求方式**：get

**请求参数**

| 名称             | 类型      | 必填  | 含义          | 备注                                                            |
| -------------- | ------- | --- | ----------- | ------------------------------------------------------------- |
| type           | number  | Y   | 查询类型        | 1-发起，2-待办，3-已办                                                |
| nodeStatus     | number  | N   | 已办列表-已办节点状态 | 2-同意，3-拒绝，4-转发，5-终止                                           |
| instanceStatus | number  | N   | 流程实例当前状态    | 1-进行中，2-通过，3-未通过，4-强行终止                                       |
| name           | string  | N   | 流程名称        |                                                               |
| version        | number  | N   | 流程版本        | >=1                                                           |
| businessKey    | string  | N   | 业务key       |                                                               |
| userId         | string  | N   | 当前用户id      | 根据配置NeoFlowConfig.baseUserChoose决定                            |
| username       | string  | N   | 当前用户名称      | 根据配置NeoFlowConfig.baseUserChoose决定                            |
| desc           | boolean | N   | 是否降序        | 发起：发起时间<br>待办：流程实例更新时间<br>已办：当前用户在该流程实例的最后操作时间<br><br>默认 true |
| pageNumber     | number  | N   | 页码          | 默认 1                                                          |
| pageSize       | number  | N   | 每页显示数量      | 默认 15                                                         |

**响应参数**

| 名称      | 类型    | 含义   | 备注  |
| ------- | ----- | ---- | --- |
| content | array | 分页数据 |     |

<font color=red>content</font>

| 名称            | 类型     | 含义          | 备注                                                                     |                       |
| ------------- | ------ | ----------- | ---------------------------------------------------------------------- | --------------------- |
| name          | string | 流程名称        |                                                                        |                       |
| activeVersion | number | 激活版本        |                                                                        |                       |
| version       | number | 当前实例版本      |                                                                        |                       |
| businessKey   | string | 业务key       |                                                                        |                       |
| initiateTime  | string | 发起时间        |                                                                        |                       |
| updateTime    | string | 实例更新时间      |                                                                        |                       |
| num           | number | 当前实例节点长度    |                                                                        |                       |
| nodeId        | number | 当前实例最后的节点id |                                                                        |                       |
| status        | number | 状态          | 发起列表，当前流程实例状态：1-进行中，2-通过，3-未通过，4-强行终止；  <br>已办列表：null；  <br>待办列表：null. |                       |
| listData      | string | N           | 流程业务列表数据                                                               | 序列化的业务数据，用于在流程列表查询时带出 |
| doneNodes     | array  | 已办列表-已办节点列表 |                                                                        |                       |

<font color=red>doneNodes</font>

| 名称           | 类型     | 含义       | 备注                  |
| ------------ | ------ | -------- | ------------------- |
| num          | number | 已办节点位置   |                     |
| nodeId       | number | 已办节点id   |                     |
| nodeName     | string | 已办节点名称   |                     |
| nodeIdentity | string | 已办节点标识   |                     |
| status       | number | 已办节点状态   | 2-同意，3-拒绝，4-转发，5-终止 |
| graft        | string | 版本移植     |                     |
| doneTime     | string | 已办节点处理时间 |                     |

**请求示例**

`/neo/query/list?type=3&userId=2&username=张三`

**响应示例**

```json
{
	"data": {
		"content": [
			{
				"name": "划款",
				"activeVersion": 1,
				"version": 1,
				"businessKey": "c814097d-127b-4468-aedd-dc7b2245cd35",
				"initiateTime": "2024-05-19 19:52:12",
				"updateTime": "2024-05-19 19:53:08",
				"num": 3,
				"nodeId": 34,
				"status": null,
				"doneNodes": [
					{
						"num": 2,
						"nodeId": 33,
						"nodeName": "中间节点1",
						"nodeIdentity": "Middle1",
						"status": 2,
						"graft": null,
						"doneTime": "2024-05-19 19:53:08"
					}
				]
			},
			{
				"name": "划款",
				"activeVersion": 1,
				"version": 2,
				"businessKey": "07c0819b-bfa5-4530-b8a8-476402afbec5",
				"initiateTime": "2024-05-19 18:19:00",
				"updateTime": "2024-05-19 19:51:50",
				"num": 4,
				"nodeId": 31,
				"status": null,
				"doneNodes": [
					{
						"num": 2,
						"nodeId": 29,
						"nodeName": "中间节点1",
						"nodeIdentity": "Middle1",
						"status": 2,
						"graft": "1-->2",
						"doneTime": "2024-05-19 19:00:13"
					},
					{
						"num": 3,
						"nodeId": 30,
						"nodeName": "中间节点1",
						"nodeIdentity": "Middle1",
						"status": 2,
						"graft": null,
						"doneTime": "2024-05-19 19:51:50"
					}
				]
			}
		],
		"pageable": {
			"pageNumber": 0,
			"pageSize": 15,
			"sort": {
				"empty": false,
				"sorted": true,
				"unsorted": false
			},
			"offset": 0,
			"unpaged": false,
			"paged": true
		},
		"last": true,
		"totalPages": 1,
		"totalElements": 2,
		"size": 15,
		"number": 0,
		"sort": {
			"empty": false,
			"sorted": true,
			"unsorted": false
		},
		"numberOfElements": 2,
		"first": true,
		"empty": false
	},
	"msg": "success"
}
```

#### 通过节点 名称/身份 查询节点状态

**请求地址**：${neo.prefix:/neo}/query/identity

**请求方式**：get

**请求参数**

| 名称           | 类型      | 必填  | 含义     | 备注                                        |
| ------------ | ------- | --- | ------ | ----------------------------------------- |
| queryType    | number  | Y   | 查询类型   | 1-待办，2-已办                                 |
| nodeStatus   | number  | N   | 已办节点状态 | 2-同意，3-拒绝，4-转发，5-终止                       |
| name         | string  | N   | 流程名称   |                                           |
| version      | number  | N   | 流程版本   | >=1                                       |
| businessKey  | string  | N   | 业务key  |                                           |
| nodeName     | string  | N   | 节点名称   | nodeName、nodeIdentity 不能全为空               |
| nodeIdentity | string  | N   | 节点标识   | nodeName、nodeIdentity 不能全为空               |
| userId       | string  | N   | 当前用户id | 根据配置NeoFlowConfig.baseUserChoose决定        |
| username     | string  | N   | 当前用户名称 | 根据配置NeoFlowConfig.baseUserChoose决定        |
| desc         | boolean |     | 是否降序   | 待办：上一节点结束时间<br>已办：查询节点结束时间<br><br>默认 true |
| pageNumber   | number  | N   | 页码     | 默认 1                                      |
| pageNumber   | number  | N   | 每页显示数量 | 默认 15                                     |

**响应参数**

| 名称      | 类型    | 含义   | 备注  |
| ------- | ----- | ---- | --- |
| content | array | 分页数据 |     |

<font color=red>content</font>

| 名称          | 类型     | 含义     | 备注                            |                       |
| ----------- | ------ | ------ | ----------------------------- | --------------------- |
| name        | string | 流程名称   |                               |                       |
| version     | number | 流程版本   |                               |                       |
| businessKey | string | 业务key  |                               |                       |
| num         | number | 查询节点位置 |                               |                       |
| nodeId      | number | 查询节点id |                               |                       |
| status      | number | 查询节点状态 | 1-待办，2-同意，3-拒绝，4-转发，5-终止      |                       |
| endTime     | string | 结束时间   | 待办：上一节点结束时间;  <br>已办：查询节点结束时间 |                       |
| listData    | string | N      | 流程业务列表数据                      | 序列化的业务数据，用于在流程列表查询时带出 |

**请求示例**

`/neo/query/identity?nodeIdentity=Middle1&queryType=2&userId=2&username=张三`

**响应示例**

```json
{
	"data": {
		"content": [
			{
				"name": "划款",
				"version": 1,
				"businessKey": "c814097d-127b-4468-aedd-dc7b2245cd35",
				"num": 2,
				"nodeId": 33,
				"status": 2,
				"endTime": "2024-05-19 19:53:08"
			},
			{
				"name": "划款",
				"version": 2,
				"businessKey": "07c0819b-bfa5-4530-b8a8-476402afbec5",
				"num": 3,
				"nodeId": 30,
				"status": 2,
				"endTime": "2024-05-19 19:51:50"
			},
			{
				"name": "划款",
				"version": 2,
				"businessKey": "07c0819b-bfa5-4530-b8a8-476402afbec5",
				"num": 2,
				"nodeId": 29,
				"status": 2,
				"endTime": "2024-05-19 19:00:13"
			}
		],
		"pageable": {
			"pageNumber": 0,
			"pageSize": 15,
			"sort": {
				"empty": false,
				"unsorted": false,
				"sorted": true
			},
			"offset": 0,
			"unpaged": false,
			"paged": true
		},
		"last": true,
		"totalElements": 3,
		"totalPages": 1,
		"numberOfElements": 3,
		"first": true,
		"size": 15,
		"number": 0,
		"sort": {
			"empty": false,
			"unsorted": false,
			"sorted": true
		},
		"empty": false
	},
	"msg": "success"
}
```

#### 查询流程实例操作历史

**请求地址**：${neo.prefix:/neo}/query/history

**请求方式**：get

**请求参数**

| 名称          | 类型     | 必填  | 含义    | 备注  |
| ----------- | ------ | --- | ----- | --- |
| businessKey | string | Y   | 业务key |     |
| num         | number | N   | 节点位置  | >=1 |

**响应参数**

| 名称  | 类型    | 含义  | 备注  |
| --- | ----- | --- | --- |
|     | array |     |     |

<font color=red>array</font>

| 名称              | 类型     | 含义           | 备注                       |
| --------------- | ------ | ------------ | ------------------------ |
| nodeName        | string | 节点名称         |                          |
| candidate       | array  | 节点候选人        |                          |
| operator        | map    | 节点操作人        |                          |
| operationResult | number | 操作结果         | 1-待办，2-同意，3-拒绝，4-转发，5-终止 |
| graft           | string | 版本移植         |                          |
| operationRemark | string | 操作备注         |                          |
| beginTime       | string | 节点开始时间       |                          |
| endTime         | string | 节点结束时间       |                          |
| during          | string | 节点持续时间       |                          |
| processDuring   | string | 节点结束时，流程持续时间 |                          |

<font color=red>operator、candidate</font>

| 名称   | 类型     | 含义  | 备注  |
| ---- | ------ | --- | --- |
| id   | string | id  |     |
| name | string | 名称  |     |

**请求示例**

`/neo/query/history?businessKey=42393457-c39d-4b9d-a095-64c2aab9704a`

**响应示例**

```json
{
	"data": [
		{
			"nodeName": "发起",
			"candidate": null,
			"operator": {
				"id": "2",
				"name": "张三"
			},
			"operationResult": 2,
			"graft": null,
			"operationRemark": null,
			"beginTime": "2024-05-19 21:28:46",
			"endTime": "2024-05-19 21:28:46",
			"during": "0S",
			"processDuring": null
		},
		{
			"nodeName": "中间节点1",
			"candidate": [
				{
					"id": "2",
					"name": "张三"
				}
			],
			"operator": {
				"id": "2",
				"name": "张三"
			},
			"operationResult": 2,
			"graft": "1-->2",
			"operationRemark": null,
			"beginTime": "2024-05-19 21:28:47",
			"endTime": "2024-05-19 21:36:45",
			"during": "7M58S",
			"processDuring": "7M58S"
		},
		{
			"nodeName": "中间节点1",
			"candidate": [
				{
					"id": "2",
					"name": "张三"
				}
			],
			"operator": {
				"id": "2",
				"name": "张三"
			},
			"operationResult": 2,
			"graft": null,
			"operationRemark": null,
			"beginTime": "2024-05-19 21:36:45",
			"endTime": "2024-05-19 21:43:49",
			"during": "7M4S",
			"processDuring": "15M2S"
		},
		{
			"nodeName": "中间节点2",
			"candidate": [
				{
					"id": "2",
					"name": "张三"
				}
			],
			"operator": {
				"id": null,
				"name": null
			},
			"operationResult": 1,
			"graft": null,
			"operationRemark": null,
			"beginTime": "2024-05-19 21:43:49",
			"endTime": null,
			"during": null,
			"processDuring": null
		}
	],
	"msg": "success"
}
```

### CacheController 缓存

#### 缓存统计

**请求地址**：${neo.prefix:/neo}/cache/statistics

**请求方式**：get

**请求参数**

null

**响应参数**

自定缓存实现情况下，以实际方法实现为准。这里以默认实现为准

| 名称  | 类型    | 含义   | 备注  |
| --- | ----- | ---- | --- |
|     | array | 统计信息 |     |

<font color=red>array</font>

| 名称                 | 类型             | 含义        | 备注  |
| ------------------ | -------------- | --------- | --- |
| cacheType          | map            | 缓存类型      |     |
| estimatedSize      | number         | 估计数量      |     |
| requestCount       | number         | 请求次数      |     |
| hitRate            | number         | 命中率       |     |
| missRate           | number         | 未命中率      |     |
| loadSuccessCount   | number         | 加载新值成功的次数 |     |
| loadFailureCount   | number         | 加载新值失败的次数 |     |
| averageLoadPenalty | number         | 加载操作的平均时间 | ms  |
| evictionCount      | number         | 驱逐缓存数量    |     |
| estimatedKeys      | array\<string> | 估计存在的key  |     |

<font color=red>cacheType</font>

| 名称          | 类型     | 含义             | 备注  |
| ----------- | ------ | -------------- | --- |
| type        | string | 类型             |     |
| info        | string | 信息             |     |
| defaultRule | string | 默认缓存实现的缓存key规则 |     |
| customRule  | string | 自定义实现的缓存key规则  |     |

**请求示例**

`/neo/cache/statistics`

**响应示例**

```json
{
	"data": [
		{
			"cacheType": {
				"type": "a_p_n",
				"info": "所有流程名称",
				"defaultRule": "all",
				"customRule": "a_p_n+分隔符+all"
			},
			"estimatedSize": 0,
			"requestCount": 0,
			"hitRate": 1,
			"missRate": 0,
			"loadSuccessCount": 0,
			"loadFailureCount": 0,
			"averageLoadPenalty": 0,
			"evictionCount": 0,
			"estimatedKeys": []
		},
		{
			"cacheType": {
				"type": "v_m",
				"info": "流程版本模型",
				"defaultRule": "流程名称+分隔符+版本号",
				"customRule": "v_m+分隔符+流程名称+分隔符+版本号"
			},
			"estimatedSize": 0,
			"requestCount": 0,
			"hitRate": 1,
			"missRate": 0,
			"loadSuccessCount": 0,
			"loadFailureCount": 0,
			"averageLoadPenalty": 0,
			"evictionCount": 0,
			"estimatedKeys": []
		},
		{
			"cacheType": {
				"type": "p_a_v_h",
				"info": "流程版本启用历史",
				"defaultRule": "流程名称",
				"customRule": "p_a_v_h+分隔符+流程名称"
			},
			"estimatedSize": 0,
			"requestCount": 0,
			"hitRate": 1,
			"missRate": 0,
			"loadSuccessCount": 0,
			"loadFailureCount": 0,
			"averageLoadPenalty": 0,
			"evictionCount": 0,
			"estimatedKeys": []
		},
		{
			"cacheType": {
				"type": "m_c_t",
				"info": "中间节点能否拒绝",
				"defaultRule": "流程名称+分隔符+版本+分隔符+当前节点对应模型节点uid",
				"customRule": "m_c_t+分隔符+流程名称+分隔符+版本+分隔符+当前节点对应模型节点uid"
			},
			"estimatedSize": 0,
			"requestCount": 0,
			"hitRate": 1,
			"missRate": 0,
			"loadSuccessCount": 0,
			"loadFailureCount": 0,
			"averageLoadPenalty": 0,
			"evictionCount": 0,
			"estimatedKeys": []
		},
		{
			"cacheType": {
				"type": "n_m_n",
				"info": "下一个模型节点",
				"defaultRule": "流程名称+分隔符+版本+分隔符+当前模型节点uid+分隔符+跳转条件",
				"customRule": "n_m_n+分隔符+流程名称+分隔符+版本+分隔符+当前模型节点uid+分隔符+跳转条件"
			},
			"estimatedSize": 1,
			"requestCount": 1,
			"hitRate": 0,
			"missRate": 1,
			"loadSuccessCount": 0,
			"loadFailureCount": 0,
			"averageLoadPenalty": 0,
			"evictionCount": 0,
			"estimatedKeys": [
				"划款:2:2:1"
			]
		},
		{
			"cacheType": {
				"type": "i_b_t",
				"info": "流程实例开始时间",
				"defaultRule": "业务key",
				"customRule": "i_b_t+分隔符+业务key"
			},
			"estimatedSize": 1,
			"requestCount": 1,
			"hitRate": 0,
			"missRate": 1,
			"loadSuccessCount": 0,
			"loadFailureCount": 0,
			"averageLoadPenalty": 0,
			"evictionCount": 0,
			"estimatedKeys": [
				"42393457-c39d-4b9d-a095-64c2aab9704a"
			]
		},
		{
			"cacheType": {
				"type": "i_i_n",
				"info": "流程实例发起节点",
				"defaultRule": "业务key",
				"customRule": "i_i_n+分隔符+业务key"
			},
			"estimatedSize": 0,
			"requestCount": 0,
			"hitRate": 1,
			"missRate": 0,
			"loadSuccessCount": 0,
			"loadFailureCount": 0,
			"averageLoadPenalty": 0,
			"evictionCount": 0,
			"estimatedKeys": []
		},
		{
			"cacheType": {
				"type": "i_o_h",
				"info": "流程实例操作历史",
				"defaultRule": "业务key 或 业务key+分隔符+节点位置",
				"customRule": "i_o_h+分隔符+业务key 或 i_o_h+分隔符+业务key+分隔符+节点位置"
			},
			"estimatedSize": 1,
			"requestCount": 2,
			"hitRate": 0,
			"missRate": 1,
			"loadSuccessCount": 0,
			"loadFailureCount": 0,
			"averageLoadPenalty": 0,
			"evictionCount": 0,
			"estimatedKeys": [
				"42393457-c39d-4b9d-a095-64c2aab9704a"
			]
		},
		{
			"cacheType": {
				"type": "c_s",
				"info": "缓存统计",
				"defaultRule": "all",
				"customRule": ""
			},
			"estimatedSize": 0,
			"requestCount": 1,
			"hitRate": 0,
			"missRate": 1,
			"loadSuccessCount": 0,
			"loadFailureCount": 0,
			"averageLoadPenalty": 0,
			"evictionCount": 0,
			"estimatedKeys": [
				"all"
			]
		},
		{
			"cacheType": {
				"type": "f_i_e",
				"info": "流程实例是否存在",
				"defaultRule": "业务key",
				"customRule": "f_i_e+分隔符+业务key"
			},
			"estimatedSize": 0,
			"requestCount": 0,
			"hitRate": 1,
			"missRate": 0,
			"loadSuccessCount": 0,
			"loadFailureCount": 0,
			"averageLoadPenalty": 0,
			"evictionCount": 0,
			"estimatedKeys": []
		},
		{
			"cacheType": {
				"type": "m_t_n",
				"info": "终止节点模型",
				"defaultRule": "流程名称+分隔符+版本",
				"customRule": "m_t_n+分隔符+流程名称+分隔符+版本"
			},
			"estimatedSize": 0,
			"requestCount": 0,
			"hitRate": 1,
			"missRate": 0,
			"loadSuccessCount": 0,
			"loadFailureCount": 0,
			"averageLoadPenalty": 0,
			"evictionCount": 0,
			"estimatedKeys": []
		},
		{
			"cacheType": {
				"type": "v_i_t",
				"info": "流程版本迭代树",
				"defaultRule": "流程名称",
				"customRule": "v_i_t+分隔符+流程名称"
			},
			"estimatedSize": 0,
			"requestCount": 0,
			"hitRate": 1,
			"missRate": 0,
			"loadSuccessCount": 0,
			"loadFailureCount": 0,
			"averageLoadPenalty": 0,
			"evictionCount": 0,
			"estimatedKeys": []
		},
		{
			"cacheType": {
				"type": "f_i_c",
				"info": "流程实例能否退回",
				"defaultRule": "业务key",
				"customRule": "f_i_c+分隔符+业务key"
			},
			"estimatedSize": 0,
			"requestCount": 0,
			"hitRate": 1,
			"missRate": 0,
			"loadSuccessCount": 0,
			"loadFailureCount": 0,
			"averageLoadPenalty": 0,
			"evictionCount": 0,
			"estimatedKeys": []
		},
		{
			"cacheType": {
				"type": "m_n_u",
				"info": "模型节点 of uid",
				"defaultRule": "流程名称+分隔符+版本+分隔符+模型节点uid",
				"customRule": "m_n_u+分隔符+流程名称+分隔符+版本+分隔符+模型节点uid"
			},
			"estimatedSize": 0,
			"requestCount": 0,
			"hitRate": 1,
			"missRate": 0,
			"loadSuccessCount": 0,
			"loadFailureCount": 0,
			"averageLoadPenalty": 0,
			"evictionCount": 0,
			"estimatedKeys": []
		}
	],
	"msg": "success"
}
```
