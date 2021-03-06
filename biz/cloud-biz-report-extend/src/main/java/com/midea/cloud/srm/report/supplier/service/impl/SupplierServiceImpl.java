package com.midea.cloud.srm.report.supplier.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.enums.OcrCurrencyType;
import com.midea.cloud.common.utils.AppUserUtil;
import com.midea.cloud.common.utils.DateUtil;
import com.midea.cloud.common.utils.OrgUtils;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.base.organization.entity.OrganizationRelation;
import com.midea.cloud.srm.model.base.purchase.entity.LatestGidailyRate;
import com.midea.cloud.srm.model.base.region.dto.AreaDTO;
import com.midea.cloud.srm.model.base.region.dto.AreaPramDTO;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.report.chart.dto.ChartDTO;
import com.midea.cloud.srm.model.report.chart.dto.SeriesData;
import com.midea.cloud.srm.model.report.supplier.dto.SupplierAnalysisDTO;
import com.midea.cloud.srm.model.report.supplier.dto.SupplierAnalysisDetailDTO;
import com.midea.cloud.srm.model.report.supplier.dto.SupplierCategoryDTO;
import com.midea.cloud.srm.model.report.supplier.dto.SupplierMapDTO;
import com.midea.cloud.srm.model.report.supplier.dto.SupplierMonthsDTO;
import com.midea.cloud.srm.model.report.supplier.dto.SupplierParamDTO;
import com.midea.cloud.srm.model.report.supplier.dto.SupplierPerformanceDTO;
import com.midea.cloud.srm.model.report.supplier.entity.SupplierConfig;
import com.midea.cloud.srm.model.suppliercooperate.order.dto.OrderDetailDTO;
import com.midea.cloud.srm.report.config.service.IConfigService;
import com.midea.cloud.srm.report.supplier.mapper.SupplierMapper;
import com.midea.cloud.srm.report.supplier.service.ISupplierService;

/**
 * 
 * 
 * <pre>
 * ???????????????serviceImpl
 * </pre>
 * 
 * @author  kuangzm
 * @version 1.00.00
 * 
 *<pre>
 * 	????????????
 * 	???????????????:
 *	???????????? 
 *	????????????:2020???11???24??? ??????8:28:32
 *	????????????:
 * </pre>
 */
@Service
public class SupplierServiceImpl  implements ISupplierService {

	@Resource
    private SupplierMapper supplierMapper;

    @Autowired
    private BaseClient baseClient;
    
    
    @Autowired
    private IConfigService iConfigService;
    
    
    
    /*
     * @Description:???????????????
     * @param param
     * @return
     */
    @Override
    public SupplierAnalysisDTO getSupplierAnalysis(SupplierParamDTO param) {
    	SupplierAnalysisDTO result = new SupplierAnalysisDTO();
    	
    	//????????????
    	this.setDateParam(param);
    	
    	LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
    	//????????????id?????????????????????
        List<Organization> permissionNodeResult = baseClient.getFullPathIdByTypeCode(loginAppUser.getUserId());
    	 //?????????????????????????????????
        List<Organization> filterByOrgTypeCode = permissionNodeResult.stream().filter(p -> param.getOrganizationTypeCode().equals(p.getOrganizationTypeCode())).collect(Collectors.toList());
    	//????????????Map
        Map<String, Organization> permissionNodeResultMap = permissionNodeResult.stream().collect(Collectors.toMap(Organization::getFullPathId,
                Function.identity()));
        //????????????????????????
        List<OrganizationRelation> treeNewList = baseClient.allTree();
        List<String> resultFullPathIdList = new ArrayList<>();
        List<String> tempList = null;
    	
        for (Organization organization : filterByOrgTypeCode) {
            String targetParentFullPathId = organization.getFullPathId();
            if (null != param.getFullPathId() && !param.getFullPathId().equals(targetParentFullPathId)) {
        		continue;
        	}
            tempList = OrgUtils.findParentStart(treeNewList,targetParentFullPathId,permissionNodeResultMap);
            resultFullPathIdList.addAll(tempList);
        }
        param.setList(resultFullPathIdList);
        
        
        //??????
        LatestGidailyRate gidailyRate = new LatestGidailyRate();
        gidailyRate.setToCurrency(OcrCurrencyType.CNY.getValue());
        Map<String,List<LatestGidailyRate>> currencyResult = baseClient.getCurrency(gidailyRate);
        
        SupplierConfig config = iConfigService.querySupplierConfig();
        
    	//???????????????
    	result.setSum(getSupplierCount(param));
    	//????????????????????????
    	result.setActiveNum(getActiveCount(param,currencyResult,config));
    	//?????????????????????
    	result.setAddNum(getAddCount(param));
    	//?????????????????????
    	result.setOutNum(getOutCount(param));
    	//?????????????????????
    	result.setCooperation(this.getCooperation(param));
    	//?????????????????????
    	result.setChinaMap(getMap(param));
    	//?????????????????????????????????
    	result.setPurchase(getPurchaseCount(param, currencyResult,config));
    	//????????????????????????
    	result.setPurchaseRank(getPurchaseRank(param, currencyResult));
    	//?????????????????????
    	result.setLevel(this.getPerformance(param));
    	//???????????????????????????
    	result.setCategory(getCategory(param,config));
    	
    	return result;
    }
    
    /*
     * @Description:???????????????
     * @param param
     * @return
     */
    private BigDecimal getSupplierCount(SupplierParamDTO param) {
    	return supplierMapper.getSupplierCount(param);
    }
    
