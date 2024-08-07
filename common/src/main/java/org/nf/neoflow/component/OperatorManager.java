package org.nf.neoflow.component;

import org.nf.neoflow.dto.execute.ExecuteForm;

/**
 * 抽象操作管理器，提供operate抽象方法
 * @author PC8650
 */
public abstract class OperatorManager {

    public abstract ExecuteForm operate(ExecuteForm form);

}
