package com.midea.cloud.common.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.util.FileUtils;
import com.alibaba.excel.write.builder.ExcelWriterSheetBuilder;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.alibaba.fastjson.JSON;
import com.midea.cloud.common.enums.FileUploadType;
import com.midea.cloud.common.enums.ImportStatus;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.handler.*;
import com.midea.cloud.common.listener.AnalysisEventListenerImpl;
import com.midea.cloud.component.context.container.SpringContextHolder;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.model.annonations.ExcelParamCheck;
import com.midea.cloud.srm.model.base.dict.dto.DictItemDTO;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCurrency;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class EasyExcelUtil {
    /**
     * ??????????????????: ??????-??????
     */
    public static final String NAME_CODE = "NAME_CODE";

    /**
     * ??????????????????: ??????-??????
     */
    public static final String CODE_NAME = "CODE_NAME";

    public static BaseClient baseClient;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TestVO {
        @ExcelProperty(value = "*??????", index = 0)
        private String name;
        @ExcelProperty(value = "*??????", index = 1)
        private int age;
        @ExcelProperty(value = "??????", index = 2)
        private String school;
    }

    @Data
    @ColumnWidth(20)
    public static class TestVO1 {
        @ExcelProperty(value = "??????", index = 0)
        private String name;
        @ExcelProperty(value = "*??????", index = 1)
        private int age;
        @ExcelProperty(value = "*??????", index = 2)
        private String school;
    }

    @Data
    @ColumnWidth(20)
    public static class MultiLanguage {
        @ExcelProperty(value = "??????1", index = 0)
        private String value1;
        @ExcelProperty(value = "??????2", index = 1)
        private String value2;
        @ExcelProperty(value = "??????3", index = 2)
        private String value3;
        @ExcelProperty(value = "??????4", index = 3)
        private String value4;
    }

    @Test
    public void testReadAllSheet() throws IOException {
        // ?????????
        FileInputStream inputStream = new FileInputStream("D:\\MyData\\wangpr\\Desktop\\?????????????????????.xlsx");
        List<MultiLanguage> listenerDatas = readExcelAllSheet(inputStream, MultiLanguage.class);
        if(CollectionUtils.isNotEmpty(listenerDatas)){
            listenerDatas.forEach(System.out::println);
        }
    }

    /**
     * ?????????????????????
     */
    @Test
    public void simpleExport() throws Exception {
        // ?????????
        FileOutputStream outputStream = new FileOutputStream("D:/???????????????.xlsx");
        // ??????
        List<TestVO> testVOS = Arrays.asList(new TestVO("??????",18,"??????"));
        // ??????
        EasyExcel.write(outputStream).head(TestVO.class).sheet(0).sheetName("sheetName").doWrite(testVOS);
    }

    /**
     * ??????????????????
     * @throws Exception
     */
    @Test
    public void simpleImport() throws Exception{
        // ?????????
        FileInputStream inputStream = new FileInputStream("D:/???????????????.xlsx");
        //????????????
        List<TestVO> datas = readExcelWithModelNew(inputStream, TestVO.class);
        // ??????
        System.out.println(JSON.toJSONString(datas));
    }

    /**
     * ?????????????????????,??????????????????
     */
    @Test
    public void testNotModelExport() throws FileNotFoundException {
        // ??????
        List<String> title = Arrays.asList("??????", "??????", "??????");
        // ??????
        List<List<Object>> dataList = new ArrayList<>();
        for(int i= 0; i< 10 ;i++){
            dataList.add(Arrays.asList("??????"+i,18+i,"??????"+i));
        }
        // ?????????
        FileOutputStream outputStream = new FileOutputStream("D:/???????????????1.xlsx");
        writeExcelWithModel(outputStream,dataList,title,"sheet",new CustomizeExportHandler(20));
    }

    @Test
    public void testBigDropDown() throws Exception {
        Map<Integer, String[]> spinnerLongMap = new HashMap<>();
        String arrays[] = new String[400];
        for(int i = 0;i<400;i++){
            arrays[i] = "????????????"+i;
        }
        spinnerLongMap.put(4,arrays);
        EasyExcel.write(
                new FileOutputStream("D:/2.xlsx"), TestVO.class)
                .registerWriteHandler(new SpinnerLongHandler())
                .sheet("??????").doWrite(new ArrayList<TestVO>());
    }

    /**
     * ????????????????????????
     */
    @Test
    public void testBatchExport() throws Exception {
        FileOutputStream outputStream = new FileOutputStream("D:/1.xlsx");
        ExcelWriter excelWriter = EasyExcel.write(outputStream).build();
        /**
         * ????????????????????????
         * ??????: ???????????????30000?????????, ?????????????????????10000?????????????????????(??????????????????????????????), ??????3???
         */
        WriteSheet writeSheet = EasyExcel.writerSheet(0).head(TestVO.class).build();
        for(int i = 0 ; i < 3 ; i++){
            // ????????????10000?????????
            List<TestVO> testVOS = new ArrayList<>();
            for(int j = 0; j < 10000;j++){
                TestVO testVO = new TestVO("??????"+j,j,"??????"+j);
                testVOS.add(testVO);
            }
            excelWriter.write(testVOS,writeSheet);
        }
        excelWriter.finish();
    }

    /**
     * ?????????????????????
     *
     * @throws IOException
     */
    @Test
    public void testDropDown() throws IOException {
        // ?????????
        OutputStream outputStream = new FileOutputStream(new File("D:\\1.xlsx"));
        // ???????????????
        List<TestVO> dataList = new ArrayList<>();
        // ?????????????????????
        List<Integer> columns = Arrays.asList(0, 1);
        // ????????????
        HashMap<Integer, String> annotationsMap = new HashMap<>();
        annotationsMap.put(0, "?????????????????????");
        annotationsMap.put(1, "?????????????????????");
        HashMap<Integer, String[]> dropDownMap = new HashMap<>();
        // ???????????????
        String[] ags = {"13", "34", "64"};
        String[] school = {"??????", "??????", "??????"};
        dropDownMap.put(1, ags);
        dropDownMap.put(2, school);
        TitleHandler titleHandler = new TitleHandler(columns, IndexedColors.RED.index, annotationsMap, dropDownMap);
        writeExcelWithModel(outputStream, dataList, TestVO.class, "sheetName", titleHandler);
    }

    /**
     * ??????????????????
     * 1. ?????????????????????????????????
     * 2. ???????????????????????????
     */
    @Test
    public void testExport1() throws FileNotFoundException {
        // ?????????
        OutputStream outputStream = new FileOutputStream(new File("D:\\1.xlsx"));
        // ???????????????
        List<TestVO> dataList = new ArrayList<>();
        // ?????????????????????
        List<Integer> columns = Arrays.asList(0, 1);
        // ????????????
        HashMap<Integer, String> annotationsMap = new HashMap<>();
        annotationsMap.put(0, "?????????????????????");
        annotationsMap.put(1, "?????????????????????");
        TitleHandler titleHandler = new TitleHandler(columns, IndexedColors.RED.index, annotationsMap);
        writeExcelWithModel(outputStream, dataList, TestVO.class, "sheetName", titleHandler);
    }

    /**
     * ??????????????????sheet??????
     *
     * @throws Exception
     */
    @Test
    public void read() throws Exception {
        String filePath = "D:/1.xlsx";
        InputStream inputStream = null;
        inputStream = new FileInputStream(new File(filePath));
        AnalysisEventListenerImpl<Object> listener = new AnalysisEventListenerImpl<>();
        ExcelReader excelReader = EasyExcel.read(inputStream, listener).build();
        // ?????????sheet????????????
        ReadSheet readSheet1 = EasyExcel.readSheet(0).head(TestVO.class).headRowNumber(3).build();
        // ?????????sheet????????????
        ReadSheet readSheet2 = EasyExcel.readSheet(1).head(TestVO1.class).build();
        // ?????????????????????sheet
        excelReader.read(readSheet1);
        List<Object> list = listener.getDatas();
        list.forEach((user) -> {
            TestVO user1 = (TestVO) user;
            System.out.println(user1.getName() + ", " + user1.getAge() + ", " + user1.getSchool());
        });
        // ?????????????????????
        listener.getDatas().clear();
        // ?????????????????????sheet
        excelReader.read(readSheet2);
        System.out.println("---------------------------------");
        List<Object> list2 = listener.getDatas();
        list2.forEach((user) -> {
            TestVO1 user2 = (TestVO1) user;
            System.out.println(user2.getName() + ", " + user2.getAge() + ", " + user2.getSchool());
        });
    }

    /**
     * ??????sheet????????????
     *
     * @throws FileNotFoundException
     */
    @Test
    public void sheetImport() throws FileNotFoundException {
        // ?????????
        OutputStream outputStream = null;
        outputStream = new FileOutputStream(new File("D:/1.xlsx"));

        // ???????????????
        List<TestVO> dataList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TestVO testVO = new TestVO();
            testVO.setAge(i + 20);
            testVO.setName("vo" + i);
            testVO.setSchool("school" + i);
            dataList.add(testVO);
        }

        // ??????
        List<String> headList = Arrays.asList("??????", "??????", "??????");

        String sheetName = "????????????";

        List<Integer> columnIndexs = Arrays.asList(0, 1, 2);
        List<Integer> rowIndexs = Arrays.asList(0);
        TitleColorSheetWriteHandler titleColorSheetWriteHandler = new TitleColorSheetWriteHandler(rowIndexs, columnIndexs, IndexedColors.RED.index);

        List<Integer> columnIndexs1 = Arrays.asList(0, 1);
        List<Integer> rowIndexs1 = Arrays.asList(1, 2, 3, 4);
        CellColorSheetWriteHandler colorSheetWriteHandler = new CellColorSheetWriteHandler(rowIndexs1, columnIndexs1, IndexedColors.RED.index);

        // ?????????sheel??????
        ExcelWriter excelWriter = EasyExcel.write(outputStream).build();
        // ????????????
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        // ???????????????
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        // ?????????????????????
        HorizontalCellStyleStrategy horizontalCellStyleStrategy = new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle);
        WriteSheet test1 = EasyExcel.writerSheet(0, "test1").head(TestVO.class).
                registerWriteHandler(horizontalCellStyleStrategy).
                registerWriteHandler(titleColorSheetWriteHandler).build();
        WriteSheet test2 = EasyExcel.writerSheet(1, "test2").head(TestVO.class).
                registerWriteHandler(horizontalCellStyleStrategy).
                registerWriteHandler(titleColorSheetWriteHandler).build();
        excelWriter.write(dataList, test1).write(dataList, test2);
        excelWriter.finish();
    }

    /**
     * ?????? ?????? ?????????Excel
     *
     * @param fileInputStream Excel????????????
     * @param tClass         ????????????
     * @return ?????? ?????? ?????????(???object??????,?????????)
     */
    public static <T> List<T> readExcelAllSheet(InputStream fileInputStream, Class<T> tClass) throws IOException {
        AnalysisEventListenerImpl<T> listener = new AnalysisEventListenerImpl<T>();
        ExcelReader excelReader = EasyExcel.read(fileInputStream, tClass, listener).build();
        excelReader.readAll();
        excelReader.finish();
        return listener.getDatas();
    }

    /**
     * ????????????????????????/??????
     *
     * @param wb         ?????????
     * @param cell       ?????????
     * @param sheet      ?????????
     * @param annotation ????????????
     * @param content    ???????????????
     */
    public static void setCellStyle(Workbook wb, Cell cell, XSSFSheet sheet, String annotation, String content) {
        // ??????????????????
        XSSFDrawing p = sheet.createDrawingPatriarch();
        // ?????????????????????
        cell.setCellValue(content);
        // ??????????????????
        // (int dx1, int dy1, int dx2, int dy2, short col1, int row1, short
        // col2, int row2)
        // ???????????????????????????,???????????????????????????????????????????????????.
        XSSFComment comment = p.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 5, 5, (short) 10, 10));
        // ??????????????????
        comment.setString(new XSSFRichTextString(annotation));
        // ????????????,??????B5?????????,????????????
