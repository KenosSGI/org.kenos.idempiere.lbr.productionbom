-- I forgot to set the DICTIONARY_ID_COMMENTS System Configurator
-- 18/07/2018 11h19min39s BRT
INSERT INTO AD_Column (AD_Column_ID,Version,Name,Description,Help,AD_Table_ID,ColumnName,FieldLength,IsKey,IsParent,IsMandatory,IsTranslated,IsIdentifier,SeqNo,IsEncrypted,AD_Reference_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,AD_Element_ID,IsUpdateable,IsSelectionColumn,EntityType,IsSyncDatabase,IsAlwaysUpdateable,IsAutocomplete,IsAllowLogging,AD_Column_UU,IsAllowCopy,SeqNoSelection,IsToolbarButton,IsSecure) VALUES (1130424,0,'Business Partner ','Identifies a Business Partner','A Business Partner is anyone with whom you transact.  This can include Vendor, Customer, Employee or Salesperson',53018,'C_BPartner_ID',22,'N','N','N','N','N',0,'N',30,0,0,'Y',TO_TIMESTAMP('2018-07-18 11:19:38','YYYY-MM-DD HH24:MI:SS'),100,TO_TIMESTAMP('2018-07-18 11:19:38','YYYY-MM-DD HH24:MI:SS'),100,187,'N','N','LBRA','N','N','N','Y','86763ab1-4063-4b16-a130-5f4e2d2fb76c','Y',0,'N','N')
;

-- 18/07/2018 11h19min47s BRT
UPDATE AD_Column SET FKConstraintName='CBPartner_PPProductBOM', FKConstraintType='N',Updated=TO_TIMESTAMP('2018-07-18 11:19:47','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Column_ID=1130424
;

-- 18/07/2018 11h19min47s BRT
ALTER TABLE PP_Product_BOM ADD COLUMN C_BPartner_ID NUMERIC(10) DEFAULT NULL 
;

-- 18/07/2018 11h19min48s BRT
ALTER TABLE PP_Product_BOM ADD CONSTRAINT CBPartner_PPProductBOM FOREIGN KEY (C_BPartner_ID) REFERENCES c_bpartner(c_bpartner_id) DEFERRABLE INITIALLY DEFERRED
;

-- 18/07/2018 11h22min55s BRT
INSERT INTO AD_Field (AD_Field_ID,Name,Description,Help,AD_Tab_ID,AD_Column_ID,IsDisplayed,DisplayLength,SeqNo,IsSameLine,IsHeading,IsFieldOnly,IsEncrypted,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadOnly,IsCentrallyMaintained,EntityType,AD_Field_UU,IsDisplayedGrid,SeqNoGrid,ColumnSpan) VALUES (1127484,'Business Partner ','Identifies a Business Partner','A Business Partner is anyone with whom you transact.  This can include Vendor, Customer, Employee or Salesperson',53028,1130424,'Y',22,200,'N','N','N','N',0,0,'Y',TO_TIMESTAMP('2018-07-18 11:22:55','YYYY-MM-DD HH24:MI:SS'),100,TO_TIMESTAMP('2018-07-18 11:22:55','YYYY-MM-DD HH24:MI:SS'),100,'N','Y','LBRA','2f0a4f7f-b612-4c50-b466-84774ae0e7ae','Y',200,2)
;

-- 18/07/2018 11h29min13s BRT
UPDATE AD_Field SET SeqNo=95, AD_Reference_Value_ID=NULL, AD_Val_Rule_ID=NULL, IsToolbarButton=NULL,Updated=TO_TIMESTAMP('2018-07-18 11:29:13','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Field_ID=1127484
;

-- 18/07/2018 11h40min37s BRT
UPDATE AD_Column SET IsUpdateable='Y',Updated=TO_TIMESTAMP('2018-07-18 11:40:37','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Column_ID=1130424
;

-- 18/07/2018 11h54min56s BRT
UPDATE AD_Field SET SeqNo=135, AD_Reference_Value_ID=NULL, AD_Val_Rule_ID=NULL, IsToolbarButton=NULL,Updated=TO_TIMESTAMP('2018-07-18 11:54:56','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Field_ID=1127484
;

-- 18/07/2018 11h54min56s BRT
UPDATE AD_Tab SET DisplayLogic='@IsBOM@=''Y'' AND 1=2' WHERE  AD_Tab_ID=317
;

-- I forgot to set the DICTIONARY_ID_COMMENTS System Configurator
-- 27/07/2018 15h15min34s BRT
INSERT INTO AD_ModelValidator (AD_Client_ID,AD_ModelValidator_ID,AD_Org_ID,Created,CreatedBy,Updated,UpdatedBy,IsActive,Name,Description,EntityType,ModelValidationClass,SeqNo,AD_ModelValidator_UU) VALUES (0,1120018,0,TO_TIMESTAMP('2018-07-27 15:15:34','YYYY-MM-DD HH24:MI:SS'),100,TO_TIMESTAMP('2018-07-27 15:15:34','YYYY-MM-DD HH24:MI:SS'),100,'Y','VLBRProductionBom','Add by plugin org.kenos.idempiere.lbr.productionbom','LBRA','org.kenos.idempiere.lbr.productionbom.validator.VLBRProductionBom',95,'2432b4d8-dc6f-490f-ac71-319e569be70f')
;

SELECT Register_Migration_Script ('201807191800_BOMLiberoOnProductionSimple.sql') FROM DUAL
;
