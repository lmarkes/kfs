/*
 * Copyright 2007 The Kuali Foundation.
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
package org.kuali.module.kra.routingform.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kuali.PropertyConstants;
import org.kuali.core.service.BusinessObjectService;
import org.kuali.module.kra.routingform.bo.QuestionType;
import org.kuali.module.kra.routingform.bo.ResearchRiskType;
import org.kuali.module.kra.routingform.bo.RoutingFormQuestion;
import org.kuali.module.kra.routingform.bo.RoutingFormResearchRisk;
import org.kuali.module.kra.routingform.document.RoutingFormDocument;
import org.kuali.module.kra.routingform.service.RoutingFormProjectDetailsService;

public class RoutingFormProjectDetailsServiceImpl implements RoutingFormProjectDetailsService {
    
    private BusinessObjectService businessObjectService;
    
    public void setupOtherProjectDetailsQuestions(RoutingFormDocument routingFormDocument) {
        List<QuestionType> questionTypes = getAllQuestionTypes();
        List<RoutingFormQuestion> questions = new ArrayList<RoutingFormQuestion>();
        for (QuestionType questionType: questionTypes) {
            questions.add(new RoutingFormQuestion(routingFormDocument.getDocumentNumber(), questionType));
        }
        routingFormDocument.setRoutingFormQuestions(questions);
    }
    
    private List<QuestionType> getAllQuestionTypes() {
        Map criteria = new HashMap();
        criteria.put(PropertyConstants.DATA_OBJECT_MAINTENANCE_CODE_ACTIVE_INDICATOR, true);
        List<QuestionType> questionTypes = (List<QuestionType>) businessObjectService.findMatchingOrderBy(
                QuestionType.class, criteria, "questionTypeSortNumber", true);
        return questionTypes;
    }

    /**
     * Sets the businessObjectService attribute value.
     * @param businessObjectService The businessObjectService to set.
     */
    public void setBusinessObjectService(BusinessObjectService businessObjectService) {
        this.businessObjectService = businessObjectService;
    }
}
