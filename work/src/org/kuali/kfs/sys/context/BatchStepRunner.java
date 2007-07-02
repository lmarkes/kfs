/*
 * Copyright 2006-2007 The Kuali Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.kfs.context;

import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.kuali.core.UserSession;
import org.kuali.core.service.KualiConfigurationService;
import org.kuali.core.util.ErrorMap;
import org.kuali.core.util.GlobalVariables;
import org.kuali.kfs.KFSConstants;
import org.kuali.kfs.batch.Step;
import org.kuali.kfs.util.SpringServiceLocator;

public class BatchStepRunner {
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(BatchStepRunner.class);

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("ERROR: You must pass the name of the step to run on the command line.");
            System.exit(8);
        }
        try {
            Log4jConfigurer.configureLogging();
            SpringContext.initializeApplicationContext();
            String[] stepNames;
            if (args[0].indexOf(",") > 0) {
                stepNames = StringUtils.split(args[0], ",");
            }
            else {
                stepNames = new String[] {args[0]};
            }
            
            String jobName = KFSConstants.BATCH_STEP_RUNNER_JOB_NAME;
            if (args.length >= 2) {
                jobName = args[1];
            }
            for (int i = 0; i < stepNames.length; i++) {
                runStep(stepNames[i], jobName);
            }
            System.exit(0);
        }
        catch (Throwable t) {
            System.err.println("ERROR: Exception caught: ");
            t.printStackTrace(System.err);
            System.exit(8);
        }
    }
    
    private static void runStep(String stepName, String jobName) throws Exception {
        GlobalVariables.setErrorMap(new ErrorMap());
        GlobalVariables.setMessageList(new ArrayList());
        String stepUserParameter = stepName + "_USER";
        KualiConfigurationService configService = SpringServiceLocator.getKualiConfigurationService();
        if (configService.hasApplicationParameter(KFSConstants.ParameterGroups.SYSTEM, stepUserParameter)) {
            GlobalVariables.setUserSession(new UserSession(configService.getApplicationParameterValue(KFSConstants.ParameterGroups.SYSTEM, stepUserParameter)));
        }
        LOG.debug("main() Retrieving step " + stepName);
        Step step = (Step) SpringServiceLocator.getService(stepName);
        String stepRunIndicatorParameter = stepName + "_FLAG";
        if (configService.hasApplicationParameter(KFSConstants.ParameterGroups.SYSTEM, stepRunIndicatorParameter) && !configService.getApplicationParameterIndicator(KFSConstants.ParameterGroups.SYSTEM, stepRunIndicatorParameter)) {
            LOG.info("main() Skipping step " + stepName + " due to flag turned off");
        }
        else {
            LOG.info("main() Running step " + stepName);
            if (step.execute(jobName)) {
                LOG.info("main() Step successful - continue");
            }
            else {
                LOG.info("main() Step successful - stop job");
                System.exit(4);
            }
        }
    }
 }