    /*
     * @Description:??????????????????
     * @param param
     * @return
     */
    private BigDecimal getActiveCount(SupplierParamDTO param,Map<String,List<LatestGidailyRate>> currencyResult,SupplierConfig config) {
    	//???????????????????????????
    	List<OrderDetailDTO> list = supplierMapper.getWareHouse(param);
    	Map<Long, BigDecimal> vendorMap = new HashMap<Long,BigDecimal>();
    	if(null != list && list.size() > 0) {
    		BigDecimal sumAmount = null;
    		for (OrderDetailDTO od : list) {
    			if (vendorMap.containsKey(od.getVendorId())) {
    				sumAmount = vendorMap.get(od.getVendorId());
    			} else {
    				sumAmount = BigDecimal.ZERO;
    			}
				
    			if (null != od.getWarehouseReceiptQuantity()) {
    				if (OcrCurrencyType.CNY.getValue().equals(od.getCurrency())) {
    					if (null != od.getReturnQuantity() && null != od.getWarehouseReceiptQuantity()) {
    						sumAmount = sumAmount.add(od.getWarehouseReceiptQuantity().subtract(od.getReturnQuantity())
    								.multiply(od.getUnitPriceContainingTax()));
    					} else if (null != od.getWarehouseReceiptQuantity()) {
    						sumAmount = sumAmount.add(od.getWarehouseReceiptQuantity().multiply(od.getUnitPriceContainingTax()));
    					}
    				} else {
    					BigDecimal rate = currencyResult.get(od.getCurrency()).get(0).getConversionRate();
    					if (null != od.getReturnQuantity() && null != od.getWarehouseReceiptQuantity()) {
    						sumAmount = sumAmount.add(od.getWarehouseReceiptQuantity().subtract(od.getReturnQuantity())
    								.multiply(od.getUnitPriceContainingTax()).multiply(rate));
    					} else if (null != od.getWarehouseReceiptQuantity()) {
    						sumAmount = sumAmount.add(od.getWarehouseReceiptQuantity().multiply(od.getUnitPriceContainingTax()).multiply(rate));
    					}
    				}
    			}
				vendorMap.put(od.getVendorId(), sumAmount);
    		}
    	}
    	int i = 0;
    	Iterator it = vendorMap.keySet().iterator();
    	while(it.hasNext()) {
    		Long key = (Long) it.next();
    		BigDecimal temp = vendorMap.get(key);
    		if (null != temp && temp.doubleValue() >= config.getOrderAmount().multiply(new BigDecimal(10000)).doubleValue()) {
    			i++;
    		}
    	}
    	
    	return new BigDecimal(i);
    }
    
    
    /*
     * @Description:?????????????????????
     * @param param
     * @return
     */
    private BigDecimal getAddCount(SupplierParamDTO param) {
    	return supplierMapper.getAddCount(param);
    }
    
    /*
     * @Description:?????????????????????
     * @param param
     * @return
     */
    private BigDecimal getOutCount(SupplierParamDTO param) {
    	return supplierMapper.getOutCount(param);
    }
    
    private ChartDTO getCooperation(SupplierParamDTO param) {
    	ChartDTO chart = new ChartDTO();
    	List<SupplierMonthsDTO> list = this.supplierMapper.getCompanyMonths(param);
    	if(null != list && list.size() > 0) {
    		int oneYear = 0;
    		int threeYear = 0;
    		int fiveYear = 0;
    		int otherYear = 0;
    		for (SupplierMonthsDTO dto : list) {
    			if(dto.getMonths() <12) {
    				oneYear ++;
    			} else if (dto.getMonths()<36) {
    				threeYear ++;
    			} else if (dto.getMonths() < 60) {
    				fiveYear ++;
    			} else {
    				otherYear ++ ;
    			}
    		}
    		List<SeriesData> seriesData = new ArrayList<SeriesData>();
    		seriesData.add(new SeriesData("1?????????",new BigDecimal(oneYear)));
    		seriesData.add(new SeriesData("1-3???",new BigDecimal(threeYear)));
    		seriesData.add(new SeriesData("3-5???",new BigDecimal(fiveYear)));
    		seriesData.add(new SeriesData("5?????????",new BigDecimal(otherYear)));
    		chart.setSeriesData(seriesData);
    	}
    	return chart;
    }
    
    
    private ChartDTO getMap(SupplierParamDTO param) {
    	ChartDTO chart = new ChartDTO();
    	List<SupplierMapDTO> list = this.supplierMapper.getMap(param);
    	if(null != list && list.size() > 0) {
    		List<SeriesData> seriesData = new ArrayList<SeriesData>();
    		SeriesData series = null;
    		AreaPramDTO areaPramDTO = new AreaPramDTO();
    		areaPramDTO.setQueryType("province");
    		List<AreaDTO> areaList = baseClient.queryRegionById(areaPramDTO);
    		for (SupplierMapDTO dto : list) {
    			series = new SeriesData();
    			for (AreaDTO area:areaList) {
    				if (null != area && null != dto && area.getProvinceId().equals(dto.getCompanyProvince())) {
    					series.setName(area.getProvince().replace("???",""));
    					if ("????????????????????????".equals(area.getProvince())) {
    						series.setName("??????");
    					} else if ("???????????????".equals(area.getProvince())) {
    						series.setName("??????");
    					} else if ("?????????????????????".equals(area.getProvince())) {
    						series.setName("??????");
    					} else if ("??????????????????".equals(area.getProvince())) {
    						series.setName("?????????");
    					} else if ("?????????????????????".equals(area.getProvince())) {
    						series.setName("??????");
    					}
    					break;
    				}
    			}
    			
    			series.setValue(dto.getNum());
    			seriesData.add(series);
    		}
    		chart.setSeriesData(seriesData);
    	}
    	return chart;
    }
    
    
    /*
     * @Description:??????????????????????????????
     * @param param
     * @return
     */
    private ChartDTO getPurchaseCount(SupplierParamDTO param,Map<String,List<LatestGidailyRate>> currencyResult,SupplierConfig config) {
    	ChartDTO chart = new ChartDTO();
    	List<OrderDetailDTO> list = supplierMapper.getWareHouse(param);
    	Map<Long, BigDecimal> vendorMap = new HashMap<Long,BigDecimal>();
    	if(null != list && list.size() > 0) {
    		BigDecimal sumAmount = null;
    		for (OrderDetailDTO od : list) {
    			if (vendorMap.containsKey(od.getVendorId())) {
    				sumAmount = vendorMap.get(od.getVendorId());
    			} else {
    				sumAmount = BigDecimal.ZERO;
    			}
    			
				if (null != od.getWarehouseReceiptQuantity()) {
				   if (OcrCurrencyType.CNY.getValue().equals(od.getCurrency())) {
					   if (null != od.getReturnQuantity() && null != od.getWarehouseReceiptQuantity()) {
						   sumAmount = sumAmount.add(od.getWarehouseReceiptQuantity().subtract(od.getReturnQuantity()).multiply(od.getUnitPriceContainingTax()));
					   } else if (null != od.getWarehouseReceiptQuantity()) {
						   sumAmount = sumAmount.add(od.getWarehouseReceiptQuantity().multiply(od.getUnitPriceContainingTax()));
					   }
				   } else {
					   BigDecimal rate = currencyResult.get(od.getCurrency()).get(0).getConversionRate();
					   if (null != od.getReturnQuantity() && null != od.getWarehouseReceiptQuantity()) {
						   sumAmount = sumAmount.add(od.getWarehouseReceiptQuantity().subtract(od.getReturnQuantity()).multiply(od.getUnitPriceContainingTax()).multiply(rate));
					   } else if (null != od.getWarehouseReceiptQuantity()) {
						   sumAmount = sumAmount.add(od.getWarehouseReceiptQuantity().multiply(od.getUnitPriceContainingTax()).multiply(rate));
					   }
					   
				   }
				}
				vendorMap.put(od.getVendorId(), sumAmount);
    		}
    	}
    	int one = 0;
    	int two = 0;
    	int third = 0;
    	int four = 0;
    	Iterator it = vendorMap.keySet().iterator();
    	while(it.hasNext()) {
    		Long key = (Long) it.next();
    		BigDecimal temp = vendorMap.get(key);
    		if (null != temp && temp.doubleValue() <= new BigDecimal(config.getPurchaseOne()).multiply(new BigDecimal(10000)).doubleValue()) {
    			one++;
    		} else if (null != temp && temp.doubleValue() <= new BigDecimal(config.getPurchaseTwoEnd()).multiply(new BigDecimal(10000)).doubleValue()) {
    			two++;
    		} else if (null != temp && temp.doubleValue() <= new BigDecimal(config.getPurchaseThreeEnd()).multiply(new BigDecimal(10000)).doubleValue()) {
    			third++;
    		} else {
    			four ++ ;
    		}
    	}
    	List<SeriesData> seriesData = new ArrayList<SeriesData>();
		seriesData.add(new SeriesData("?????????X<="+config.getPurchaseOne()+"??????",new BigDecimal(one)));
		seriesData.add(new SeriesData("?????????"+config.getPurchaseTwoStart()+"???<X<="+config.getPurchaseTwoEnd()+"??????",new BigDecimal(two)));
		seriesData.add(new SeriesData("?????????"+config.getPurchaseThreeStart()+"???<X<="+config.getPurchaseThreeEnd()+"??????",new BigDecimal(third)));
		seriesData.add(new SeriesData("?????????X>="+config.getPurchaseFour()+"??????",new BigDecimal(four)));
		chart.setSeriesData(seriesData);
		return chart;
    }
    
