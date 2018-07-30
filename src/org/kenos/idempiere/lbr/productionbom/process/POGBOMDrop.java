package org.kenos.idempiere.lbr.productionbom.process;

import org.compiere.model.MBPartner;
import org.compiere.model.MLocator;
import org.compiere.model.MProductionLine;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.kenos.idempiere.lbr.base.model.MLBRProductionGroup;
import org.kenos.idempiere.lbr.base.process.IPOGBOMDrop;
import org.kenos.idempiere.lbr.productionbom.model.MProduction;

public class POGBOMDrop extends SvrProcess implements IPOGBOMDrop
{
	private int p_LBR_ProductionGroup_ID;

	@Override
	protected void prepare()
	{
		p_LBR_ProductionGroup_ID = getRecord_ID();
	}	//	prepare

	@Override
	protected String doIt() throws Exception
	{
		MLBRProductionGroup pg = new MLBRProductionGroup (getCtx(), p_LBR_ProductionGroup_ID, null);
		String xyz = String.valueOf (pg.getLBR_ProductionGroup_ID ());
		MLocator locator = MLocator.get (pg.getCtx(), pg.getM_Warehouse_ID(), "PRD-" + pg.getDocumentNo(), xyz, xyz, xyz);
		locator.set_ValueOfColumn(MBPartner.COLUMNNAME_C_BPartner_ID, pg.getC_BPartner_ID());
		locator.save();
		
		//	Get MProduction Origin
		for (org.compiere.model.MProduction production : pg.getProduction())
		{
			//	Transform into MProduction customizated for this plugin
			MProduction p = (MProduction) production;
			
			//	Drop the BOM
			String result = dropBOM (p, locator.getM_Locator_ID());
			
			//	Error
			if (result != null && !result.isEmpty())
				return result;
		}
		return "@Success@";
	}	//	doIt
	
	/**
	 * 	Drop BOM
	 * @param p_production
	 * @param p_M_Locator_ID
	 * @return
	 */
	public String dropBOM (org.compiere.model.MProduction p_production, MLBRProductionGroup pg)
	{
		p_LBR_ProductionGroup_ID = pg.getLBR_ProductionGroup_ID();
		
		String xyz = String.valueOf (pg.getLBR_ProductionGroup_ID ());
		MLocator locator = MLocator.get (pg.getCtx(), pg.getM_Warehouse_ID(), "PRD-" + pg.getDocumentNo(), xyz, xyz, xyz);
		locator.set_ValueOfColumn(MBPartner.COLUMNNAME_C_BPartner_ID, pg.getC_BPartner_ID());
		locator.save();
		//
		return dropBOM (p_production, locator.getM_Locator_ID());
	}	//	dropBOM
	
	/**
	 * 	Drop BOM
	 * @param p_production
	 * @param p_M_Locator_ID
	 * @return
	 */
	public String dropBOM (org.compiere.model.MProduction production, int p_M_Locator_ID)
	{
		MLBRProductionGroup pg = new MLBRProductionGroup (getCtx(), p_LBR_ProductionGroup_ID, null);
		
		MProduction p_production = new MProduction (Env.getCtx(), production.getM_Production_ID(), production.get_TrxName());
	
		//	Delete before continue
		for (MProductionLine pl : p_production.getLines())
			pl.delete(true);
		
		if (!p_production.getM_Product().isBOM())
			return "@Error@ Produto [" + p_production.getM_Product().getValue() + "] não possuí Lista de Materiais";
		p_production.createLines (false, pg.getC_BPartner_ID());
		p_production.setIsCreated ("Y");
		p_production.save();
		
		//	Update locator
		if (p_M_Locator_ID <= 0)
			return null;
		
		for (MProductionLine pl : p_production.getLines())
		{
			if (!pl.isEndProduct())
			{
				pl.setM_Locator_ID(p_M_Locator_ID);
				pl.save();
			}
		}
		
		return null;
	}	//	dropBOM
	
}	//	POGBOMDrop
