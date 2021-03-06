package com.midea.cloud.common.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.Sheet;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.util.FileUtils;
import com.alibaba.excel.write.builder.ExcelWriterSheetBuilder;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.handler.CellColorSheetWriteHandler;
import com.midea.cloud.common.handler.SpinnerLongHandler;
import com.midea.cloud.common.handler.TitleColorSheetWriteHandler;
import com.midea.cloud.common.handler.TitleHandler;
import com.midea.cloud.common.listener.AnalysisEventListenerImpl;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.model.base.dict.dto.DictItemDTO;
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
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class EasyExcelUtilNotAutoSetWidth {
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

    private static Sheet initSheet;

    static {
        initSheet = new Sheet(1, 0);
        initSheet.setSheetName("sheet");
        //?????????????????????
        initSheet.setAutoWidth(Boolean.TRUE);
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
        FileOutputStream outputStream = new FileOutputStream("D:/2.xlsx");
        ExcelWriter excelWriter = EasyExcel.write(outputStream).build();
        /**
         * ????????????????????????
         * ??????: ???????????????30000?????????, ?????????????????????10000?????????????????????(??????????????????????????????), ??????3???
         */
        for(int i = 0 ; i < 3 ; i++){
            WriteSheet writeSheet = EasyExcel.writerSheet(i, "sheet" + i).head(TestVO.class).
                    registerWriteHandler(new LongestMatchColumnWidthStyleStrategy()).build();
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
     * ?????????????????????
     */
    @Test
    public void testExport() throws Exception {
        // ?????????
        OutputStream outputStream = new FileOutputStream(new File("D:/??????excel.xlsx"));

        // ???????????????
        List<TestVO> dataList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TestVO testVO = new TestVO();
            testVO.setAge(i + 20);
            testVO.setName("vo" + i);
            testVO.setSchool("school" + i);
            dataList.add(testVO);
        }

        // ?????????????????????
        List<Integer> rows = Arrays.asList(0);
        // ?????????????????????
        List<Integer> columns = Arrays.asList(1, 2);
        TitleColorSheetWriteHandler titleColorSheetWriteHandler = new TitleColorSheetWriteHandler(rows, columns, IndexedColors.RED.index);
        writeExcelWithModel(outputStream, dataList, TestVO.class, "sheetName", titleColorSheetWriteHandler);
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
        ExcelWriter writer = EasyExcel.write(outputStream, clazz).build();
        //??????writeSheet?????????????????????
        WriteSheet writeSheet = new WriteSheet();
        writeSheet.setSheetName(sheetName);
        writer.write(dataList, writeSheet);
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
            EasyExcel.write(outputStream).head(list).sheet(sheetName).doWrite(lineList);
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
        Sheet sheet = initSheet;

        if (head != null) {
            List<List<String>> list = new ArrayList<>();
            head.forEach(h -> list.add(Collections.singletonList(h)));
            sheet.setHead(list);
            sheet.setAutoWidth(Boolean.TRUE);
        }

        ExcelWriter writer = null;
        try {
            writer = EasyExcelFactory.getWriter(outputStream);
            writer.write1(data, sheet);
        } catch (Exception e) {
            log.error("??????????????????" + e);
        } finally {
            try {
                if (writer != null) {
                    writer.finish();
                }

                if (outputStream != null) {
                    outputStream.close();
                }

            } catch (IOException e) {
                log.error("excel??????????????????, ???????????????{}", e);
            }
        }

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
        ServletOutputStream outputStream = EasyExcelUtilNotAutoSetWidth.getServletOutputStream(response, fileName);
        // ????????????
        EasyExcelUtilNotAutoSetWidth.writeExcelWithModel(outputStream, fileName, head, dataList);
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
        if (!EasyExcelUtilNotAutoSetWidth.isExcel(fileName)) {
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
    public static void writeExcelWithModel(OutputStream outputStream, List<? extends Object> dataList, List<String> headList, String sheetName, CellWriteHandler... cellWriteHandlers) {
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
        Map<String, String> divisionMap = new HashMap<>();
        if (StringUtil.notEmpty(code)) {
            List<DictItemDTO> division = baseClient.listAllByDictCode(code);
            if (CollectionUtils.isNotEmpty(division)) {
                divisionMap = division.stream().collect(Collectors.toMap(DictItemDTO::getDictItemName, DictItemDTO::getDictItemCode, (k1, k2) -> k1));
            }
        }
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
        if (StringUtil.notEmpty(code)) {
            List<DictItemDTO> division = baseClient.listAllByDictCode(code);
            if (CollectionUtils.isNotEmpty(division)) {
                divisionMap = division.stream().collect(Collectors.toMap(DictItemDTO::getDictItemCode, DictItemDTO::getDictItemName, (k1, k2) -> k1));
            }
        }
        return divisionMap;
    }

    public static <T> List<T> readExcelWithModel(Class<T> clazz, InputStream fileInputStream) throws IOException {
        AnalysisEventListenerImpl<T> listener = new AnalysisEventListenerImpl();
        ExcelReader excelReader = EasyExcel.read(fileInputStream, clazz, listener).build();
        ReadSheet readSheet = EasyExcel.readSheet(0).build();
        excelReader.read(readSheet);
        excelReader.finish();
        return listener.getDatas();
    }

}
