README

	Este plugin alterar a Produção (Individual) para utilizar a Lista de Material do IDempiere Libero (Tabelas PP_Product_BOM e PP_Product_BOMLine) ao invés da Lista de Material padrão do IDempiere (Tabelas M_Product_BOM e M_Product_BOMLine). Também permite a crição de Lista de Material por Parceiro de Negócio

O que você encontra aqui?
	
	Script

Script

	Execute o Script 201807191800_BOMLiberoOnProductionSimple.sql para:
	
	 - Adicionar o campo Parceiro de Negócio na Janela Lista de Materiais.
	 - Adicionar ao Validador de Modelo o Validator org.kenos.idempiere.lbr.productionbom.validator.VLBRProductionBom