    /*
     * @Description:??????????????????????????????
     * @param param
     * @return
     */
    private ChartDTO getPurchaseRank(SupplierParamDTO param,Map<String,List<LatestGidailyRate>> currencyResult) {
    	ChartDTO chart = new ChartDTO();
    	List<OrderDetailDTO> list = supplierMapper.getWareHouse(param);
    	Map<Long, BigDecimal> vendorMap = new HashMap<Long,BigDecimal>();
    	Map<Long,String> nameMap = new HashMap<Long,String>();
    	if(null != list && list.size() > 0) {
    		BigDecimal sumAmount = null;
    		for (OrderDetailDTO od : list) {
    			if (vendorMap.containsKey(od.getVendorId())) {
    				sumAmount = vendorMap.get(od.getVendorId());
    			} else {
    				sumAmount = BigDecimal.ZERO;
    			}
    			
				if (null != od.getWarehouseReceiptQuantity()) {
				   if (OcrCurrencyType.CNY.getValue().equals(od.getCurrency())) {
					   if (null != od.getReturnQuantity() && null != od.getWarehouseReceiptQuantity()) {
						   sumAmount = sumAmount.add(od.getWarehouseReceiptQuantity().subtract(od.getReturnQuantity()).multiply(od.getUnitPriceContainingTax()));
					   } else if (null != od.getWarehouseReceiptQuantity()) {
						   sumAmount = sumAmount.add(od.getWarehouseReceiptQuantity().multiply(od.getUnitPriceContainingTax()));
					   }
				   } else {
					   BigDecimal rate = currencyResult.get(od.getCurrency()).get(0).getConversionRate();
					   if (null != od.getReturnQuantity() && null != od.getWarehouseReceiptQuantity()) {
						   sumAmount = sumAmount.add(od.getWarehouseReceiptQuantity().subtract(od.getReturnQuantity()).multiply(od.getUnitPriceContainingTax()).multiply(rate));
					   } else if (null != od.getWarehouseReceiptQuantity()) {
						   sumAmount = sumAmount.add(od.getWarehouseReceiptQuantity().multiply(od.getUnitPriceContainingTax()).multiply(rate));
					   }
				   }
				}
				vendorMap.put(od.getVendorId(), sumAmount);
				nameMap.put(od.getVendorId(), od.getVendorName());
    		}
    	}
    	
    	
    	List<SeriesData> seriesData = new ArrayList<SeriesData>();
    	SeriesData series = null;
    	Iterator it = vendorMap.keySet().iterator();
    	while(it.hasNext()) {
    		Long key = (Long) it.next();
    		BigDecimal temp = vendorMap.get(key);
    		series = new SeriesData();
    		series.setName(nameMap.get(key));
    		series.setValue(temp);
    		seriesData.add(series);
    	}
    	
    	//??????
    	if (seriesData.size() > 0) {
    		Collections.sort(seriesData, Comparator.comparing(SeriesData::getValue).reversed());
    	}
    	//??????10???
    	if(seriesData.size() > 10) {
    		List<SeriesData> newList = new ArrayList<SeriesData>();
    		for(int i=0;i<10;i++) {
    			newList.add(seriesData.get(i));
    		}
    		chart.setSeriesData(newList);
    	} else {
    		chart.setSeriesData(seriesData);
    	}    	
		return chart;
    }
    
    /*
     * @Description:?????????????????????
     * @param param
     * @return
     */
    private ChartDTO getPerformance(SupplierParamDTO param) {
    	ChartDTO chart = new ChartDTO();
    	List<SupplierPerformanceDTO> list = getNewPerformance(supplierMapper.getPeformance(param));
    	if (null != list && list.size() > 0) {
    		List<SeriesData> seriesData = new ArrayList<SeriesData>();
        	int one = 0;
        	int two = 0;
        	int three = 0;
        	int four = 0;
        	for (SupplierPerformanceDTO dto : list) {
        		if (dto.getScore().doubleValue() >= 90) {
        			one++;
        		} else if (dto.getScore().doubleValue() >= 80) {
        			two++;
        		} else if (dto.getScore().doubleValue() >= 60) {
        			three++;
        		} else {
        			four ++;
        		}
        	}
        	seriesData.add(new SeriesData("???", new BigDecimal(one)));
        	seriesData.add(new SeriesData("???", new BigDecimal(two)));
        	seriesData.add(new SeriesData("??????", new BigDecimal(three)));
        	seriesData.add(new SeriesData("?????????", new BigDecimal(four)));
        	chart.setSeriesData(seriesData);
    	}
    	return chart;
    }
    
