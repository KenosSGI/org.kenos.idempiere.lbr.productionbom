package org.kenos.idempiere.lbr.productionbom.validator;

import org.adempiere.base.IModelValidatorFactory;
import org.compiere.model.ModelValidator;

/**
 * 		Model Validator Factory
 * 
 * 	@author Rogério Feitosa (Kenos, www.kenos.com.br)
 *	@version $Id: ModelValidatorFactory.java, v1.0 2018/01/17 19:08:31, rfeitosa Exp $
 */
public class ModelValidatorFactory implements IModelValidatorFactory
{
	/**
	 * 	Model Validator Factory
	 */
	@Override
	public ModelValidator newModelValidatorInstance(String className)
	{
		if (VLBRProductionBom.class.getName().equals(className))
			return new VLBRProductionBom();
		return null;
	}	//	newModelValidatorInstance
}	//	ModelValidatorFactory