//		comment.setAuthor("toad");
        // ????????????????????????????????????
        cell.setCellComment(comment);
        CellStyle cellStyle = wb.createCellStyle();
        // ????????????????????????:????????????
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        // ????????????????????????:????????????
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        // ??????????????????
        Font font = wb.createFont();
        font.setBold(true);
        cellStyle.setFont(font);

        cellStyle.setBorderBottom(BorderStyle.THIN); //?????????
        cellStyle.setBorderLeft(BorderStyle.THIN);//?????????
        cellStyle.setBorderTop(BorderStyle.THIN);//?????????
        cellStyle.setBorderRight(BorderStyle.THIN);//?????????
//		// ????????????????????????
//		cellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
//		cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        // ?????????????????????
        cell.setCellStyle(cellStyle);
    }

    /**
     * ??????????????????EXCEL??????
     * @param file ???????????????
     * @param classA  ?????????
     * @param <T> ??????
     * @return
     */
    public static <T> List<T> readExcelWithModel(MultipartFile file, Class<T> classA) {
        List<T> list = new ArrayList<>();
        try {
            // ???????????????
            InputStream inputStream = file.getInputStream();
            // ???????????????
            AnalysisEventListenerImpl<T> listener = new AnalysisEventListenerImpl<>();
            ExcelReader excelReader = EasyExcel.read(inputStream, listener).build();
            // ?????????sheet????????????
            ReadSheet readSheet = EasyExcel.readSheet(0).head(classA).build();
            // ?????????????????????sheet
            excelReader.read(readSheet);
            list = listener.getDatas();
        } catch (IOException e) {
            throw new BaseException("excel????????????");
        }
        return list;
    }

    /**
     * ?????? ?????? ?????????Excel
     *
     * @param fileInputStream Excel????????????
     * @param tClass         ????????????
     * @return ?????? ?????? ?????????(???object??????,?????????)
     */
    public static <T> List<T> readExcelWithModelNew(InputStream fileInputStream, Class<T> tClass) throws IOException {
        AnalysisEventListenerImpl<T> listener = new AnalysisEventListenerImpl<T>();
        ExcelReader excelReader = EasyExcel.read(fileInputStream, tClass, listener).build();
        ReadSheet readSheet = EasyExcel.readSheet(0).build();
        excelReader.read(readSheet);
        excelReader.finish();
        return listener.getDatas();
    }

    /**
     * ?????? ?????? ?????????Excel
     *
     * @param fileInputStream Excel????????????
     * @param clazz           ????????????
     * @return ?????? ?????? ?????????(???object??????,?????????)
     */
    public static List<Object> readExcelWithModel(InputStream fileInputStream, Class<? extends Object> clazz) throws IOException {
        AnalysisEventListenerImpl<Object> listener = new AnalysisEventListenerImpl<Object>();
        ExcelReader excelReader = EasyExcel.read(fileInputStream, clazz, listener).build();
        ReadSheet readSheet = EasyExcel.readSheet(0).build();
        excelReader.read(readSheet);
        excelReader.finish();
        return listener.getDatas();
    }


    public static<T> void readExcelWithModel(InputStream fileInputStream,Class<T> clazz, AnalysisEventListener listener , int sheetNo) throws IOException {
        ExcelReader reader = EasyExcel.read(fileInputStream,clazz, listener).build();
        ReadSheet readSheet = EasyExcel.readSheet(sheetNo).build();
        reader.read(readSheet);
        reader.finish();
    }


    /**
     * ???????????????Excel
     *
     * @param dataList ???????????????
     * @param clazz    ????????????
     * @throws IOException
     */
    public static byte[] writeExcelWithModel(List<? extends Object> dataList, Class<? extends Object> clazz) throws IOException {
        String fileName = System.currentTimeMillis() + ".xls";
        //????????????????????????
        ExcelWriter excelWriter = EasyExcel.write(fileName).build();
        //??????writeSheet?????????????????????
        WriteSheet writeSheet = new WriteSheet();
        //??????sheet??????
        writeSheet.setSheetName("sheet1");
        //??????Clazz??????????????????ExcelProperty???????????????
        writeSheet.setClazz(clazz);
        /**
         * ????????????Write????????????
         * ????????????????????????????????????
         * ??????????????????????????????sheet??????
         */
        excelWriter.write(dataList, writeSheet);
        excelWriter.finish();
        File file = new File(fileName);
        byte[] buffer = FileUtils.readFileToByteArray(file);
        file.delete();
        return buffer;
    }

    /**
     * ?????? ?????? ?????????Excel
     *
     * @param outputStream Excel????????????
     * @param dataList     ??????????????? ?????? ??????????????????
     * @param clazz        ????????????
     */
    public static void writeExcelWithModel(OutputStream outputStream, String sheetName, List<? extends Object> dataList, Class<? extends Object> clazz) {
        //?????????????????????????????????model????????????????????????
        ExcelWriter writer = EasyExcel.write(outputStream).build();
        //??????writeSheet?????????????????????
        WriteSheet sheet = EasyExcel.writerSheet().sheetName(sheetName).head(clazz).build();
        writer.write(dataList, sheet);
        writer.finish();
    }

    /**
     * ???????????????Excel
     *
     * @param dataList ???????????????
     * @param clazz    ????????????
     * @throws IOException
     */
    public static <T> void writeExcelWithModel(HttpServletResponse response,String fileName,List<T> dataList, Class<T> clazz) throws IOException {
        OutputStream outputStream = getServletOutputStream(response, fileName);
        EasyExcel.write(outputStream).head(clazz).sheet(0,fileName).doWrite(dataList);
    }

    /**
     * ?????? ?????? ?????????Excel
     *
     * @param outputStream Excel????????????
     * @param dataList     ??????????????? ?????? ??????????????????
     * @param clazz        ????????????
     */
    public static void writeExcelWithModel(OutputStream outputStream,List<? extends Object> dataList, Class<? extends Object> clazz) {
        //?????????????????????????????????model????????????????????????
        ExcelWriter writer = EasyExcel.write(outputStream).build();
        //??????writeSheet?????????????????????
        WriteSheet sheet = EasyExcel.writerSheet().head(clazz).build();
        writer.write(dataList, sheet);
        writer.finish();
    }

    /**
     * ????????????????????????sheet???Excel
     * Description
     *
     * @Param [outputStream, sheetName, dataList, clazz]
     * @Author fansb3@meicloud.com
     * @Date 2020/10/15
     **/
    public static void writeExcelWithModel(OutputStream outputStream, String[] sheetName, List<List<? extends Object>> dataList, Class<? extends Object>[] clazz) {
        ExcelWriter writer = EasyExcel.write(outputStream).build();
        for (int i = 1; i < sheetName.length; i++) {
            WriteSheet sheet = EasyExcel.writerSheet(i - 1, sheetName[i]).head(clazz[i - 1]).build();
            writer.write(dataList.get(i - 1), sheet);
        }
        writer.finish();
    }

    /**
     * @param filePath  ????????????
     * @param sheetName ??????sheet??????
     * @param headList  sheet??????
     * @param lineList  sheet?????????
     */
    public static void writeExcelWithModel(String filePath, String sheetName, List<List<String>> headList, List lineList) {
        EasyExcel.write(filePath).head(headList).sheet(sheetName).doWrite(lineList);
    }

    /**
     * @param outputStream ?????????
     * @param sheetName    ??????sheet??????
     * @param headList     sheet??????
     * @param lineList     sheet?????????
     */
    public static void writeExcelWithModel(OutputStream outputStream, String sheetName, List<String> headList, List<List<Object>> lineList) {
        List<List<String>> list = new ArrayList<>();
        if (headList != null) {
            headList.forEach(h -> list.add(Collections.singletonList(h)));
            EasyExcel.write(outputStream).head(list).sheet(sheetName).
                    registerWriteHandler(new LongestMatchColumnWidthStyleStrategy()).doWrite(lineList);
        }
    }

    /**
     * ??????excle
     *
     * @param outputStream ?????????
     * @param data         ?????????
     * @param head         ??????
     */
    public static void writeSimpleBySheet(OutputStream outputStream, List<List<Object>> data, List<String> head) {
        // ????????????
        writeExcelWithModel(outputStream,data,head,"sheet",new CustomizeExportHandler(15));
    }

    /**
     * ???????????????
     *
     * @param response
     * @param fileName
     * @return
     * @throws IOException
     */
    public static ServletOutputStream getServletOutputStream(HttpServletResponse response, String fileName) throws IOException {
        fileName = URLEncoder.encode(fileName, "UTF-8");
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf8");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xlsx");
        response.setHeader("Pragma", "public");
        response.setHeader("Cache-Control", "no-store");
        response.addHeader("Cache-Control", "max-age=0");
        return response.getOutputStream();
    }

    /**
     * ????????????
     *
     * @param response
     * @param dataList
     * @param head
     * @param fileName
     * @throws IOException
     */
    public static void exportStart(HttpServletResponse response, List<List<Object>> dataList, List<String> head, String fileName) throws IOException {
        // ???????????????
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileName);
        // ????????????
        EasyExcelUtil.writeExcelWithModel(outputStream, fileName, head, dataList);
    }

    /**
     * ?????????????????? Excel
     *
     * @param fileName ?????????
     * @return
     */
    public static boolean isExcel(String fileName) {
        if (null != fileName) {
            fileName = fileName.substring(fileName.lastIndexOf(".") + 1);
            if ("xls".equals(fileName) || "xlsx".equals(fileName)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * ??????excel??????.xlsx
     * @param file
     * @return
     */
    public static void checkExcelIsXlsx(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (null != fileName) {
            fileName = fileName.substring(fileName.lastIndexOf(".") + 1);
            if (!"xlsx".equals(fileName)) {
                throw new BaseException("??????????????????excel??????,??????????????????: [.xlsx]");
            }
        } else {
            throw new BaseException("???????????????!");
        }
    }

    /**
     * ????????????????????????
     *
     * @param fileCenterClient
     * @param fileupload
     * @param dataList
     * @param clazz
     */
    public static Fileupload uploadErrorFile(FileCenterClient fileCenterClient, Fileupload fileupload
            , List<? extends Object> dataList, Class<? extends Object> clazz, String fileName, String originalFilename, String contentType) {
        ByteArrayInputStream inputStream = null;
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            writeExcelWithModel(outputStream, fileupload.getFileSourceName(), dataList, clazz);
            inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            MultipartFile file = new MockMultipartFile(fileName, originalFilename, contentType, inputStream);
            return fileCenterClient.feignClientUpload(file, fileupload.getSourceType(), fileupload.getUploadType(), fileupload.getFileModular(), fileupload.getFileFunction(), fileupload.getFileType());
        } catch (Exception e) {
            throw new BaseException(e.getMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Fileupload uploadErrorFile(FileCenterClient fileCenterClient, Fileupload fileupload
            , List<List<? extends Object>> dataList, Class<? extends Object>[] clazz, String[] fileName, String originalFilename, String contentType) {
        ByteArrayInputStream inputStream = null;
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            writeExcelWithModel(outputStream, fileName, dataList, clazz);
            inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            MultipartFile file = new MockMultipartFile(fileName[0], originalFilename, contentType, inputStream);
            return fileCenterClient.feignClientUpload(file, fileupload.getSourceType(), fileupload.getUploadType(), fileupload.getFileModular(), fileupload.getFileFunction(), fileupload.getFileType());
        } catch (Exception e) {
            throw new BaseException(e.getMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * ??????????????????
     * @param fileCenterClient
     * @param dataList
     * @param tClass
     * @param fileName
     * @param file
     * @param <T>
     * @return
     * @throws IOException
     */
    public static <T> Map<String,Object> uploadErrorFile(FileCenterClient fileCenterClient
            , List<T> dataList, Class<T> tClass,String fileName,MultipartFile file) throws IOException {
        Fileupload fileupload = new Fileupload().
                setUploadType(FileUploadType.FASTDFS.name()).
                setSourceType("WEB_APP").
                setFileModular("logistics").
                setFileFunction("quotedLineImport").
                setFileType("images");
        ByteArrayInputStream inputStream = null;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeExcelWithModel(outputStream, fileName, dataList, tClass);
        inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        MultipartFile fileNew = new MockMultipartFile(file.getName(), file.getOriginalFilename(), file.getContentType(), inputStream);
        Fileupload errorFileupload = fileCenterClient.feignClientUpload(fileNew, fileupload.getSourceType(), fileupload.getUploadType(), fileupload.getFileModular(), fileupload.getFileFunction(), fileupload.getFileType());
        return ImportStatus.importError(errorFileupload.getFileuploadId(),errorFileupload.getFileSourceName());
    }

    /**
     * ????????????????????????list????????????
     *
     * @param fileCenterClient
     * @param fileupload
     * @param dataList
     * @param file
     * @param <T>
     * @return
     */
    public static <T> Fileupload uploadErrorFile(FileCenterClient fileCenterClient
            , Fileupload fileupload, List<T> dataList, Class<T> clazz, MultipartFile file) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ) {
            //???????????????outputStream
            writeExcelWithModel(outputStream, fileupload.getFileSourceName(), dataList, clazz);
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
                file = new MockMultipartFile(file.getName(), file.getOriginalFilename(), file.getContentType(), inputStream);
                return fileCenterClient.feignClientUpload(file, fileupload.getSourceType(), fileupload.getUploadType()
                        , fileupload.getFileModular(), fileupload.getFileFunction()
                        , fileupload.getFileType());
            }
        } catch (IOException e) {
            throw new BaseException("????????????????????????!");
        }
    }

    /**
     * ????????????????????????
     *
     * @param file
     * @param fileupload
     */
    public static void checkParam(MultipartFile file, Fileupload fileupload) {
        Assert.notNull(fileupload, "????????????????????????");
        Assert.notNull(fileupload.getSourceType(), "????????????????????????");
        Assert.notNull(fileupload.getUploadType(), "??????????????????????????????");
        Assert.notNull(fileupload.getFileModular(), "??????????????????????????????");
        Assert.notNull(fileupload.getFileFunction(), "??????????????????????????????");
        Assert.notNull(fileupload.getFileType(), "??????????????????????????????");
        Assert.notNull(file, "??????????????????");
        //?????????????????????excel
        String fileName = file.getOriginalFilename();
        if (!EasyExcelUtil.isExcel(fileName)) {
            throw new RuntimeException("??????????????????Excel??????");
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @param outputStream      ?????????
     * @param dataList          ????????????
     * @param headList          ????????????
     * @param sheetName         sheetname
     * @param cellWriteHandlers
     */
    public static void writeExcelWithModel(OutputStream outputStream, List<List<Object>> dataList, List<String> headList, String sheetName, CellWriteHandler... cellWriteHandlers) {
        List<List<String>> list = new ArrayList<>();
        if (headList != null) {
            headList.forEach(h -> list.add(Collections.singletonList(h)));
        }

        // ????????????
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        // ???????????????
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        // ?????????????????????
        HorizontalCellStyleStrategy horizontalCellStyleStrategy = new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle);

        ExcelWriterSheetBuilder excelWriterSheetBuilder = EasyExcel.write(outputStream).head(list).sheet(sheetName).registerWriteHandler(horizontalCellStyleStrategy);
        if (null != cellWriteHandlers && cellWriteHandlers.length > 0) {
            for (int i = 0; i < cellWriteHandlers.length; i++) {
                excelWriterSheetBuilder.registerWriteHandler(cellWriteHandlers[i]);
            }
        }
        // ????????????
        excelWriterSheetBuilder.doWrite(dataList);
    }

    /**
     * ??????excel
     *
     * @param outputStream      ?????????
     * @param dataList          ???????????????
     * @param classT            ?????????
     * @param sheetName         sheetName
     * @param cellWriteHandlers ???????????????
     */
    public static void writeExcelWithModel(OutputStream outputStream, List<? extends Object> dataList, Class<? extends Object> classT, String sheetName, CellWriteHandler... cellWriteHandlers) {

        // ????????????
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        // ???????????????
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        // ?????????????????????
        HorizontalCellStyleStrategy horizontalCellStyleStrategy = new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle);

        ExcelWriterSheetBuilder excelWriterSheetBuilder = EasyExcel.write(outputStream, classT).sheet(sheetName).registerWriteHandler(horizontalCellStyleStrategy);
        if (null != cellWriteHandlers && cellWriteHandlers.length > 0) {
            for (int i = 0; i < cellWriteHandlers.length; i++) {
                excelWriterSheetBuilder.registerWriteHandler(cellWriteHandlers[i]);
            }
        }
        // ????????????
        excelWriterSheetBuilder.doWrite(dataList);
    }

    /**
     * ??????????????????
     *
     * @param code       ????????????
     * @param baseClient
     * @return ?????? - ??????
     */
    public static Map<String, String> getDicNameCode(String code, BaseClient baseClient) {
        Map<String, String> divisionMap = new HashMap<>(16);
        divisionMap = getDicMapByCode(code, baseClient, divisionMap,NAME_CODE);
        return divisionMap;
    }

    /**
     * ??????????????????
     *
     * @param code       ????????????
     * @return ?????? - ??????
     */
    public static Map<String, String> getDicNameCode(String code) {
        Map<String, String> divisionMap = new HashMap<>(16);
        divisionMap = getDicMapByCode(code, baseClient, divisionMap,NAME_CODE);
        return divisionMap;
    }

    /**
     * ??????????????????
     *
     * @param code       ????????????
     * @param baseClient
     * @return ?????? - ??????
     */
    public static Map<String, String> getDicCodeName(String code, BaseClient baseClient) {
        Map<String, String> divisionMap = new HashMap<>();
        divisionMap = getDicMapByCode(code, baseClient, divisionMap, CODE_NAME);
        return divisionMap;
    }

    /**
     * ??????????????????
     *
     * @param code       ????????????
     * @return ?????? - ??????
     */
    public static Map<String, String> getDicCodeName(String code) {
        Map<String, String> divisionMap = new HashMap<>();
        divisionMap = getDicMapByCode(code, baseClient, divisionMap, CODE_NAME);
        return divisionMap;
    }

    /**
     * ????????????????????????
     * @param dicCodes ??????????????????
     * @return ?????? - ??????
     */
    public static Map<String, Map<String,String>> getDicCodeNameByCodes(List<String> dicCodes){
        Map<String, Map<String,String>> dicCodeNameMap = new HashMap<>(16);
        getDicMapByCodes(dicCodes, dicCodeNameMap,CODE_NAME);
        return dicCodeNameMap;
    }

    /**
     * ????????????????????????
     * @param dicCodes ??????????????????
     * @return ?????? - ??????
     */
    public static Map<String, Map<String,String>> getDicNameCodeByCodes(List<String> dicCodes){
        Map<String, Map<String,String>> dicCodeNameMap = new HashMap<>(16);
        getDicMapByCodes(dicCodes, dicCodeNameMap,NAME_CODE);
        return dicCodeNameMap;
    }

    /**
     * ?????????????????????????????????
     * @param baseClient ?????? - ??????
     * @return
     */
    public static Map<String,String> getCurrencyCodeName(BaseClient baseClient){
        Map<String, String> currencyCodeName = new HashMap<>();
        getCurrencyAll(baseClient, currencyCodeName,CODE_NAME);
        return currencyCodeName;
    }

    /**
     * ?????????????????????????????????
     * @param baseClient ?????? - ??????
     * @return
     */
    public static Map<String,String> getCurrencyCodeName(){
        Map<String, String> currencyCodeName = new HashMap<>();
        getCurrencyAll(baseClient, currencyCodeName,CODE_NAME);
        return currencyCodeName;
    }

    /**
     * ?????????????????????????????????
     * @param baseClient ?????? - ??????
     * @return
     */
    public static Map<String,String> getCurrencyNameCode(BaseClient baseClient){
        Map<String, String> currencyCodeName = new HashMap<>();
        getCurrencyAll(baseClient, currencyCodeName,NAME_CODE);
        return currencyCodeName;
    }

    /**
     * ?????????????????????????????????
     * @param baseClient ?????? - ??????
     * @return
     */
    public static Map<String,String> getCurrencyNameCode(){
        Map<String, String> currencyCodeName = new HashMap<>();
        getCurrencyAll(baseClient, currencyCodeName,NAME_CODE);
        return currencyCodeName;
    }

    public static Map<String, String> getDicMapByCode(String code, BaseClient baseClient, Map<String, String> divisionMap,String type) {
        if (StringUtil.notEmpty(code)) {
            if(null == baseClient){
                baseClient = getBaseClient();
            }
            List<DictItemDTO> division = baseClient.listAllByDictCode(code);
            if (CollectionUtils.isNotEmpty(division)) {
                Map<String, String> map;
                if (NAME_CODE.equals(type)) {
                    map = division.stream().collect(Collectors.toMap(DictItemDTO::getDictItemName, DictItemDTO::getDictItemCode, (k1, k2) -> k1));
                }else {
                    map = division.stream().collect(Collectors.toMap(DictItemDTO::getDictItemCode, DictItemDTO::getDictItemName, (k1, k2) -> k1));
                }
                divisionMap.putAll(map);
            }
        }
        return divisionMap;
    }

    public static void getDicMapByCodes(List<String> dicCodes, Map<String, Map<String, String>> dicCodeNameMap, String type) {
        if (CollectionUtils.isNotEmpty(dicCodes)) {
            BaseClient baseClient = getBaseClient();
            List<DictItemDTO> dictItemDTOS = baseClient.listByDictCode(dicCodes);
            if(CollectionUtils.isNotEmpty(dictItemDTOS)){
                Map<String, List<DictItemDTO>> listMap = dictItemDTOS.stream().collect(Collectors.groupingBy(DictItemDTO::getDictCode));
                listMap.forEach((decCode, dictItems) -> {
                    if(CollectionUtils.isNotEmpty(dictItems)){
                        Map<String, String> map = null;
                        if (NAME_CODE.equals(type)) {
                            map = dictItems.stream().collect(Collectors.toMap(DictItemDTO::getDictItemName, DictItemDTO::getDictItemCode, (k1, k2) -> k1));
                        }else {
                            map = dictItems.stream().collect(Collectors.toMap(DictItemDTO::getDictItemCode, DictItemDTO::getDictItemName, (k1, k2) -> k1));

                        }
                        dicCodeNameMap.put(decCode,map);
                    }
                });
            }
        }
    }

    public static void getCurrencyAll(BaseClient baseClient, Map<String, String> currencyCodeName,String type) {
        if(null == baseClient){
            baseClient = getBaseClient();
        }
        List<PurchaseCurrency> purchaseCurrencies = baseClient.listAllPurchaseCurrency();
        if(CollectionUtils.isNotEmpty(purchaseCurrencies)){
            Map<String,String> map ;
            if (NAME_CODE.equals(type)) {
                map = purchaseCurrencies.stream().collect(Collectors.toMap(PurchaseCurrency::getCurrencyName, PurchaseCurrency::getCurrencyCode, (k1, k2) -> k1));
            }else {
                map = purchaseCurrencies.stream().collect(Collectors.toMap(PurchaseCurrency::getCurrencyCode, PurchaseCurrency::getCurrencyName, (k1, k2) -> k1));
            }
            currencyCodeName.putAll(map);
        }
    }

    public static <T> List<T> readExcelWithModel(Class<T> clazz, InputStream fileInputStream) throws IOException {
        AnalysisEventListenerImpl<T> listener = new AnalysisEventListenerImpl();
        ExcelReader excelReader = EasyExcel.read(fileInputStream, clazz, listener).build();
        ReadSheet readSheet = EasyExcel.readSheet(0).build();
        excelReader.read(readSheet);
        excelReader.finish();
        return listener.getDatas();
    }

    /**
     * ????????????
     *
     * @param response
     * @param dataList ????????????
     * @param head     ??????
     * @param fileName ?????????
     * @Param width    ??????  (??????15)
     * @throws IOException
     */
    public static void exportStart(HttpServletResponse response, List<List<Object>> dataList, List<String> head, String fileName,int width) throws IOException {
        // ???????????????
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileName);
        // ????????????
        writeExcelWithModel(outputStream,dataList,head,fileName,new CustomizeExportHandler(width));
    }

    /**
     * ????????????????????????
     * @param list
     * @return <T>
     * @throws IOException
     */
    public static <T> void exportDicChange(List<T> list) {
        if(CollectionUtils.isNotEmpty(list)){
            // ????????????????????????
            Map<String, Map<String,String>> dicMap = new HashMap<>(16);
            Class<?> aClass = list.get(0).getClass();
            Field[] fields = aClass.getDeclaredFields();
            // ???ExcelParamCheck???????????????
            List<Field> fieldsList = new ArrayList<>();

            if(!ObjectUtils.isEmpty(fields)){
                Arrays.stream(fields).forEach(field -> {
                    // ??????????????????
                    ExcelParamCheck excelParamCheck = field.getAnnotation(ExcelParamCheck.class);
                    if(null != excelParamCheck){
                        // ????????????
                        String dicCode = excelParamCheck.dicCode();
                        if (!ObjectUtils.isEmpty(dicCode)) {
                            Map<String, String> dicCodeName = getDicCodeName(dicCode);
                            dicMap.put(dicCode,dicCodeName);
                            fieldsList.add(field);
                        }
                    }
                });

                // ?????? (??????-??????)
                Map<String, String> currencyCodeName = getCurrencyCodeName();

                if (CollectionUtils.isNotEmpty(fieldsList)) {
                    list.forEach(obj -> {
                        try {
                            for(Field field : fieldsList){
                                field.setAccessible(true);
                                Object value = field.get(obj);
                                if(!ObjectUtils.isEmpty(value)){
                                    ExcelParamCheck annotation = field.getAnnotation(ExcelParamCheck.class);
                                    String dicCode = annotation.dicCode(); // ????????????
                                    String valueStr = value.toString().trim(); // ??????????????????
                                    // ???????????????
                                    if(!ObjectUtils.isEmpty(dicCode)){
                                        Map<String, String> nameCodeMap = dicMap.get(dicCode);
                                        String dicName = nameCodeMap.get(valueStr);
                                        if(!ObjectUtils.isEmpty(dicName)){
                                            field.set(obj,dicName);
                                        }
                                    }
                                    // ????????????
                                    boolean ifCurrency = annotation.isCurrency();
                                    if(ifCurrency){
                                        String currencyName = currencyCodeName.get(valueStr);
                                        if(!ObjectUtils.isEmpty(currencyName)){
                                            field.set(obj,currencyName);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            String errorMsg = ExceptionUtil.getErrorMsg(e);
                            log.error("????????????????????????????????????????????????"+errorMsg);
                            throw new BaseException("????????????????????????????????????!");
                        }
                    });
                }
            }
        }
    }

    /**
     * ???????????????????????????????????????
     * <<??????: ????????????????????????: errorMsg>></??????:>
     * @param list
     * @param errorFlag
     * @return <T>
     * @throws IOException
     */
    public static <T> void checkParamNoNullAndDic(List<T> list, AtomicBoolean errorFlag) {
        if(CollectionUtils.isNotEmpty(list)){
            // ????????????????????????
            Map<String, Map<String,String>> dicMap = new HashMap<>(16);
            Class<?> aClass = list.get(0).getClass();
            Field[] fields = aClass.getDeclaredFields();
            // ???ExcelParamCheck???????????????
            List<Field> fieldsList = new ArrayList<>();

            if(!ObjectUtils.isEmpty(fields)){
                AtomicBoolean existsCurrency = new AtomicBoolean(false);
                AtomicBoolean isCurrencyName = new AtomicBoolean(false);
                Arrays.stream(fields).forEach(field -> {
                    // ??????????????????
                    ExcelParamCheck excelParamCheck = field.getAnnotation(ExcelParamCheck.class);
                    if(null != excelParamCheck){
                        // ????????????
                        String dicCode = excelParamCheck.dicCode();
                        if (!ObjectUtils.isEmpty(dicCode)) {
                            Map<String, String> dicNameCode = getDicNameCode(dicCode);
                            dicMap.put(dicCode,dicNameCode);
                        }
                        fieldsList.add(field);
                        // ???????????????
                        if(excelParamCheck.isCurrency()) {
                            existsCurrency.set(true);
                            // ????????????????????????
                            if(excelParamCheck.isCurrencyName()){
                                isCurrencyName.set(true);
                            }
                        }
                    }
                });

                Map<String, String> currencyMap = new HashMap<String, String>(16);
                if(existsCurrency.get()){
                    if(isCurrencyName.get()){
                        currencyMap = getCurrencyNameCode();
                    }else {
                        currencyMap = getCurrencyCodeName();
                    }
                }

                if (CollectionUtils.isNotEmpty(fieldsList)) {
                    Map<String, String> finalCurrencyMap = currencyMap;
                    list.forEach(obj -> {
                        StringBuffer errorMsg = new StringBuffer();
                        try {
                            for(Field field : fieldsList){
                                field.setAccessible(true);
                                Object value = field.get(obj);
                                ExcelParamCheck annotation = field.getAnnotation(ExcelParamCheck.class);
                                String dicCode = annotation.dicCode(); // ????????????
                                boolean ifRequired = annotation.ifRequired(); // ????????????
                                boolean isCurrency = annotation.isCurrency(); // ???????????????
                                if(ObjectUtils.isEmpty(value)){
                                    if(ifRequired){
                                        if(ObjectUtils.isEmpty(value)){
                                            String localeMsg = LocaleHandler.getLocaleMsg(annotation.errorMsg());
                                            errorMsg.append(localeMsg+" ;");
                                            errorFlag.set(true);
                                        }
                                    }
                                }else {
                                    String valueStr = value.toString().trim();
                                    // ???????????????
                                    if(!ObjectUtils.isEmpty(dicCode)){
                                        Map<String, String> nameCodeMap = dicMap.get(dicCode);
                                        String code = nameCodeMap.get(valueStr);
                                        if(!ObjectUtils.isEmpty(code)){
                                            field.set(obj,code);
                                        }else {
                                            String localeMsg = LocaleHandler.getLocaleMsg(annotation.dicErrorMsg());
                                            errorMsg.append(localeMsg+" ;");
                                            errorFlag.set(true);
                                        }
                                    }
                                    // ????????????
                                    if(isCurrency){
                                        String currency = finalCurrencyMap.get(valueStr);
                                        if(!ObjectUtils.isEmpty(currency)){
                                            field.set(obj,currency);
                                        }else {
                                            String localeMsg = LocaleHandler.getLocaleMsg(annotation.currencyErrorMsg());
                                            errorMsg.append(localeMsg+" ;");
                                            errorFlag.set(true);
                                        }
                                    }
                                }
                            }
                            Field field = aClass.getDeclaredField("errorMsg");
                            field.setAccessible(true);
                            if(errorMsg.length() > 0){
                                field.set(obj,errorMsg.toString());
                            }else {
                                field.set(obj,null);
                            }
                        } catch (Exception e) {
                            log.error("????????????????????????????????????????????????"+e.getMessage());
                            log.error("????????????????????????????????????????????????"+e);
                            throw new BaseException("????????????????????????????????????????????????!");
                        }
                    });
                }
            }
        }
    }

    /**
     * ??????????????????????????????, ???????????????????????????
     * @param list
     * @param aClass
     * @param errorFlag
     * @param <T>
     * @param <V>
     * @return
     */
    public static <T,V> List<V> dataTransform(List<T> list,Class<V> aClass,AtomicBoolean errorFlag){
        List<V> vList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list)) {
            // ???????????????
            Field[] businessFields = aClass.getDeclaredFields();
            // ?????????????????????
            Class importClass = list.get(0).getClass();
            Field[] importFields = importClass.getDeclaredFields();
            if(!ObjectUtils.isEmpty(businessFields) && !ObjectUtils.isEmpty(importFields)){
                // ????????????
                Map<String, Field> businessFieldMap = Arrays.stream(businessFields).collect(Collectors.toMap(Field::getName, Function.identity()));
                // ??????????????????
                Map<String, Field> importFieldMap = Arrays.stream(importFields).collect(Collectors.toMap(Field::getName, Function.identity()));

                for(T obj:list){
                    // ??????????????????
                    StringBuffer errorMsg = new StringBuffer();
                    V businessInstance = null;
                    try {
                        businessInstance = aClass.newInstance();
                    } catch (Exception e) {
                        log.error("?????????????????????"+e.getMessage());
                        log.error("?????????????????????"+e);
                    }
                    if (null != businessInstance) {
                        V finalBusinessInstance = businessInstance;
                        businessFieldMap.forEach((fieldName, field) -> {
                            field.setAccessible(true);
                            // ???????????????
                            Field importField = importFieldMap.get(fieldName);
                            if(null != importField){
                                String typeName = field.getType().getSimpleName();
                                Object value = null;
                                try {
                                    // ??????????????????
                                    importField.setAccessible(true);
                                    value = importField.get(obj);
                                    if (!ObjectUtils.isEmpty(value)) {
                                        String valueStr = value.toString();
                                        switch (typeName){
                                            case "Long":
                                                long aLong = Long.parseLong(valueStr);
                                                field.set(finalBusinessInstance,aLong);
                                                break;
                                            case "Integer":
                                                int i1 = Integer.parseInt(valueStr);
                                                field.set(finalBusinessInstance,i1);
                                                break;
                                            case "BigDecimal":
                                                BigDecimal decimal = new BigDecimal(valueStr);
                                                field.set(finalBusinessInstance,decimal);
                                                break;
                                            case "Date":
                                                Date date = DateUtil.parseDate(valueStr);
                                                field.set(finalBusinessInstance,date);
                                                break;
                                            case "LocalDate":
                                                Date date1 = DateUtil.parseDate(valueStr);
                                                LocalDate localDate = DateUtil.dateToLocalDate(date1);
                                                field.set(finalBusinessInstance,localDate);
                                                break;
                                            case "LocalDateTime":
                                                Date date2 = DateUtil.parseDate(valueStr);
                                                LocalDateTime localDateTime = DateUtil.dateToLocalDateTime(date2);
                                                field.set(finalBusinessInstance,localDateTime);
                                                break;
                                            case "Double":
                                                double aDouble = Double.parseDouble(valueStr);
                                                field.set(finalBusinessInstance,aDouble);
                                                break;
                                            default:
                                                field.set(finalBusinessInstance,valueStr);
                                        }
                                    }
                                } catch (Exception e) {
                                    ExcelParamCheck annotation = importField.getAnnotation(ExcelParamCheck.class);
                                    String formatErrorMsg = "??????????????????";
                                    if(null != annotation){
//                                        formatErrorMsg = annotation.formatErrorMsg();
                                        formatErrorMsg = LocaleHandler.getLocaleMsg(annotation.formatErrorMsg());
                                    }
                                    errorMsg.append(formatErrorMsg+" ;");
                                    errorFlag.set(true);
                                }
                            }
                        });
                        vList.add((V) finalBusinessInstance);
                        try {
                            Field errorMsgField = importClass.getDeclaredField("errorMsg");
                            errorMsgField.setAccessible(true);
                            if(errorMsg.length() > 0){
                                errorMsgField.set(obj,errorMsg.toString());
                            }else {
                                errorMsgField.set(obj,null);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
        return vList;
    }

    public static synchronized BaseClient getBaseClient(){
        if(null == baseClient){
            baseClient = SpringContextHolder.getBean(BaseClient.class);
        }
        return baseClient;
    }
}
