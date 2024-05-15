create FULLTEXT index BUSINESS_fullText_oc for ()-[r:BUSINESS]-() on each [r.operationCandidate];
create FULLTEXT index InstanceNode_fullText_oc for (n:InstanceNode) on each [n.operationCandidate];