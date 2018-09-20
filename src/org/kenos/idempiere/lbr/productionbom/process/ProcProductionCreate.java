package org.kenos.idempiere.lbr.productionbom.process;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;

import org.compiere.model.I_M_ProductionPlan;
import org.compiere.model.MProductionPlan;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.kenos.idempiere.lbr.productionbom.model.MProduction;


/**
 * 
 * Process to create production lines based on the plans
 * defined for a particular production header
 * @author Paul Bowden
 * @contributor Rogério Feitosa <www.kenos.com.br>
 *
 */
public class ProcProductionCreate extends SvrProcess
{
	
	private int p_M_Production_ID=0;
	private MProduction m_production = null;
	private boolean mustBeStocked = false;  //not used
	private boolean recreate = false;
	private BigDecimal newQty = null;
	//private int p_M_Locator_ID=0;	
	
	protected void prepare()
	{
		
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if ("Recreate".equals(name))
				recreate = "Y".equals(para[i].getParameter());
			else if ("ProductionQty".equals(name))
				newQty  = (BigDecimal) para[i].getParameter();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);		
		}
		
		p_M_Production_ID = getRecord_ID();
		m_production = new MProduction(getCtx(), p_M_Production_ID, get_TrxName());

	}	//prepare

	@Override
	protected String doIt() throws Exception{

		if ( m_production.get_ID() == 0 )
			throw new AdempiereUserError("Could not load production header");

		if ( m_production.isProcessed() )
			return "Already processed";

		return createLines();

	}
	
	private boolean costsOK(int M_Product_ID, int C_BPartner_ID) throws AdempiereUserError {
		// Warning will not work if non-standard costing is used
		String sql = "SELECT ABS(((cc.currentcostprice-(SELECT SUM(c.currentcostprice*bomline.qtybom)"
            + " FROM m_cost c"
            + " INNER JOIN pp_product_bomline bomline ON (c.m_product_id=bomline.m_product_id)"
            + " INNER JOIN pp_product_bom bom ON (bomline.PP_Product_BOM_ID=bom.PP_Product_BOM_ID)"
	        + " INNER JOIN m_costelement ce ON (c.m_costelement_id = ce.m_costelement_id AND ce.costingmethod = 'S')"
            + " WHERE bom.m_product_id = pp.m_product_id AND SYSDATE Between bom.ValidFrom AND NVL (bom.ValidTo, SYSDATE + 1)";
           
			if (C_BPartner_ID > 0)
    			sql = sql + " AND bom.C_BPartner_ID = " + C_BPartner_ID;
			else
				sql = sql + " AND bom.C_BPartner_ID IS NULL";
			
            sql = sql + " ))/cc.currentcostprice))"
            + " FROM m_product pp"
            + " INNER JOIN m_cost cc on (cc.m_product_id=pp.m_product_id)"
            + " INNER JOIN m_costelement ce ON (cc.m_costelement_id=ce.m_costelement_id)"
            + " WHERE cc.currentcostprice > 0 AND pp.M_Product_ID = ? "
            + "AND ce.costingmethod='S'";		
		
		BigDecimal costPercentageDiff = DB.getSQLValueBD(get_TrxName(), sql, M_Product_ID);
		
		if (costPercentageDiff == null)
		{
			// Try do Find Register without Business Partner
			// Warning will not work if non-standard costing is used
			sql = "SELECT ABS(((cc.currentcostprice-(SELECT SUM(c.currentcostprice*bomline.qtybom)"
	            + " FROM m_cost c"
	            + " INNER JOIN pp_product_bomline bomline ON (c.m_product_id=bomline.m_product_id)"
	            + " INNER JOIN pp_product_bom bom ON (bomline.PP_Product_BOM_ID=bom.PP_Product_BOM_ID)"
		        + " INNER JOIN m_costelement ce ON (c.m_costelement_id = ce.m_costelement_id AND ce.costingmethod = 'S')"
	            + " WHERE bom.m_product_id = pp.m_product_id AND SYSDATE Between bom.ValidFrom AND NVL (bom.ValidTo, SYSDATE + 1)";

			sql = sql + " AND bom.C_BPartner_ID IS NULL";
				
            sql = sql + " ))/cc.currentcostprice))"
            + " FROM m_product pp"
            + " INNER JOIN m_cost cc on (cc.m_product_id=pp.m_product_id)"
            + " INNER JOIN m_costelement ce ON (cc.m_costelement_id=ce.m_costelement_id)"
            + " WHERE cc.currentcostprice > 0 AND pp.M_Product_ID = ? "
            + "AND ce.costingmethod='S'";
            
            costPercentageDiff = DB.getSQLValueBD(get_TrxName(), sql, M_Product_ID);
			
            if (costPercentageDiff == null)
            {	
				costPercentageDiff = Env.ZERO;
				String msg = "Could not retrieve costs";
				if (MSysConfig.getBooleanValue(MSysConfig.MFG_ValidateCostsOnCreate, false, getAD_Client_ID())) {
					throw new AdempiereUserError(msg);
				} else {
					log.warning(msg);
				}
            }	
		}
		
		if ( (costPercentageDiff.compareTo(new BigDecimal("0.005")))< 0 )
			return true;
		
		return false;
	}

	protected String createLines() throws Exception {
		
		int created = 0;
		if (!m_production.isUseProductionPlan()) {
			validateEndProduct(m_production.getM_Product_ID(), m_production.getC_BPartner_ID());
			
			if (!recreate && "Y".equalsIgnoreCase(m_production.getIsCreated()))
				throw new AdempiereUserError("Production already created.");
			
			if (newQty != null )
				m_production.setProductionQty(newQty);
			
			m_production.deleteLines(get_TrxName());
			created = m_production.createLines(mustBeStocked);
		} else {
			Query planQuery = new Query(getCtx(), I_M_ProductionPlan.Table_Name, "M_ProductionPlan.M_Production_ID=?", get_TrxName());
			List<MProductionPlan> plans = planQuery.setParameters(m_production.getM_Production_ID()).list();
			for(MProductionPlan plan : plans) {
				validateEndProduct(plan.getM_Product_ID(), m_production.getC_BPartner_ID());
				
				if (!recreate && "Y".equalsIgnoreCase(m_production.getIsCreated()))
					throw new AdempiereUserError("Production already created.");
				
				plan.deleteLines(get_TrxName());
				int n = plan.createLines(mustBeStocked);
				if ( n == 0 ) 
				{return "Failed to create production lines"; }
				created = created + n;
			}
		}
		if ( created == 0 ) 
		{return "Failed to create production lines"; }
		
		
		m_production.setIsCreated("Y");
		m_production.save(get_TrxName());
		StringBuilder msgreturn = new StringBuilder().append(created).append(" production lines were created");
		return msgreturn.toString();
	}

	private void validateEndProduct(int M_Product_ID, int C_BPartner_ID) throws Exception {
		isBom(M_Product_ID, C_BPartner_ID);
		
		if (!costsOK(M_Product_ID, C_BPartner_ID)) {
			String msg = "Excessive difference in standard costs";
			if (MSysConfig.getBooleanValue(MSysConfig.MFG_ValidateCostsDifferenceOnCreate, false, getAD_Client_ID())) {
				throw new AdempiereUserError("Excessive difference in standard costs");
			} else {
				log.warning(msg);
			}
		}
	}
	
	protected void isBom(int M_Product_ID, int C_BPartner_ID) throws Exception
	{
		String bom = DB.getSQLValueString(get_TrxName(), "SELECT isbom FROM M_Product WHERE M_Product_ID = ?", M_Product_ID);
		if ("N".compareTo(bom) == 0)
		{
			throw new AdempiereUserError ("Attempt to create product line for Non Bill Of Materials");
		}
		
		StringBuffer sql = new StringBuffer();
		
		sql.append("SELECT count(ppl.PP_Product_BOMLine_ID) FROM PP_Product_BOMLine ppl ");
		sql.append("INNER JOIN PP_Product_BOM pp ON pp.PP_Product_BOM_ID = ppl.PP_Product_BOM_ID ");
		sql.append("WHERE pp.M_Product_ID = ? AND SYSDATE Between pp.ValidFrom AND NVL (pp.ValidTo, SYSDATE + 1) ");
		
		if (C_BPartner_ID > 0)
			sql.append("AND pp.C_BPartner_ID = " + C_BPartner_ID);
		else
			sql.append("AND pp.C_BPartner_ID IS NULL");
		
		int materials = DB.getSQLValue(get_TrxName(), sql.toString(), M_Product_ID);
		if (materials == 0)
		{
			// Try do Find Register without Business Partner
			sql.append("SELECT count(ppl.PP_Product_BOMLine_ID) FROM PP_Product_BOMLine ppl ");
			sql.append("INNER JOIN PP_Product_BOM pp ON pp.PP_Product_BOM_ID = ppl.PP_Product_BOM_ID ");
			sql.append("WHERE pp.M_Product_ID = ? AND SYSDATE Between pp.ValidFrom AND NVL (pp.ValidTo, SYSDATE + 1) ");
			sql.append("AND pp.C_BPartner_ID IS NULL");
			
			materials = DB.getSQLValue(get_TrxName(), sql.toString(), M_Product_ID);
			
			if (materials == 0)
				throw new AdempiereUserError ("Attempt to create product line for Bill Of Materials with no BOM Products");
		}
	}
}
