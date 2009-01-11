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
package org.kuali.kfs.module.purap.document.web.struts;

import java.math.BigDecimal;
import java.util.List;

import org.kuali.kfs.integration.purap.CapitalAssetLocation;
import org.kuali.kfs.module.purap.businessobject.PurApItem;
import org.kuali.kfs.module.purap.businessobject.RequisitionAccount;
import org.kuali.kfs.module.purap.businessobject.RequisitionCapitalAssetLocation;
import org.kuali.kfs.module.purap.businessobject.RequisitionItem;
import org.kuali.kfs.module.purap.businessobject.RequisitionItemCapitalAsset;
import org.kuali.kfs.module.purap.document.PurchaseOrderDocument;
import org.kuali.kfs.module.purap.document.RequisitionDocument;
import org.kuali.kfs.module.purap.document.authorization.PurchaseOrderDocumentActionAuthorizer;
import org.kuali.kfs.module.purap.document.authorization.PurchasingDocumentActionAuthorizer;
import org.kuali.kfs.module.purap.document.authorization.RequisitionDocumentActionAuthorizer;
import org.kuali.rice.kns.util.ObjectUtils;
import org.kuali.rice.kns.web.ui.ExtraButton;
import org.kuali.rice.kns.web.ui.HeaderField;
import org.kuali.rice.kns.workflow.service.KualiWorkflowDocument;

/**
 * Struts Action Form for Requisition document.
 */
public class RequisitionForm extends PurchasingFormBase {

    private String shopUrl;
    
    RequisitionDocumentActionAuthorizer auth;

    /**
     * Constructs a RequisitionForm instance and sets up the appropriately casted document.
     */
    public RequisitionForm() {
        super();
        setDocument(new RequisitionDocument());
    }

    public RequisitionDocument getRequisitionDocument() {
        return (RequisitionDocument) getDocument();
    }

    public void setRequisitionDocument(RequisitionDocument requisitionDocument) {
        setDocument(requisitionDocument);
    }
    
    public void populateHeaderFields(KualiWorkflowDocument workflowDocument) {
        super.populateHeaderFields(workflowDocument);
        if (ObjectUtils.isNotNull(this.getRequisitionDocument().getPurapDocumentIdentifier())) {
            getDocInfo().add(new HeaderField("DataDictionary.RequisitionDocument.attributes.purapDocumentIdentifier", ((RequisitionDocument) this.getDocument()).getPurapDocumentIdentifier().toString()));
        }
        else {
            getDocInfo().add(new HeaderField("DataDictionary.RequisitionDocument.attributes.purapDocumentIdentifier", "Not Available"));
        }
        if (ObjectUtils.isNotNull(this.getRequisitionDocument().getStatus())) {
            getDocInfo().add(new HeaderField("DataDictionary.RequisitionDocument.attributes.statusCode", ((RequisitionDocument) this.getDocument()).getStatus().getStatusDescription()));
        }
        else {
            getDocInfo().add(new HeaderField("DataDictionary.RequisitionDocument.attributes.statusCode", "Not Available"));
        }
    }

    @Override
    public Class getCapitalAssetLocationClass() {
        return RequisitionCapitalAssetLocation.class;
    }

    @Override
    public Class getItemCapitalAssetClass() {
        return RequisitionItemCapitalAsset.class;
    }

    /**
     * @see org.kuali.kfs.module.purap.document.web.struts.PurchasingFormBase#setupNewPurchasingItemLine()
     */
    @Override
    public PurApItem setupNewPurchasingItemLine() {
        RequisitionItem ri = new RequisitionItem();
        return ri;
    }

    /**
     * @see org.kuali.kfs.module.purap.document.web.struts.PurchasingFormBase#setupNewPurchasingAccountingLine()
     */
    @Override
    public RequisitionAccount setupNewPurchasingAccountingLine() {
        return new RequisitionAccount();
    }

    /**
     * Override the superclass method to add appropriate buttons for
     * RequisitionDocument.
     * 
     * @see org.kuali.rice.kns.web.struts.form.KualiForm#getExtraButtons()
     */
    @Override
    public List<ExtraButton> getExtraButtons() {
        RequisitionDocument req = (RequisitionDocument) this.getDocument();
        auth = new RequisitionDocumentActionAuthorizer(req, getDocumentActions());

        //add buttons from purapformbase
        return super.getExtraButtons();
    }

    /**
     * @see org.kuali.kfs.module.purap.document.web.struts.PurchasingFormBase#setupNewAccountDistributionAccountingLine()
     */
    @Override
    public RequisitionAccount setupNewAccountDistributionAccountingLine() {
        RequisitionAccount account = setupNewPurchasingAccountingLine();
        account.setAccountLinePercent(new BigDecimal(100));
        return account;
    }

    @Override
    public CapitalAssetLocation setupNewPurchasingCapitalAssetLocationLine() {
        CapitalAssetLocation location = new RequisitionCapitalAssetLocation();
        return location;
    }

    public PurchasingDocumentActionAuthorizer getAuth() {
        return auth;
    }
    public void setAuth(RequisitionDocumentActionAuthorizer auth) {
        this.auth = auth;
    }

    public String getShopUrl() {
        return shopUrl;
    }

    public void setShopUrl(String shopUrl) {
        this.shopUrl = shopUrl;
    }
}
