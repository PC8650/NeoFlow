# NeoFlow
基于Neo4j + Spring Boot 实现的流程引擎

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
![NeoFlow数据建模](https://github.com/PC8650/NeoFlow/assets/70273864/95949a0b-6dcc-4e25-880a-7eec9b98d6e4)


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