    private List<SupplierPerformanceDTO> getNewPerformance(List<SupplierPerformanceDTO> list) {
    	List<SupplierPerformanceDTO> newList = new ArrayList<SupplierPerformanceDTO>();
    	Map<String,Object> map = new HashMap<String,Object>();
    	if (null != list && list.size() >0 ) {
    		for (SupplierPerformanceDTO dto : list) {
    			String key = dto.getFullPathId()+"_"+dto.getCategoryId()+"_"+dto.getCompanyId();
    			if (map.containsKey(key)) {
    				continue;
    			}
    			newList.add(dto);
    		}
    	}
    	return newList;
    }
    
    /*
     * @Description:???????????????????????????
     * @param param
     * @return
     */
    private ChartDTO getCategory(SupplierParamDTO param,SupplierConfig config) {
    	ChartDTO chart = new ChartDTO();
    	List<SupplierCategoryDTO> list = supplierMapper.getCategory(param);
    	if (null != list && list.size() > 0) {
    		List<SeriesData> seriesData = new ArrayList<SeriesData>();
        	int one = 0;
        	int two = 0;
        	int three = 0;
        	int four = 0;
        	for (SupplierCategoryDTO dto : list) {
        		if (dto.getNum().doubleValue() <=config.getCategoryOne()) {
        			one++;
        		} else if (dto.getNum().doubleValue() <=config.getCategoryTwoEnd()) {
        			two++;
        		} else if (dto.getNum().doubleValue() <= config.getCategoryThreeEnd()) {
        			three++;
        		} else {
        			four ++;
        		}
        	}
        	seriesData.add(new SeriesData("??????(X<="+config.getCategoryOne()+")", new BigDecimal(one)));
        	seriesData.add(new SeriesData("??????("+config.getCategoryTwoStart()+"<X<="+config.getCategoryTwoEnd()+")", new BigDecimal(two)));
        	seriesData.add(new SeriesData("??????("+config.getCategoryThreeStart()+"<X<="+config.getCategoryThreeEnd()+")", new BigDecimal(three)));
        	seriesData.add(new SeriesData("??????(X>"+config.getCategoryFour()+")", new BigDecimal(four)));
        	chart.setSeriesData(seriesData);
    	}
    	return chart;
    }
    
    
    
    private void setDateParam(SupplierParamDTO dto) {
    	if(dto.getSeason().equals(0)) {
   		 //??????????????????
           String dateModel1 = "$date-01-01 00:00:00";
           String dateModel2 = "$date-12-31 23:59:59";
           try {
	           	dto.setStartDate(DateUtil.parseDate(dateModel1.replace("$date",String.valueOf(dto.getYear()))));
	           	dto.setEndDate(DateUtil.parseDate(dateModel2.replace("$date",String.valueOf(dto.getYear()))));
	   		} catch (Exception e) {
	   			
	   		}
	   	} else {
	   		dto.setStartDate(DateUtil.getStartDateOfQuarter(dto.getYear(),dto.getSeason()));
	       	dto.setEndDate(DateUtil.getEndDateOfQuarter(dto.getYear(),dto.getSeason()));
	   	}
    }
    
    @Override
    public List<SupplierAnalysisDetailDTO> queryCooperationDetail(SupplierParamDTO param) {
    	//????????????
    	this.setDateParam(param);
    	
    	LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
    	//????????????id?????????????????????
        List<Organization> permissionNodeResult = baseClient.getFullPathIdByTypeCode(loginAppUser.getUserId());
    	 //?????????????????????????????????
        List<Organization> filterByOrgTypeCode = permissionNodeResult.stream().filter(p -> param.getOrganizationTypeCode().equals(p.getOrganizationTypeCode())).collect(Collectors.toList());
    	//????????????Map
        Map<String, Organization> permissionNodeResultMap = permissionNodeResult.stream().collect(Collectors.toMap(Organization::getFullPathId,
                Function.identity()));
        //????????????????????????
        List<OrganizationRelation> treeNewList = baseClient.allTree();
        List<String> resultFullPathIdList = new ArrayList<>();
        List<String> tempList = null;
    	
        for (Organization organization : filterByOrgTypeCode) {
            String targetParentFullPathId = organization.getFullPathId();
            if (null != param.getFullPathId() && !param.getFullPathId().equals(targetParentFullPathId)) {
        		continue;
        	}
            tempList = OrgUtils.findParentStart(treeNewList,targetParentFullPathId,permissionNodeResultMap);
            resultFullPathIdList.addAll(tempList);
        }
        param.setList(resultFullPathIdList);
        
       return this.supplierMapper.queryCooperationDetail(param);
    }
    
