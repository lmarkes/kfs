/*
 * Copyright (c) 2004, 2005 The National Association of College and University Business Officers,
 * Cornell University, Trustees of Indiana University, Michigan State University Board of Trustees,
 * Trustees of San Joaquin Delta College, University of Hawai'i, The Arizona Board of Regents on
 * behalf of the University of Arizona, and the r*smart group.
 * 
 * Licensed under the Educational Community License Version 1.0 (the "License"); By obtaining,
 * using and/or copying this Original Work, you agree that you have read, understand, and will
 * comply with the terms and conditions of the Educational Community License.
 * 
 * You may obtain a copy of the License at:
 * 
 * http://kualiproject.org/license.html
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.kuali.module.financial.rules;

import org.kuali.KeyConstants;
import org.kuali.PropertyConstants;
import org.kuali.core.bo.AccountingLine;
import org.kuali.core.document.TransactionalDocument;
import org.kuali.core.rule.KualiParameterRule;
import org.kuali.core.util.GlobalVariables;
import org.kuali.core.util.ObjectUtils;
import org.kuali.core.util.SpringServiceLocator;
import org.kuali.module.chart.bo.ObjectCode;

/**
 * This class holds document specific business rules for the Distribution of Income and Expense. It overrides methods in the base
 * rule class to apply specific checks.
 * 
 * @author Kuali Financial Transactions Team (kualidev@oncourse.iu.edu)
 */
public class DistributionOfIncomeAndExpenseDocumentRule extends TransactionalDocumentRuleBase implements
        DistributionOfIncomeAndExpenseDocumentRuleConstants {

    /**
     * @see org.kuali.module.financial.rules.TransactionalDocumentRuleBase#isDebit(org.kuali.core.bo.AccountingLine)
     */
    public boolean isDebit(AccountingLine accountingLine) throws IllegalStateException {
        if (accountingLine.getAmount().isNegative()) {
            throw new IllegalStateException("Negative amounts are not allowed.");
        }
        return isDebitConsideringSection(accountingLine);
    }

    /**
     * @see org.kuali.core.rule.AccountingLineRule#isObjectSubTypeAllowed(org.kuali.core.bo.AccountingLine)
     */
    public boolean isObjectSubTypeAllowed(AccountingLine accountingLine) {
        boolean valid = super.isObjectSubTypeAllowed(accountingLine);
        if (valid) {
            KualiParameterRule rule = getParameterRule(DISTRIBUTION_OF_INCOME_AND_EXPENSE_DOCUMENT_SECURITY_GROUPING,
                    RESTRICTED_SUB_TYPE_GROUP_CODES);
            String objectSubTypeCode = accountingLine.getObjectCode().getFinancialObjectSubTypeCode();

            ObjectCode objectCode = accountingLine.getObjectCode();
            if (ObjectUtils.isNull(objectCode)) {
                accountingLine.refreshReferenceObject(PropertyConstants.OBJECT_CODE);
            }

            String objectSubType = objectCode.getFinancialObjectSubTypeCode();

            valid = rule.failsRule(objectSubTypeCode);

            if (!valid) {
                reportError(PropertyConstants.FINANCIAL_OBJECT_CODE,
                        KeyConstants.DistributionOfIncomeAndExpense.ERROR_DOCUMENT_DI_INVALID_OBJ_SUB_TYPE, new String[] {
                                objectCode.getFinancialObjectCode(), objectSubTypeCode });
            }
        }
        return valid;
    }

    /**
     * adds the following additional balance checks
     * <ol>
     * <li> transactions (income and expense; assets or liabilities) must balance. FIS will check and prompt the initiator if the
     * transaction does not balance
     * <li> the total of the revenue objects - total of the expense objects in the "from" window = the total of the revenue objects -
     * total of the expense objects in the "to" window
     * <ul>
     * 
     * @see org.kuali.module.financial.rules.TransactionalDocumentRuleBase#isDocumentBalanceValid(org.kuali.core.document.TransactionalDocument)
     */
    protected boolean isDocumentBalanceValid(TransactionalDocument transactionalDocument) {
        boolean valid = super.isDocumentBalanceValid(transactionalDocument);
        if (valid) {
            valid = isDocumentBalancedConsideringObjectTypes(transactionalDocument);
        }

        if (valid) {
            valid = isDocumenBalancedConsideringRevenueAndExpenseObjectTypes(transactionalDocument);
        }
        return valid;
    }

    /**
     * 
     * @see org.kuali.core.rule.AccountingLineRule#isObjectTypeAllowed(org.kuali.core.bo.AccountingLine)
     */
    public boolean isObjectTypeAllowed(AccountingLine accountingLine) {
        boolean valid = super.isObjectTypeAllowed(accountingLine);

        if (valid) {
            KualiParameterRule rule = SpringServiceLocator.getKualiConfigurationService().getApplicationParameterRule(
                    DISTRIBUTION_OF_INCOME_AND_EXPENSE_DOCUMENT_SECURITY_GROUPING, RESTRICTED_OBJECT_TYPE_CODES);

            ObjectCode objectCode = accountingLine.getObjectCode();

            String objectTypeCode = objectCode.getFinancialObjectTypeCode();

            valid = rule.failsRule(objectTypeCode);
            if (!valid) {
                // add message
                GlobalVariables.getErrorMap().put(PropertyConstants.FINANCIAL_OBJECT_CODE,
                        KeyConstants.DistributionOfIncomeAndExpense.ERROR_DOCUMENT_DI_INVALID_OBJECT_TYPE_CODE,
                        new String[] { objectCode.getFinancialObjectCode(), objectTypeCode });
            }
        }

        return valid;
    }
}
