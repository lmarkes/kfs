/*
 * Copyright 2008 The Kuali Foundation.
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
package org.kuali.kfs.module.purap.service.impl;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.kuali.kfs.module.purap.businessobject.ElectronicInvoice;
import org.kuali.kfs.module.purap.businessobject.ElectronicInvoiceItem;
import org.kuali.kfs.module.purap.businessobject.ElectronicInvoiceItemMapping;
import org.kuali.kfs.module.purap.businessobject.ElectronicInvoiceOrder;
import org.kuali.kfs.module.purap.businessobject.ElectronicInvoiceRejectItem;
import org.kuali.kfs.module.purap.businessobject.ElectronicInvoiceRejectReason;
import org.kuali.kfs.module.purap.businessobject.ItemType;
import org.kuali.kfs.module.purap.businessobject.PurApItem;
import org.kuali.kfs.module.purap.businessobject.PurchaseOrderItem;
import org.kuali.kfs.module.purap.document.ElectronicInvoiceRejectDocument;
import org.kuali.kfs.module.purap.document.PurchaseOrderDocument;
import org.kuali.kfs.module.purap.util.ElectronicInvoiceUtils;
import org.kuali.kfs.vnd.businessobject.VendorDetail;
import org.kuali.rice.kns.util.GlobalVariables;

public class ElectronicInvoiceOrderHolder {
    
    private final static Logger LOG = Logger.getLogger(ElectronicInvoiceOrderHolder.class);
    
    private ElectronicInvoiceRejectDocument rejectDocument;
    private ElectronicInvoiceOrder invoiceOrder;
    private ElectronicInvoice eInvoice;
    private PurchaseOrderDocument poDocument;
    private Map<String,ElectronicInvoiceItemMapping> itemTypeMappings;
    private Map<String,ItemType> kualiItemTypes;
    
    private List<ElectronicInvoiceItemHolder> items = new ArrayList<ElectronicInvoiceItemHolder>();
    
    private boolean isRejected = false;
    private boolean isRejectDocumentHolder;
    private boolean validateHeader;
    
    public ElectronicInvoiceOrderHolder(ElectronicInvoiceRejectDocument rejectDocument,
                                        Map itemTypeMappings,
                                        Map itemTypes){
        
        if (rejectDocument == null){
            throw new NullPointerException("ElectronicInvoiceRejectDocument should not be null");
        }
        
        this.rejectDocument = rejectDocument;
        this.itemTypeMappings = itemTypeMappings;
        this.poDocument = rejectDocument.getCurrentPurchaseOrderDocument();
        this.kualiItemTypes = itemTypes;
        
        isRejectDocumentHolder = true;
        validateHeader = true;
        
        for (int i = 0; i < rejectDocument.getInvoiceRejectItems().size(); i++) {
            
            ElectronicInvoiceRejectItem invoiceRejectItem = rejectDocument.getInvoiceRejectItems().get(i);
            
            PurApItem poItem = null;
            if (poDocument != null){
                poItem = poDocument.getItemByLineNumber(invoiceRejectItem.getInvoiceReferenceItemLineNumber());
            }
            
            items.add(new ElectronicInvoiceItemHolder(invoiceRejectItem,itemTypeMappings,poItem == null ? null : (PurchaseOrderItem)poItem,this));
        }
    }
    
    public ElectronicInvoiceOrderHolder(ElectronicInvoice eInvoice,
                                        ElectronicInvoiceOrder invoiceOrder,
                                        PurchaseOrderDocument poDocument,
                                        Map itemTypeMappings,
                                        Map itemTypes,
                                        boolean validateHeader){
        
        if (eInvoice == null){
            throw new NullPointerException("ElectronicInvoice should not be null");
        }
        
        if (invoiceOrder == null){
            throw new NullPointerException("ElectronicInvoiceOrder should not be null");
        }
        
        this.eInvoice = eInvoice;
        this.invoiceOrder = invoiceOrder;
        this.itemTypeMappings = itemTypeMappings;
        this.validateHeader = validateHeader;
        this.kualiItemTypes = itemTypes;
        
        this.poDocument = poDocument;
        
        isRejectDocumentHolder = false;
        
        for (int i = 0; i < invoiceOrder.getInvoiceItems().size(); i++) {

            ElectronicInvoiceItem orderItem = invoiceOrder.getInvoiceItems().get(i);
            
            PurApItem poItem = null;
            if (poDocument != null){
                poItem = poDocument.getItemByLineNumber(orderItem.getReferenceLineNumberInteger());
            }
            
            items.add(new ElectronicInvoiceItemHolder(orderItem,itemTypeMappings,poItem == null ? null : (PurchaseOrderItem)poItem,this));
        }
        
    }
    
    public String getFileName() {
        if (isRejectDocumentHolder()){
            return rejectDocument.getInvoiceFileName();
        }else{
            return eInvoice.getFileName();
        }
    }
    
    public String getDunsNumber() {
        if (isRejectDocumentHolder()){
            return rejectDocument.getVendorDunsNumber();
        }else{
            return eInvoice.getDunsNumber();
        }
    }
    
    public String getCustomerNumber() {
        if (isRejectDocumentHolder()){
            return rejectDocument.getInvoiceCustomerNumber();
        }else{
            return eInvoice.getCustomerNumber();
        }
    }
    
    public Integer getVendorHeaderId(){
        if (isRejectDocumentHolder()){
            return rejectDocument.getVendorHeaderGeneratedIdentifier();
        }else{
            return eInvoice.getVendorHeaderID();
        }
    }
    
    public Integer getVendorDetailId(){
        if (isRejectDocumentHolder()){
            return rejectDocument.getVendorDetailAssignedIdentifier();
        }else{
            return eInvoice.getVendorDetailID();
        }
    }
    
    public String getVendorName(){
        if (isRejectDocumentHolder()){
            if (rejectDocument.getVendorDetail() != null){
                return rejectDocument.getVendorDetail().getVendorName();
            }else{
               return StringUtils.EMPTY; 
            }
        }else{
            return eInvoice.getVendorName();
        }
    }
    
    public String getInvoiceNumber(){
        if (isRejectDocumentHolder()){
            return rejectDocument.getInvoiceFileNumber();
        }else{
            return eInvoice.getInvoiceDetailRequestHeader().getInvoiceId();
        }
    }
    
    public Date getInvoiceDate(){
        if (isRejectDocumentHolder()){
            return ElectronicInvoiceUtils.getDate(rejectDocument.getInvoiceFileDate());
        }else{
            return eInvoice.getInvoiceDetailRequestHeader().getInvoiceDate();
        }
    }
    
    public String getInvoiceDateString(){
        if (isRejectDocumentHolder()){
            return rejectDocument.getInvoiceFileDate();
        }else{
            return eInvoice.getInvoiceDetailRequestHeader().getInvoiceDateString();
        }
    }
    
    
    public boolean isInformationOnly(){
        if (isRejectDocumentHolder()){
            return rejectDocument.isInvoiceFileInformationOnlyIndicator();
        }else{
            return eInvoice.getInvoiceDetailRequestHeader().isInformationOnly();
        }
    }
    
    public String getInvoicePurchaseOrderID(){
        if (isRejectDocumentHolder()){
            return rejectDocument.getInvoicePurchaseOrderNumber();
        }else{
            return invoiceOrder.getOrderReferenceOrderID();
        }
    }
    
    public boolean isTaxInLine(){
        if (isRejectDocumentHolder()){
            return rejectDocument.isInvoiceFileTaxInLineIndicator();
        }else{
            return eInvoice.getInvoiceDetailRequestHeader().isTaxInLine();
        }
    }
    
    public BigDecimal getTaxAmount(){
        if (isRejectDocumentHolder()){
            return rejectDocument.getInvoiceItemTaxAmount();
        }else{
            return eInvoice.getInvoiceDetailRequestSummary().getInvoiceTaxAmount();
        }
    }
    
    public boolean isSpecialHandlingInLine(){
        if (isRejectDocumentHolder()){
            return rejectDocument.isInvoiceFileSpecialHandlingInLineIndicator();
        }else{
            return eInvoice.getInvoiceDetailRequestHeader().isSpecialHandlingInLine();
        }
    }
    
    public BigDecimal getSpecialHandlingAmount(){
        if (isRejectDocumentHolder()){
            return rejectDocument.getInvoiceItemSpecialHandlingAmount();
        }else{
            return eInvoice.getInvoiceDetailRequestSummary().getInvoiceSpecialHandlingAmount();
        }
    }
    
    public boolean isShippingInLine(){
        if (isRejectDocumentHolder()){
            return rejectDocument.isInvoiceFileShippingInLineIndicator();
        }else{
            return eInvoice.getInvoiceDetailRequestHeader().isShippingInLine();
        }
    }
    
    public BigDecimal getInvoiceShippingAmount(){
        if (isRejectDocumentHolder()){
            return rejectDocument.getInvoiceItemShippingAmount();
        }else{
            return eInvoice.getInvoiceDetailRequestSummary().getInvoiceShippingAmount();
        }
    }
    
    public boolean isDiscountInLine(){
        if (isRejectDocumentHolder()){
            return rejectDocument.isInvoiceFileDiscountInLineIndicator();
        }else{
            return eInvoice.getInvoiceDetailRequestHeader().isDiscountInLine();
        }
    }
    
    public BigDecimal getInvoiceDiscountAmount(){
        if (isRejectDocumentHolder()){
            return rejectDocument.getInvoiceItemDiscountAmount();
        }else{
            return eInvoice.getInvoiceDetailRequestSummary().getInvoiceDiscountAmount();
        }
    }
    
    public PurchaseOrderDocument getPurchaseOrderDocument(){
        return poDocument;
    }
    
    public ElectronicInvoiceItemHolder[] getItems() {
        if (items != null){
            ElectronicInvoiceItemHolder[] returnItems = new ElectronicInvoiceItemHolder[items.size()];
            items.toArray(returnItems);
            return returnItems;
        }
        return null;
    }
    
    public void addInvoiceHeaderRejectReason(ElectronicInvoiceRejectReason rejectReason){
        addInvoiceHeaderRejectReason(rejectReason,null);
    }
    
    public void addInvoiceHeaderRejectReason(ElectronicInvoiceRejectReason rejectReason,
                                             String fieldName){
        
        LOG.debug("Adding reject reason - " + rejectReason.getInvoiceRejectReasonDescription());
        
        if (isRejectDocumentHolder()){
            rejectDocument.addRejectReason(rejectReason);
            if (fieldName != null){
                GlobalVariables.getErrorMap().putError("vendorDunsNumber", rejectReason.getInvoiceRejectReasonDescription());    
            }
        }else{
            eInvoice.addFileRejectReasonToList(rejectReason);
            eInvoice.setFileRejected(true);
        }
    }
    
    public void addInvoiceOrderRejectReason(ElectronicInvoiceRejectReason rejectReason,
                                            String fieldName){
        
        LOG.debug("Adding reject reason - " + rejectReason.getInvoiceRejectReasonDescription());
        
        if (isRejectDocumentHolder()){
            rejectDocument.addRejectReason(rejectReason);
            if (fieldName != null){
                GlobalVariables.getErrorMap().putError("vendorDunsNumber", null);
            }
        }else{
            invoiceOrder.addRejectReasonToList(rejectReason);
            eInvoice.setFileRejected(true);
        }
    }
    
    public void addInvoiceOrderRejectReason(ElectronicInvoiceRejectReason rejectReason) {
        addInvoiceOrderRejectReason(rejectReason,null);
    }
    
    public boolean isValidateHeaderInformation(){
        return validateHeader;
    }
    
    public boolean isRejectDocumentHolder(){
        return isRejectDocumentHolder;
    }

//    public ElectronicInvoiceItemMapping getItemMapping(String invoiceItemTypeCode){
//        if (itemTypeMappings == null){
//            return null;
//        }else{
//            return itemTypeMappings.get(invoiceItemTypeCode);
//        }
//    }
    
    /*public boolean isItemTypeAvailableInKuali(String invoiceItemTypeCode) {
        if (itemTypeMappings == null) {
            return false;
        }
        else {
            return itemTypeMappings.containsKey(invoiceItemTypeCode);
        }
    }*/
    
    public boolean isItemTypeAvailableInKuali(String invoiceItemTypeCode) {
        if (itemTypeMappings == null) {
            return false;
        }
        else {
            return kualiItemTypes.containsKey(invoiceItemTypeCode);
        }
    }

    
    public boolean isInvoiceRejected() {
        if (isRejectDocumentHolder()) {
            return rejectDocument.getInvoiceRejectReasons() != null && rejectDocument.getInvoiceRejectReasons().size() > 1;
        }
        else {
            return eInvoice.isFileRejected();
        }
    }

    
    /*public String getKualiItemTypeCode(String invoiceItemTypeCode) {
        
        ElectronicInvoiceItemMapping itemMapping = getItemMapping(invoiceItemTypeCode);
        
        if (itemMapping != null) {
            return itemMapping.getItemTypeCode();
        }
        else {
            return null;
        }
    }*/
    
    public String getKualiItemTypeCode(String invoiceItemTypeCode) {
        
        ItemType itemType = kualiItemTypes.get(invoiceItemTypeCode);
        
        if (itemType != null) {
            return itemType.getItemTypeCode();
        }
        else {
            return null;
        }
    }
    
    public ElectronicInvoiceItemMapping[] getInvoiceItemTypeMappings(){
        if (itemTypeMappings != null){
            ElectronicInvoiceItemMapping[] itemMappings = new ElectronicInvoiceItemMapping[itemTypeMappings.size()];
            itemTypeMappings.values().toArray(itemMappings);
            return itemMappings;
        }else{
            return null;
        }
    }
    
    public boolean isInvoiceNumberAcceptIndicatorEnabled(){
        if (isRejectDocumentHolder()) {
            return rejectDocument.isInvoiceNumberAcceptIndicator();
        }else {
            return false;
        }
    }
    
    public ElectronicInvoice getElectronicInvoice(){
        if (isRejectDocumentHolder()){
            throw new UnsupportedOperationException("ElectronicInvoice object not available for ElectronicInvoiceRejectDocument");
        }else{
            return eInvoice;
        }
    }
}