    @Override
    public PageInfo<SupplierAnalysisDetailDTO> queryPurchaseAmount(SupplierParamDTO param) {
    	
    	Integer pageSize = param.getPageSize();
    	Integer pageNum = param.getPageNum();
    	param.setPageNum(null);
    	param.setPageSize(null);
    	
    	//????????????
    	this.setDateParam(param);
    	
    	LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
    	//????????????id?????????????????????
        List<Organization> permissionNodeResult = baseClient.getFullPathIdByTypeCode(loginAppUser.getUserId());
    	 //?????????????????????????????????
        List<Organization> filterByOrgTypeCode = permissionNodeResult.stream().filter(p -> param.getOrganizationTypeCode().equals(p.getOrganizationTypeCode())).collect(Collectors.toList());
    	//????????????Map
        Map<String, Organization> permissionNodeResultMap = permissionNodeResult.stream().collect(Collectors.toMap(Organization::getFullPathId,
                Function.identity()));
        //????????????????????????
        List<OrganizationRelation> treeNewList = baseClient.allTree();
        List<String> resultFullPathIdList = new ArrayList<>();
        List<String> tempList = null;
    	
        for (Organization organization : filterByOrgTypeCode) {
            String targetParentFullPathId = organization.getFullPathId();
            if (null != param.getFullPathId() && !param.getFullPathId().equals(targetParentFullPathId)) {
        		continue;
        	}
            tempList = OrgUtils.findParentStart(treeNewList,targetParentFullPathId,permissionNodeResultMap);
            resultFullPathIdList.addAll(tempList);
        }
        param.setList(resultFullPathIdList);
        
        
        //??????
        LatestGidailyRate gidailyRate = new LatestGidailyRate();
        gidailyRate.setToCurrency(OcrCurrencyType.CNY.getValue());
        Map<String,List<LatestGidailyRate>> currencyResult = baseClient.getCurrency(gidailyRate);
        
        
       List<OrderDetailDTO> list  = this.supplierMapper.getWareHouse(param);
       List<SupplierAnalysisDetailDTO> details = null;
       Map<String,SupplierAnalysisDetailDTO> map = new HashMap<String,SupplierAnalysisDetailDTO>();
       if (null != list && list.size() > 0) {
    	   SupplierAnalysisDetailDTO temp = null;
    	   for (OrderDetailDTO od : list) {
    		   BigDecimal sumAmount = BigDecimal.ZERO;
    		   if (null != od.getWarehouseReceiptQuantity()) {
				   if (OcrCurrencyType.CNY.getValue().equals(od.getCurrency())) {
					   if (null != od.getReturnQuantity() && null != od.getWarehouseReceiptQuantity()) {
						   sumAmount = sumAmount.add(od.getWarehouseReceiptQuantity().subtract(od.getReturnQuantity()).multiply(od.getUnitPriceContainingTax()));
					   } else if (null != od.getWarehouseReceiptQuantity()) {
						   sumAmount = sumAmount.add(od.getWarehouseReceiptQuantity().multiply(od.getUnitPriceContainingTax()));
					   }
				   } else {
					   BigDecimal rate = currencyResult.get(od.getCurrency()).get(0).getConversionRate();
					   if (null != od.getReturnQuantity() && null != od.getWarehouseReceiptQuantity()) {
						   sumAmount = sumAmount.add(od.getWarehouseReceiptQuantity().subtract(od.getReturnQuantity()).multiply(od.getUnitPriceContainingTax()).multiply(rate));
					   } else if (null != od.getWarehouseReceiptQuantity()) {
						   sumAmount = sumAmount.add(od.getWarehouseReceiptQuantity().multiply(od.getUnitPriceContainingTax()).multiply(rate));
					   }
				   }
				}
    		   
    		   String key = od.getOrganizationName() +"_"+od.getVendorId() + "_" + od.getCategoryId();
    		   if (map.containsKey(key)) {
    			   temp = map.get(key);
    			   temp.setOrderAmount(temp.getOrderAmount().add(sumAmount));
    		   } else {
    			   temp = new SupplierAnalysisDetailDTO();
    			   temp.setCategoryName(od.getCategoryName());
    			   temp.setOrganizationName(od.getOrganizationName());
    			   temp.setCompanyName(od.getVendorName());
    			   temp.setOrderAmount(sumAmount);
    		   }
    		   map.put(key, temp);
    	   }
    	   if (null != map && map.size() > 0) {
    		   Iterator it = map.keySet().iterator();
    		   details = new ArrayList<SupplierAnalysisDetailDTO>();
    		   while (it.hasNext()) {
    			   String key = (String) it.next();
    			   details.add(map.get(key));
    		   }
    	   }
    	  
    	   if (null != details && details.size() > 0) {
    		   //??????
    		   Collections.sort(details, Comparator.comparing(SupplierAnalysisDetailDTO::getOrderAmount).reversed());
    		   //????????????
    		   int i=1;
    		   for (SupplierAnalysisDetailDTO dto : details) {
    			   dto.setNo(i);
    			   i++;
    		   }
    	   }
       }
    	//????????????
    	return this.getPage(details, pageSize, pageNum);
    }
    
