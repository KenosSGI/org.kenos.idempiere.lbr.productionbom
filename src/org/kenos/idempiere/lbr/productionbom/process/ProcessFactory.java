package org.kenos.idempiere.lbr.productionbom.process;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;

/**
 * 		Callout Factory
 * 
 * 	@author Ricardo Santana (Kenos, www.kenos.com.br)
 * 	@contributor Rogério Feitosa <Kenos, www.kenos.com.br)
 *		@version $Id: ProcessFactory.java, v1.0 2017/09/04 5:06:32 PM, ralexsander Exp $
 */
public class ProcessFactory implements IProcessFactory
{
	public ProcessCall newProcessInstance (String className)
	{
		if ("org.compiere.process.ProductionCreate".equals (className))
			return new ProcProductionCreate();
		else if ("org.kenos.idempiere.lbr.base.process.POGBOMDrop".equals (className))
			return new POGBOMDrop();
		return null;
	}	//	newProcessInstance
}	//	CalloutFactory
