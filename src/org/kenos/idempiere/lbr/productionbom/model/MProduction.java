package org.kenos.idempiere.lbr.productionbom.model;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MClient;
import org.compiere.model.MLocator;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MProductCategory;
import org.compiere.model.MProductionLine;
import org.compiere.model.MProject;
import org.compiere.model.MProjectLine;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.MWarehouse;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class MProduction extends org.compiere.model.MProduction {	

	/**
	 * 
	 */
	private static final long serialVersionUID = 8047044372956625013L;

	/**
	 * 
	 */
	/** Log								*/
	@SuppressWarnings("unused")
	private static CLogger		m_log = CLogger.getCLogger (MProduction.class);
	private int lineno;
	private int count;
	
	public MProduction(Properties ctx, int M_Production_ID, String trxName) {
		super(ctx, M_Production_ID, trxName);
		if (M_Production_ID == 0) {
			setDocStatus(DOCSTATUS_Drafted);
			setDocAction (DOCACTION_Prepare);
		}
	}

	public MProduction(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	public MProduction( MOrderLine line ) {
		super( line.getCtx(), 0, line.get_TrxName());
		setAD_Client_ID(line.getAD_Client_ID());
		setAD_Org_ID(line.getAD_Org_ID());
		setMovementDate( line.getDatePromised() );
	}

	public MProduction( MProjectLine line ) {
		super( line.getCtx(), 0, line.get_TrxName());
		MProject project = new MProject(line.getCtx(), line.getC_Project_ID(), line.get_TrxName());
		MWarehouse wh = new MWarehouse(line.getCtx(), project.getM_Warehouse_ID(), line.get_TrxName());
		
		MLocator M_Locator = null;
		int M_Locator_ID = 0;

		if (wh != null)
		{
			M_Locator = wh.getDefaultLocator();
			M_Locator_ID = M_Locator.getM_Locator_ID();
		}
		setAD_Client_ID(line.getAD_Client_ID());
		setAD_Org_ID(line.getAD_Org_ID());
		setM_Product_ID(line.getM_Product_ID());
		setProductionQty(line.getPlannedQty());
		setM_Locator_ID(M_Locator_ID);
		setDescription(project.getValue()+"_"+project.getName()+" Line: "+line.getLine()+" (project)");
		setC_Project_ID(line.getC_Project_ID());
		setC_BPartner_ID(project.getC_BPartner_ID());
		setC_Campaign_ID(project.getC_Campaign_ID());
		setAD_OrgTrx_ID(project.getAD_OrgTrx_ID());
		setC_Activity_ID(project.getC_Activity_ID());
		setC_ProjectPhase_ID(line.getC_ProjectPhase_ID());
		setC_ProjectTask_ID(line.getC_ProjectTask_ID());
		setMovementDate( Env.getContextAsDate(p_ctx, "#Date"));
	}
	
	public int createLines(boolean mustBeStocked) {
		return createLines(mustBeStocked, 0);
	}
	
	public int createLines(boolean mustBeStocked, int C_BPartner_ID) {
		
		lineno = 100;

		count = 0;

		// product to be produced
		MProduct finishedProduct = new MProduct(getCtx(), getM_Product_ID(), get_TrxName());
		
		MProductionLine line = new MProductionLine( this );
		line.setLine( lineno );
		line.setM_Product_ID( finishedProduct.get_ID() );
		line.setM_Locator_ID( getM_Locator_ID() );
		line.setMovementQty( getProductionQty());
		line.setPlannedQty(getProductionQty());
		
		line.saveEx();
		count++;
		
		createLines(mustBeStocked, finishedProduct, getProductionQty(), C_BPartner_ID);
		
		return count;
	}

	private int createLines(boolean mustBeStocked, MProduct finishedProduct, BigDecimal requiredQty)
	{
		return createLines(mustBeStocked, finishedProduct, requiredQty, 0);
	}
	private int createLines(boolean mustBeStocked, MProduct finishedProduct, BigDecimal requiredQty, int C_BPartner_ID) {
		
		int defaultLocator = 0;
		
		MLocator finishedLocator = MLocator.get(getCtx(), getM_Locator_ID());
		
		int M_Warehouse_ID = finishedLocator.getM_Warehouse_ID();
		
		int asi = 0;

		// products used in production
		String sql = "";
		
		sql = "SELECT ppl.M_Product_ID, ppl.QtyBOM" + " FROM PP_Product_BOMLine ppl"
				+ " INNER JOIN PP_Product_Bom pp ON pp.PP_Product_Bom_ID = ppl.PP_Product_Bom_ID"
				+ " WHERE pp.M_Product_ID=" + finishedProduct.getM_Product_ID();
				
				if (C_BPartner_ID == 0)
					sql = sql + " AND pp.C_BPartner_ID IS NULL ";
				else
					sql = sql + " AND pp.C_BPartner_ID = " + C_BPartner_ID ;
				
				sql = sql + " AND SYSDATE Between pp.ValidFrom AND NVL(pp.ValidTo, SYSDATE) ORDER BY ppl.Line";

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());

			rs = pstmt.executeQuery();
			
			// Try do Find Register without Business Partner
			if (!rs.isBeforeFirst() )
			{				
				rs = null;
				
				sql = "SELECT ppl.M_Product_ID, ppl.QtyBOM" + " FROM PP_Product_BOMLine ppl"
						+ " INNER JOIN PP_Product_Bom pp ON pp.PP_Product_Bom_ID = ppl.PP_Product_Bom_ID"
						+ " WHERE pp.M_Product_ID=" + finishedProduct.getM_Product_ID()
						+ " AND pp.C_BPartner_ID IS NULL "
						+ " AND SYSDATE Between pp.ValidFrom AND NVL(pp.ValidTo, SYSDATE) ORDER BY ppl.Line";
			
			
			PreparedStatement pstmt2 = DB.prepareStatement(sql, get_TrxName());
			pstmt2 = DB.prepareStatement(sql, get_TrxName());

			rs = pstmt2.executeQuery();
			
			}
			
			while (rs.next()) {
				
				lineno = lineno + 10;
				int BOMProduct_ID = rs.getInt(1);
				BigDecimal BOMQty = rs.getBigDecimal(2);
				BigDecimal BOMMovementQty = BOMQty.multiply(requiredQty);
				
				MProduct bomproduct = new MProduct(Env.getCtx(), BOMProduct_ID, get_TrxName());
				

				if ( bomproduct.isBOM() && bomproduct.isPhantom() )
				{
					createLines(mustBeStocked, bomproduct, BOMMovementQty, C_BPartner_ID);
				}
				else
				{

					defaultLocator = bomproduct.getM_Locator_ID();
					if ( defaultLocator == 0 )
						defaultLocator = getM_Locator_ID();

					if (!bomproduct.isStocked())
					{					
						MProductionLine BOMLine = null;
						BOMLine = new MProductionLine( this );
						BOMLine.setLine( lineno );
						BOMLine.setM_Product_ID( BOMProduct_ID );
						BOMLine.setM_Locator_ID( defaultLocator );  
						BOMLine.setQtyUsed(BOMMovementQty );
						BOMLine.setPlannedQty( BOMMovementQty );
						BOMLine.saveEx(get_TrxName());

						lineno = lineno + 10;
						count++;					
					}
					else if (BOMMovementQty.signum() == 0) 
					{
						MProductionLine BOMLine = null;
						BOMLine = new MProductionLine( this );
						BOMLine.setLine( lineno );
						BOMLine.setM_Product_ID( BOMProduct_ID );
						BOMLine.setM_Locator_ID( defaultLocator );  
						BOMLine.setQtyUsed( BOMMovementQty );
						BOMLine.setPlannedQty( BOMMovementQty );
						BOMLine.saveEx(get_TrxName());

						lineno = lineno + 10;
						count++;
					}
					else
					{

						// BOM stock info
						MStorageOnHand[] storages = null;
						MProduct usedProduct = MProduct.get(getCtx(), BOMProduct_ID);
						defaultLocator = usedProduct.getM_Locator_ID();
						if ( defaultLocator == 0 )
							defaultLocator = getM_Locator_ID();
						if (usedProduct == null || usedProduct.get_ID() == 0)
							return 0;

						MClient client = MClient.get(getCtx());
						MProductCategory pc = MProductCategory.get(getCtx(),
								usedProduct.getM_Product_Category_ID());
						String MMPolicy = pc.getMMPolicy();
						if (MMPolicy == null || MMPolicy.length() == 0) 
						{ 
							MMPolicy = client.getMMPolicy();
						}

						storages = MStorageOnHand.getWarehouse(getCtx(), M_Warehouse_ID, BOMProduct_ID, 0, null,
								MProductCategory.MMPOLICY_FiFo.equals(MMPolicy), true, 0, get_TrxName());

						MProductionLine BOMLine = null;
						int prevLoc = -1;
						int previousAttribSet = -1;
						// Create lines from storage until qty is reached
						for (int sl = 0; sl < storages.length; sl++) {

							BigDecimal lineQty = storages[sl].getQtyOnHand();
							if (lineQty.signum() != 0) {
								if (lineQty.compareTo(BOMMovementQty) > 0)
									lineQty = BOMMovementQty;


								int loc = storages[sl].getM_Locator_ID();
								int slASI = storages[sl].getM_AttributeSetInstance_ID();
								int locAttribSet = new MAttributeSetInstance(getCtx(), asi,
										get_TrxName()).getM_AttributeSet_ID();

								// roll up costing attributes if in the same locator
								if (locAttribSet == 0 && previousAttribSet == 0
										&& prevLoc == loc) {
									BOMLine.setQtyUsed(BOMLine.getQtyUsed()
											.add(lineQty));
									BOMLine.setPlannedQty(BOMLine.getQtyUsed());
									BOMLine.saveEx(get_TrxName());

								}
								// otherwise create new line
								else {
									BOMLine = new MProductionLine( this );
									BOMLine.setLine( lineno );
									BOMLine.setM_Product_ID( BOMProduct_ID );
									BOMLine.setM_Locator_ID( loc );
									BOMLine.setQtyUsed( lineQty);
									BOMLine.setPlannedQty( lineQty);
									if ( slASI != 0 && locAttribSet != 0 )  // ie non costing attribute
										BOMLine.setM_AttributeSetInstance_ID(slASI);
									BOMLine.saveEx(get_TrxName());

									lineno = lineno + 10;
									count++;
								}
								prevLoc = loc;
								previousAttribSet = locAttribSet;
								// enough ?
								BOMMovementQty = BOMMovementQty.subtract(lineQty);
								if (BOMMovementQty.signum() == 0)
									break;
							}
						} // for available storages

						// fallback
						if (BOMMovementQty.signum() != 0 ) {
							if (!mustBeStocked)
							{

								// roll up costing attributes if in the same locator
								if ( previousAttribSet == 0
										&& prevLoc == defaultLocator) {
									BOMLine.setQtyUsed(BOMLine.getQtyUsed()
											.add(BOMMovementQty));
									BOMLine.setPlannedQty(BOMLine.getQtyUsed());
									BOMLine.saveEx(get_TrxName());

								}
								// otherwise create new line
								else {

									BOMLine = new MProductionLine( this );
									BOMLine.setLine( lineno );
									BOMLine.setM_Product_ID( BOMProduct_ID );
									BOMLine.setM_Locator_ID( defaultLocator );  
									BOMLine.setQtyUsed( BOMMovementQty);
									BOMLine.setPlannedQty( BOMMovementQty);
									BOMLine.saveEx(get_TrxName());

									lineno = lineno + 10;
									count++;
								}

							}
							else
							{
								throw new AdempiereUserError("Not enough stock of " + BOMProduct_ID);
							}
						}
					}
				}
			} // for all bom products
		} catch (Exception e) {
			throw new AdempiereException("Failed to create production lines", e);
		}
		finally {
			DB.close(rs, pstmt);
		}

		return count;
	}	
}