    @Override
    public PageInfo<SupplierAnalysisDetailDTO> queryPeformanceDetail(SupplierParamDTO param) {
    	Integer pageSize = param.getPageSize();
    	Integer pageNum = param.getPageNum();
    	param.setPageNum(null);
    	param.setPageSize(null);
    	//????????????
    	this.setDateParam(param);
    	
    	LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
    	//????????????id?????????????????????
        List<Organization> permissionNodeResult = baseClient.getFullPathIdByTypeCode(loginAppUser.getUserId());
    	 //?????????????????????????????????
        List<Organization> filterByOrgTypeCode = permissionNodeResult.stream().filter(p -> param.getOrganizationTypeCode().equals(p.getOrganizationTypeCode())).collect(Collectors.toList());
    	//????????????Map
        Map<String, Organization> permissionNodeResultMap = permissionNodeResult.stream().collect(Collectors.toMap(Organization::getFullPathId,
                Function.identity()));
        //????????????????????????
        List<OrganizationRelation> treeNewList = baseClient.allTree();
        List<String> resultFullPathIdList = new ArrayList<>();
        List<String> tempList = null;
    	
        for (Organization organization : filterByOrgTypeCode) {
            String targetParentFullPathId = organization.getFullPathId();
            if (null != param.getFullPathId() && !param.getFullPathId().equals(targetParentFullPathId)) {
        		continue;
        	}
            tempList = OrgUtils.findParentStart(treeNewList,targetParentFullPathId,permissionNodeResultMap);
            resultFullPathIdList.addAll(tempList);
        }
        param.setList(resultFullPathIdList);
        
        List<SupplierAnalysisDetailDTO> list  = this.supplierMapper.queryPeformanceDetail(param);
        
        if (null != list && list.size() > 0) {
 		   //??????
 		   Collections.sort(list, Comparator.comparing(SupplierAnalysisDetailDTO::getScore).reversed());
 		   //????????????
 		   int i=1;
 		   for (SupplierAnalysisDetailDTO dto : list) {
 		   		//?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
			   //?????????>=90???
			   //?????????>=80,<90;
			   //????????????>=60,<80;
			   //???????????????<60.
			   if(dto.getScore().intValue()>=90){
				   dto.setIndicatorLineDes("???");
			   }else if(dto.getScore().intValue()>=80){
				   dto.setIndicatorLineDes("???");
			   }else if(dto.getScore().intValue()>=60){
				   dto.setIndicatorLineDes("??????");
			   }else{
				   dto.setIndicatorLineDes("?????????");
			   }
 			   dto.setNo(i);
 			   i++;
 		   }
 	   }
        
        return this.getPage(list, pageSize, pageNum);
    }
    
    
    @Override
    public PageInfo<SupplierAnalysisDetailDTO> querySupplierDetail(SupplierParamDTO param) {
    	
    	Integer pageSize = param.getPageSize();
    	Integer pageNum = param.getPageNum();
    	param.setPageNum(null);
    	param.setPageSize(null);
    	
    	//????????????
    	this.setDateParam(param);
    	
    	LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
    	//????????????id?????????????????????
        List<Organization> permissionNodeResult = baseClient.getFullPathIdByTypeCode(loginAppUser.getUserId());
    	 //?????????????????????????????????
        List<Organization> filterByOrgTypeCode = permissionNodeResult.stream().filter(p -> param.getOrganizationTypeCode().equals(p.getOrganizationTypeCode())).collect(Collectors.toList());
    	//????????????Map
        Map<String, Organization> permissionNodeResultMap = permissionNodeResult.stream().collect(Collectors.toMap(Organization::getFullPathId,
                Function.identity()));
        //????????????????????????
        List<OrganizationRelation> treeNewList = baseClient.allTree();
        List<String> resultFullPathIdList = new ArrayList<>();
        List<String> tempList = null;
    	
        for (Organization organization : filterByOrgTypeCode) {
            String targetParentFullPathId = organization.getFullPathId();
            if (null != param.getFullPathId() && !param.getFullPathId().equals(targetParentFullPathId)) {
        		continue;
        	}
            tempList = OrgUtils.findParentStart(treeNewList,targetParentFullPathId,permissionNodeResultMap);
            resultFullPathIdList.addAll(tempList);
        }
        param.setList(resultFullPathIdList);
        
        
        //??????
        LatestGidailyRate gidailyRate = new LatestGidailyRate();
        gidailyRate.setToCurrency(OcrCurrencyType.CNY.getValue());
        Map<String,List<LatestGidailyRate>> currencyResult = baseClient.getCurrency(gidailyRate);
        
        
       List<SupplierAnalysisDetailDTO> list  = this.supplierMapper.querySupplierDetail(param);
       List<SupplierAnalysisDetailDTO> details = new ArrayList<SupplierAnalysisDetailDTO>();
       Map<String,SupplierAnalysisDetailDTO> map = new HashMap<String,SupplierAnalysisDetailDTO>();
       if (null != list && list.size() > 0) {
    	   SupplierAnalysisDetailDTO temp = null;
    	   for (SupplierAnalysisDetailDTO od : list) {
    		   BigDecimal sumAmount = BigDecimal.ZERO;
    		   if (null != od.getWarehouseReceiptQuantity()) {
				   if (OcrCurrencyType.CNY.getValue().equals(od.getCurrency())) {
					   if (null != od.getReturnQuantity() && null != od.getWarehouseReceiptQuantity()) {
						   sumAmount = sumAmount.add(od.getWarehouseReceiptQuantity().subtract(od.getReturnQuantity()).multiply(od.getUnitPriceContainingTax()));
					   } else if (null != od.getWarehouseReceiptQuantity()) {
						   sumAmount = sumAmount.add(od.getWarehouseReceiptQuantity().multiply(od.getUnitPriceContainingTax()));
					   }
				   } else {
					   BigDecimal rate = currencyResult.get(od.getCurrency()).get(0).getConversionRate();
					   if (null != od.getReturnQuantity() && null != od.getWarehouseReceiptQuantity()) {
						   sumAmount = sumAmount.add(od.getWarehouseReceiptQuantity().subtract(od.getReturnQuantity()).multiply(od.getUnitPriceContainingTax()).multiply(rate));
					   } else if (null != od.getWarehouseReceiptQuantity()) {
						   sumAmount = sumAmount.add(od.getWarehouseReceiptQuantity().multiply(od.getUnitPriceContainingTax()).multiply(rate));
					   }
				   }
				}
    		   
    		   String key = od.getFullPathId() +"_"+od.getVendorId() + "_" + od.getCategoryId()+"_"+od.getMaterialCode();
    		   if (map.containsKey(key)) {
    			   temp = map.get(key);
    			   temp.setOrderAmount(temp.getOrderAmount().add(sumAmount));
    		   } else {
    			   temp = od;
    			   temp.setOrderAmount(sumAmount);
    		   }
    		   map.put(key, temp);
    	   }
    	   if (null != map && map.size() > 0) {
    		   Iterator it = map.keySet().iterator();
    		   
    		   while (it.hasNext()) {
    			   String key = (String) it.next();
    			   details.add(map.get(key));
    		   }
    	   }
    	  
    	   if (null != details && details.size() > 0) {
    		   //??????
    		   Collections.sort(details, Comparator.comparing(SupplierAnalysisDetailDTO::getOrderAmount).reversed());
    		   //????????????
    		   int i=1;
    		   for (SupplierAnalysisDetailDTO dto : details) {
    			   dto.setNo(i);
    			   i++;
    		   }
    	   }
       }
    	//????????????
    	return this.getPage(details, pageSize, pageNum);
    }


