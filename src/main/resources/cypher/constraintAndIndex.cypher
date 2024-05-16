//UNIQUE Constraint
create constraint Process_unique_name if not exists for (n:Process) require n.name is unique;
create constraint BUSSINESS_unique_key if not exists for ()-[r:BUSINESS]-() require r.key is unique;

//RANGE Index
create index Version_range_version if not exists for (n:Version) on n.version;
create index BUSINESS_range_status if not exists for ()-[r:BUSINESS]-() on r.status;
create index BUSINESS_range_beginTime if not exists for ()-[r:BUSINESS]-() on r.beginTime;
create index BUSINESS_range_endTime if not exists for ()-[r:BUSINESS]-() on r.endTime;
create index InstanceNode_range_status if not exists for (n:InstanceNode) on n.status;
create index InstanceNode_range_beginTime if not exists for (n:InstanceNode) on n.beginTime;
create index InstanceNode_range_endTime if not exists for (n:InstanceNode) on n.endTime;

//TEXT Index
create TEXT index Process_text_name if not exists for (n:Process) on n.name;
create TEXT index ModelNode_text_nodeUid if not exists for (n:modelNode) on n.nodeUid;
create TEXT index InstancelNode_text_identity if not exists for (n:InstancelNode) on n.identity;
create TEXT index InstancelNode_text_operationBy if not exists for (n:InstancelNode) on n.operationBy;