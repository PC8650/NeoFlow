//FULLTEXT Index
create FULLTEXT index BUSINESS_fullText_oc if not exists for ()-[r:BUSINESS]-() on each [r.operationCandidate];
create FULLTEXT index InstanceNode_fullText_oc if not exists for (n:InstanceNode) on each [n.operationCandidate];