	@Override
	public PageInfo<SupplierAnalysisDetailDTO> queryCategoryDetail(SupplierParamDTO param) {

		Integer pageSize = param.getPageSize();
		Integer pageNum = param.getPageNum();
		param.setPageNum(null);
		param.setPageSize(null);

		//??????
		SupplierConfig config = iConfigService.querySupplierConfig();

		//????????????
		this.setDateParam(param);

		LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
		//????????????id?????????????????????
		List<Organization> permissionNodeResult = baseClient.getFullPathIdByTypeCode(loginAppUser.getUserId());
		//?????????????????????????????????
		List<Organization> filterByOrgTypeCode = permissionNodeResult.stream().filter(p -> param.getOrganizationTypeCode().equals(p.getOrganizationTypeCode())).collect(Collectors.toList());
		//????????????Map
		Map<String, Organization> permissionNodeResultMap = permissionNodeResult.stream().collect(Collectors.toMap(Organization::getFullPathId,
				Function.identity()));
		//????????????????????????
		List<OrganizationRelation> treeNewList = baseClient.allTree();
		List<String> resultFullPathIdList = new ArrayList<>();
		List<String> tempList = null;

		for (Organization organization : filterByOrgTypeCode) {
			String targetParentFullPathId = organization.getFullPathId();
			if (null != param.getFullPathId() && !param.getFullPathId().equals(targetParentFullPathId)) {
				continue;
			}
			tempList = OrgUtils.findParentStart(treeNewList,targetParentFullPathId,permissionNodeResultMap);
			resultFullPathIdList.addAll(tempList);
		}
		param.setList(resultFullPathIdList);

		List<SupplierAnalysisDetailDTO> list  = this.supplierMapper.queryCategoryDetail(param);
		Map<Long,SupplierAnalysisDetailDTO> categoryMap = new HashMap<Long,SupplierAnalysisDetailDTO>();
		if (null != list && list.size() > 0) {
			for (SupplierAnalysisDetailDTO dto : list) {
				categoryMap.put(dto.getCategoryId(), dto);
			}
		}
		Map<String,SupplierAnalysisDetailDTO> map = new HashMap<String,SupplierAnalysisDetailDTO>();

		List<SupplierAnalysisDetailDTO> details = null;
		list  = this.supplierMapper.querySupplierDetail(param);
		List<SupplierAnalysisDetailDTO> listExclude = this.supplierMapper.querySupplierDetailExclude(param);
		Map<Long,Object> existMap = new HashMap<Long,Object>();
		Double one=0d, two=0d, three=0d, four=0d;
		for (SupplierAnalysisDetailDTO od : list) {

			if (categoryMap.containsKey(od.getCategoryId())) {
				SupplierAnalysisDetailDTO c = categoryMap.get(od.getCategoryId());
				od.setCooperationVendorNum(c.getCooperationVendorNum());
				od.setOutVendorNum(c.getOutVendorNum());
			}
			if(od.getCooperationVendorNum()!=null) {
				if (od.getCooperationVendorNum().doubleValue() <= new BigDecimal(config.getCategoryOne()).doubleValue()) {
					one++;
				} else if (od.getCooperationVendorNum().doubleValue() <= new BigDecimal(config.getCategoryTwoEnd()).doubleValue()) {
					two++;
				} else if (od.getCooperationVendorNum().doubleValue() <= new BigDecimal(config.getCategoryThreeEnd()).doubleValue()) {
					three++;
				} else {
					four++;
				}
			}
		}

		if (null != list && list.size() > 0) {
			SupplierAnalysisDetailDTO temp = null;
			for (SupplierAnalysisDetailDTO od : list) {
				String key = od.getMaterialCode();
				if (categoryMap.containsKey(od.getCategoryId())) {
					SupplierAnalysisDetailDTO c = categoryMap.get(od.getCategoryId());
					od.setCooperationVendorNum(c.getCooperationVendorNum());
					od.setOutVendorNum(c.getOutVendorNum());
				}

				if(od.getCooperationVendorNum()!=null) {
					if (od.getCooperationVendorNum().doubleValue() <= new BigDecimal(config.getCategoryOne()).doubleValue()) {
						od.setBelongRange("???????????????<" + config.getCategoryOne() + "???");
						od.setBelongRangePercent(BigDecimal.valueOf(one / (one + two + three + four)));
					} else if (od.getCooperationVendorNum().doubleValue() <= new BigDecimal(config.getCategoryTwoEnd()).doubleValue()) {
						od.setBelongRange(config.getCategoryTwoStart() + "???" + "<=" + "???????????????<" + config.getCategoryTwoEnd() + "???");
						od.setBelongRangePercent(BigDecimal.valueOf(two / (one + two + three + four)));
					} else if (od.getCooperationVendorNum().doubleValue() <= new BigDecimal(config.getCategoryThreeEnd()).doubleValue()) {
						od.setBelongRange(config.getCategoryThreeStart() + "???" + "<=" + "???????????????<" + config.getCategoryThreeEnd() + "???");
						od.setBelongRangePercent(BigDecimal.valueOf(three / (one + two + three + four)));
					} else {
						od.setBelongRange("???????????????>=" + config.getCategoryFour() + "???");
						od.setBelongRangePercent(BigDecimal.valueOf(four / (one + two + three + four)));
					}
				}

				existMap.put(od.getCategoryId(),null);

				if (map.containsKey(key)) {
					temp = map.get(key);
					temp.setCompanyName(temp.getCompanyName()+","+od.getCompanyName());
				} else {
					temp = od;
				}
				map.put(key, temp);
			}

			if (null != map && map.size() > 0) {
				Iterator it = map.keySet().iterator();
				details = new ArrayList<SupplierAnalysisDetailDTO>();
				while (it.hasNext()) {
					String key = (String) it.next();
					details.add(map.get(key));
				}
			}

			Iterator itTemp =  categoryMap.keySet().iterator();
			while(itTemp.hasNext()) {
				Long key = (Long) itTemp.next();
				if (existMap.containsKey(key)) {
					continue;
				}
				temp = categoryMap.get(key);
				details.add(temp);
			}

			if (null != details && details.size() > 0) {
				//??????
				Collections.sort(details, Comparator.comparing(SupplierAnalysisDetailDTO::getCooperationVendorNum).reversed());
				//????????????
				int i=1;
				for (SupplierAnalysisDetailDTO dto : details) {
					dto.setNo(i);
					i++;
				}
				//?????????????????????????????????????????????
				details.addAll(listExclude);
			}
		}


		//????????????
		return this.getPage(details, pageSize, pageNum);
	}

    
    @Override
    public PageInfo<SupplierAnalysisDetailDTO> queryCategoryDetailNew(SupplierParamDTO param) {
    	
    	Integer pageSize = param.getPageSize();
    	Integer pageNum = param.getPageNum();
    	param.setPageNum(null);
    	param.setPageSize(null);

		//??????
		SupplierConfig config = iConfigService.querySupplierConfig();

    	//????????????
    	this.setDateParam(param);
    	
    	LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
    	//????????????id?????????????????????
        List<Organization> permissionNodeResult = baseClient.getFullPathIdByTypeCode(loginAppUser.getUserId());
    	 //?????????????????????????????????
        List<Organization> filterByOrgTypeCode = permissionNodeResult.stream().filter(p -> param.getOrganizationTypeCode().equals(p.getOrganizationTypeCode())).collect(Collectors.toList());
    	//????????????Map
        Map<String, Organization> permissionNodeResultMap = permissionNodeResult.stream().collect(Collectors.toMap(Organization::getFullPathId,
                Function.identity()));
        //????????????????????????
        List<OrganizationRelation> treeNewList = baseClient.allTree();
        List<String> resultFullPathIdList = new ArrayList<>();
        List<String> tempList = null;
    	
        for (Organization organization : filterByOrgTypeCode) {
            String targetParentFullPathId = organization.getFullPathId();
            if (null != param.getFullPathId() && !param.getFullPathId().equals(targetParentFullPathId)) {
        		continue;
        	}
            tempList = OrgUtils.findParentStart(treeNewList,targetParentFullPathId,permissionNodeResultMap);
            resultFullPathIdList.addAll(tempList);
        }
        param.setList(resultFullPathIdList);
        
       List<SupplierAnalysisDetailDTO> list  = this.supplierMapper.queryCategoryDetail(param);
       Map<Long,SupplierAnalysisDetailDTO> categoryMap = new HashMap<Long,SupplierAnalysisDetailDTO>();
       if (null != list && list.size() > 0) {
    	   for (SupplierAnalysisDetailDTO dto : list) {
    		   categoryMap.put(dto.getCategoryId(), dto);
    	   }
       }
       Map<Long,SupplierAnalysisDetailDTO> map = new HashMap<Long,SupplierAnalysisDetailDTO>();
       
       List<SupplierAnalysisDetailDTO> details = null;
       list  = this.supplierMapper.querySupplierDetail(param);
		List<SupplierAnalysisDetailDTO> listExclude = this.supplierMapper.querySupplierDetailExclude(param);
       Map<Long,Object> existMap = new HashMap<Long,Object>();
		Double one=0d, two=0d, three=0d, four=0d;
		for (SupplierAnalysisDetailDTO od : list) {

			if (categoryMap.containsKey(od.getCategoryId())) {
				SupplierAnalysisDetailDTO c = categoryMap.get(od.getCategoryId());
				od.setCooperationVendorNum(c.getCooperationVendorNum());
				od.setOutVendorNum(c.getOutVendorNum());
			}
			if(od.getCooperationVendorNum()!=null) {
				if (od.getCooperationVendorNum().doubleValue() <= new BigDecimal(config.getCategoryOne()).doubleValue()) {
					one++;
				} else if (od.getCooperationVendorNum().doubleValue() <= new BigDecimal(config.getCategoryTwoEnd()).doubleValue()) {
					two++;
				} else if (od.getCooperationVendorNum().doubleValue() <= new BigDecimal(config.getCategoryThreeEnd()).doubleValue()) {
					three++;
				} else {
					four++;
				}
			}
		}

       if (null != list && list.size() > 0) {
    	   SupplierAnalysisDetailDTO temp = null;
    	   for (SupplierAnalysisDetailDTO od : list) {
			   Long key = od.getCategoryId();
    		   if (categoryMap.containsKey(od.getCategoryId())) {
    			   SupplierAnalysisDetailDTO c = categoryMap.get(od.getCategoryId());
    			   od.setCooperationVendorNum(c.getCooperationVendorNum());
    			   od.setOutVendorNum(c.getOutVendorNum());
    		   }

			   if(od.getCooperationVendorNum()!=null) {
				   if (od.getCooperationVendorNum().doubleValue() <= new BigDecimal(config.getCategoryOne()).doubleValue()) {
					   od.setBelongRange("???????????????<" + config.getCategoryOne() + "???");
					   od.setBelongRangePercent(BigDecimal.valueOf(one / (one + two + three + four)));
				   } else if (od.getCooperationVendorNum().doubleValue() <= new BigDecimal(config.getCategoryTwoEnd()).doubleValue()) {
					   od.setBelongRange(config.getCategoryTwoStart() + "???" + "<=" + "???????????????<" + config.getCategoryTwoEnd() + "???");
					   od.setBelongRangePercent(BigDecimal.valueOf(two / (one + two + three + four)));
				   } else if (od.getCooperationVendorNum().doubleValue() <= new BigDecimal(config.getCategoryThreeEnd()).doubleValue()) {
					   od.setBelongRange(config.getCategoryThreeStart() + "???" + "<=" + "???????????????<" + config.getCategoryThreeEnd() + "???");
					   od.setBelongRangePercent(BigDecimal.valueOf(three / (one + two + three + four)));
				   } else {
					   od.setBelongRange("???????????????>=" + config.getCategoryFour() + "???");
					   od.setBelongRangePercent(BigDecimal.valueOf(four / (one + two + three + four)));
				   }
			   }

    		   existMap.put(od.getCategoryId(),null);

    		   if (map.containsKey(key)) {
    			   temp = map.get(key);
    			   temp.setCompanyName(temp.getCompanyName()+","+od.getCompanyName());
    		   } else {
    			   temp = od;
    		   }
    		   map.put(key, temp);
    	   }

    	   if (null != map && map.size() > 0) {
    		   Iterator it = map.keySet().iterator();
    		   details = new ArrayList<SupplierAnalysisDetailDTO>();
    		   while (it.hasNext()) {
    			   Long key = (Long) it.next();
    			   temp = categoryMap.get(key);
    			   if (null != temp ) {
    				   if (null == temp.getCooperationVendorNum()) {
        				   temp.setCooperationVendorNum(0);
        			   }
        			   details.add(map.get(key));
    			   }
    		   }
    	   }

    	   Iterator itTemp =  categoryMap.keySet().iterator();
    	   while(itTemp.hasNext()) {
    		   Long key = (Long) itTemp.next();
    		   if (existMap.containsKey(key)) {
    			   continue;
    		   }
    		   temp = categoryMap.get(key);
    		   if (null == temp.getCooperationVendorNum()) {
    			   temp.setCooperationVendorNum(0);
    		   }
    		   details.add(temp);
    	   }

    	   if (null != details && details.size() > 0) {
    		   //??????
    		   Collections.sort(details, Comparator.comparing(SupplierAnalysisDetailDTO::getCooperationVendorNum).reversed());
    		   //????????????
    		   int i=1;
    		   for (SupplierAnalysisDetailDTO dto : details) {
    			   dto.setNo(i);
    			   i++;
    		   }
    		 //?????????????????????????????????????????????
			   details.addAll(listExclude);
    	   }
       }


    	//????????????
    	return this.getPage(details, pageSize, pageNum);
    }
    
    
    private PageInfo getPage(List list,Integer pageSize,Integer pageNum) {
   	 PageInfo pageInfo = new PageInfo() ;
        List newList = new ArrayList();
        //????????????
        if (null != pageNum && null != pageSize) {
        	pageInfo.setPageSize(pageSize);
        	pageInfo.setPageNum(pageNum);
        	int start = (pageNum - 1) * pageSize;
        	int end = 0;
        	if (list.size() > (pageNum * pageSize)) {
        		end = pageNum * pageSize;
        	} else {
        		end = list.size();
        	}
        	for (int i = start ;i<end;i++) {
        		newList.add(list.get(i));
        	}
        	pageInfo.setList(newList);
        	
        } else {
        	pageInfo.setList(list);
        }
        pageInfo.setTotal(list.size());
    	return pageInfo;
   }
}
