package org.flowable;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jorge Morando
 */
public class ExternalDelegateApproved implements JavaDelegate {

    Logger logger = LoggerFactory.getLogger(ExternalDelegateApproved.class);

    public void execute(DelegateExecution execution) {
        logger.info("Approved holidays for employee \"{}\"", execution.getVariable("employee"));
    }

}