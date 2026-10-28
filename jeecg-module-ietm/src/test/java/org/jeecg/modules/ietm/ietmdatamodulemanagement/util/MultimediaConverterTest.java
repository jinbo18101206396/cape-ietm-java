package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/**
 * MultimediaConverter 单元测试
 * 测试覆盖：XML↔Object转换、multimediaType检测、中英文转换、XSS防护
 */
public class MultimediaConverterTest {

    // ============ xmlToMultimedia() 测试 ============

    @Test
    public void testXmlToMultimedia_Complete() throws Exception {
        String xml = "<multimedia id=\"mm-001\">\n" +
                     "<title>测试视频</title>\n" +
                     "<multimediaObject xlink:type=\"simple\" xlink:show=\"replace\" " +
                     "xlink:actuate=\"onRequest\" infoEntityIdent=\"ICN-001\" multimediaType=\"video\">\n" +
                     "<hotspot applicationStructureIdent=\"aps-001\" " +
                     "applicationStructureName=\"引擎\" id=\"hs-001\"/>\n" +
                     "</multimediaObject>\n" +
                     "</multimedia>";

        MultimediaResponseVO result = MultimediaConverter.xmlToMultimedia(xml);

        assertNotNull(result);
        assertEquals("测试视频", result.getTitle());
        assertEquals("mm-001", result.getId());
        assertEquals(1, result.getMultimediaObjects().size());

        MultimediaObjectItemVO obj = result.getMultimediaObjects().get(0);
        assertEquals("ICN-001", obj.getIcn());
        assertEquals("video", obj.getMultimediaType());
        assertEquals(1, obj.getHotspots().size());

        HotspotItemVO hs = obj.getHotspots().get(0);
        assertEquals("aps-001", hs.getApsid());
        assertEquals("引擎", hs.getName());
        assertEquals("hs-001", hs.getId());
    }

    @Test
    public void testXmlToMultimedia_WithoutId() throws Exception {
        String xml = "<multimedia>\n" +
                     "<title>简单多媒体</title>\n" +
                     "</multimedia>";

        MultimediaResponseVO result = MultimediaConverter.xmlToMultimedia(xml);
        assertEquals("简单多媒体", result.getTitle());
        assertEquals("", result.getId());
        assertEquals(0, result.getMultimediaObjects().size());
    }

    @Test
    public void testXmlToMultimedia_MultipleObjects() throws Exception {
        String xml = "<multimedia id=\"mm-002\">\n" +
                     "<title>多对象测试</title>\n" +
                     "<multimediaObject xlink:type=\"simple\" infoEntityIdent=\"ICN-VIDEO-001\" multimediaType=\"video\"/>\n" +
                     "<multimediaObject xlink:type=\"simple\" infoEntityIdent=\"ICN-3D-001\" multimediaType=\"3D\"/>\n" +
                     "<multimediaObject xlink:type=\"simple\" infoEntityIdent=\"ICN-CGM-001\"/>\n" +
                     "</multimedia>";

        MultimediaResponseVO result = MultimediaConverter.xmlToMultimedia(xml);
        assertEquals(3, result.getMultimediaObjects().size());
        assertEquals("video", result.getMultimediaObjects().get(0).getMultimediaType());
        assertEquals("3D", result.getMultimediaObjects().get(1).getMultimediaType());
        assertEquals("", result.getMultimediaObjects().get(2).getMultimediaType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testXmlToMultimedia_EmptyString() throws Exception {
        MultimediaConverter.xmlToMultimedia("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testXmlToMultimedia_Null() throws Exception {
        MultimediaConverter.xmlToMultimedia(null);
    }

    // ============ multimediaToXml() 测试 ============

    @Test
    public void testMultimediaToXml_Complete() {
        MultimediaResponseVO multimedia = new MultimediaResponseVO();
        multimedia.setTitle("测试多媒体");
        multimedia.setId("mm-001");

        MultimediaObjectItemVO obj = new MultimediaObjectItemVO();
        obj.setIcn("ICN-VIDEO-001");
        obj.setFilename("video.mp4");
        obj.setMultimediaType("video");
        obj.setHotspots(new ArrayList<>());

        multimedia.setMultimediaObjects(Arrays.asList(obj));

        String xml = MultimediaConverter.multimediaToXml(multimedia);

        assertTrue(xml.contains("<multimedia id=\"mm-001\">"));
        assertTrue(xml.contains("<title>测试多媒体</title>"));
        assertTrue(xml.contains("infoEntityIdent=\"ICN-VIDEO-001\""));
        assertTrue(xml.contains("multimediaType=\"video\""));
        assertTrue(xml.contains("xlink:type=\"simple\""));
        assertTrue(xml.contains("xlink:show=\"replace\""));
        assertTrue(xml.contains("xlink:actuate=\"onRequest\""));
    }

    @Test
    public void testMultimediaToXml_WithoutId() {
        MultimediaResponseVO multimedia = new MultimediaResponseVO();
        multimedia.setTitle("简单多媒体");
        multimedia.setId("");
        multimedia.setMultimediaObjects(new ArrayList<>());

        String xml = MultimediaConverter.multimediaToXml(multimedia);
        assertTrue(xml.contains("<multimedia>"));
        assertFalse(xml.contains("id="));
    }

    @Test
    public void testMultimediaToXml_AutoDetectType() {
        MultimediaResponseVO multimedia = new MultimediaResponseVO();
        multimedia.setTitle("自动检测");

        MultimediaObjectItemVO obj = new MultimediaObjectItemVO();
        obj.setIcn("ICN-001");
        obj.setFilename("test.mp4");
        obj.setMultimediaType("");
        obj.setHotspots(new ArrayList<>());

        multimedia.setMultimediaObjects(Arrays.asList(obj));

        String xml = MultimediaConverter.multimediaToXml(multimedia);
        assertTrue(xml.contains("multimediaType=\"video\""));
    }

    @Test
    public void testMultimediaToXml_WithHotspots() {
        MultimediaResponseVO multimedia = new MultimediaResponseVO();
        multimedia.setTitle("热点测试");

        HotspotItemVO hs1 = new HotspotItemVO();
        hs1.setApsid("aps-001");
        hs1.setName("部件A");
        hs1.setId("hs-001");

        HotspotItemVO hs2 = new HotspotItemVO();
        hs2.setApsid("aps-002");
        hs2.setName("部件B");
        hs2.setId("");

        MultimediaObjectItemVO obj = new MultimediaObjectItemVO();
        obj.setIcn("ICN-CGM-001");
        obj.setFilename("diagram.cgm");
        obj.setMultimediaType("");
        obj.setHotspots(Arrays.asList(hs1, hs2));

        multimedia.setMultimediaObjects(Arrays.asList(obj));

        String xml = MultimediaConverter.multimediaToXml(multimedia);
        assertTrue(xml.contains("<hotspot applicationStructureIdent=\"aps-001\" applicationStructureName=\"部件A\" id=\"hs-001\"/>"));
        assertTrue(xml.contains("<hotspot applicationStructureIdent=\"aps-002\" applicationStructureName=\"部件B\"/>"));
        assertTrue(xml.contains("</multimediaObject>"));
    }

    @Test
    public void testMultimediaToXml_SelfClosingObject() {
        MultimediaResponseVO multimedia = new MultimediaResponseVO();
        multimedia.setTitle("无热点");

        MultimediaObjectItemVO obj = new MultimediaObjectItemVO();
        obj.setIcn("ICN-VIDEO-001");
        obj.setFilename("video.mp4");
        obj.setMultimediaType("video");
        obj.setHotspots(new ArrayList<>());

        multimedia.setMultimediaObjects(Arrays.asList(obj));

        String xml = MultimediaConverter.multimediaToXml(multimedia);
        assertTrue(xml.matches("(?s).*<multimediaObject[^>]+/>.*"));
    }

    @Test
    public void testMultimediaToXml_XmlEscaping() {
        MultimediaResponseVO multimedia = new MultimediaResponseVO();
        multimedia.setTitle("Title with <tag> & \"quotes\"");
        multimedia.setId("id-with-'apostrophe'");
        multimedia.setMultimediaObjects(new ArrayList<>());

        String xml = MultimediaConverter.multimediaToXml(multimedia);
        assertTrue(xml.contains("&lt;tag&gt;"));
        assertTrue(xml.contains("&amp;"));
        assertTrue(xml.contains("&quot;"));
        assertTrue(xml.contains("&apos;"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMultimediaToXml_Null() {
        MultimediaConverter.multimediaToXml(null);
    }

    // ============ detectMultimediaType() 测试 ============

    @Test
    public void testDetectMultimediaType_Video() {
        assertEquals("video", MultimediaConverter.detectMultimediaType("video.mp4"));
        assertEquals("video", MultimediaConverter.detectMultimediaType("video.avi"));
        assertEquals("video", MultimediaConverter.detectMultimediaType("video.wmv"));
        assertEquals("video", MultimediaConverter.detectMultimediaType("video.mov"));
        assertEquals("video", MultimediaConverter.detectMultimediaType("video.rm"));
        assertEquals("video", MultimediaConverter.detectMultimediaType("video.mpg"));
        assertEquals("video", MultimediaConverter.detectMultimediaType("video.webm"));
    }

    @Test
    public void testDetectMultimediaType_Audio() {
        assertEquals("video", MultimediaConverter.detectMultimediaType("audio.mp3"));
    }

    @Test
    public void testDetectMultimediaType_3D() {
        assertEquals("3D", MultimediaConverter.detectMultimediaType("model.wrl"));
        assertEquals("3D", MultimediaConverter.detectMultimediaType("model.smg"));
        assertEquals("3D", MultimediaConverter.detectMultimediaType("model.swf"));
    }

    @Test
    public void testDetectMultimediaType_Image() {
        assertEquals("", MultimediaConverter.detectMultimediaType("image.cgm"));
        assertEquals("", MultimediaConverter.detectMultimediaType("image.jpg"));
        assertEquals("", MultimediaConverter.detectMultimediaType("image.png"));
        assertEquals("", MultimediaConverter.detectMultimediaType("image.bmp"));
        assertEquals("", MultimediaConverter.detectMultimediaType("image.gif"));
    }

    @Test
    public void testDetectMultimediaType_CaseInsensitive() {
        assertEquals("video", MultimediaConverter.detectMultimediaType("VIDEO.MP4"));
        assertEquals("3D", MultimediaConverter.detectMultimediaType("Model.WRL"));
        assertEquals("", MultimediaConverter.detectMultimediaType("Image.CGM"));
    }

    @Test
    public void testDetectMultimediaType_NoExtension() {
        assertEquals("", MultimediaConverter.detectMultimediaType("noextension"));
        assertEquals("", MultimediaConverter.detectMultimediaType(""));
        assertEquals("", MultimediaConverter.detectMultimediaType(null));
    }

    @Test
    public void testDetectMultimediaType_Unknown() {
        assertEquals("", MultimediaConverter.detectMultimediaType("file.unknown"));
        assertEquals("", MultimediaConverter.detectMultimediaType("document.pdf"));
    }

    // ============ 中英文转换测试 ============

    @Test
    public void testToCnMultimediaXml() {
        String enXml = "<multimedia id=\"mm-001\">\n" +
                       "<title>测试标题</title>\n" +
                       "<multimediaObject xlink:type=\"simple\" infoEntityIdent=\"ICN-001\" multimediaType=\"video\">\n" +
                       "<hotspot applicationStructureIdent=\"aps-001\" applicationStructureName=\"部件A\" id=\"hs-001\"/>\n" +
                       "</multimediaObject>\n" +
                       "</multimedia>";

        String cnXml = MultimediaConverter.toCnMultimediaXml(enXml);

        assertTrue(cnXml.contains("<多媒体"));
        assertTrue(cnXml.contains("<标题>"));
        assertTrue(cnXml.contains("<多媒体对象"));
        assertTrue(cnXml.contains("<热点"));
        assertTrue(cnXml.contains("信息实体标识="));
        assertTrue(cnXml.contains("多媒体类型="));
        assertTrue(cnXml.contains("应用结构标识="));
        assertTrue(cnXml.contains("应用结构名称="));
    }

    @Test
    public void testToEnMultimediaXml() {
        String cnXml = "<多媒体 id=\"mm-001\">\n" +
                       "<标题>测试标题</标题>\n" +
                       "<多媒体对象 xlink:type=\"simple\" 信息实体标识=\"ICN-001\" 多媒体类型=\"video\">\n" +
                       "<热点 应用结构标识=\"aps-001\" 应用结构名称=\"部件A\" id=\"hs-001\"/>\n" +
                       "</多媒体对象>\n" +
                       "</多媒体>";

        String enXml = MultimediaConverter.toEnMultimediaXml(cnXml);

        assertTrue(enXml.contains("<multimedia"));
        assertTrue(enXml.contains("<title>"));
        assertTrue(enXml.contains("<multimediaObject"));
        assertTrue(enXml.contains("<hotspot"));
        assertTrue(enXml.contains("infoEntityIdent="));
        assertTrue(enXml.contains("multimediaType="));
        assertTrue(enXml.contains("applicationStructureIdent="));
        assertTrue(enXml.contains("applicationStructureName="));
    }

    @Test
    public void testRoundTripConversion_EnToCnToEn() {
        String enXml = "<multimedia id=\"mm-001\">\n" +
                       "<title>测试标题</title>\n" +
                       "<multimediaObject xlink:type=\"simple\" infoEntityIdent=\"ICN-001\" multimediaType=\"video\">\n" +
                       "<hotspot applicationStructureIdent=\"aps-001\" applicationStructureName=\"部件A\" id=\"hs-001\"/>\n" +
                       "</multimediaObject>\n" +
                       "</multimedia>";

        String cnXml = MultimediaConverter.toCnMultimediaXml(enXml);
        String backToEn = MultimediaConverter.toEnMultimediaXml(cnXml);

        assertEquals(enXml, backToEn);
    }

    @Test
    public void testRoundTripConversion_CnToEnToCn() {
        String cnXml = "<多媒体 id=\"mm-001\">\n" +
                       "<标题>测试标题</标题>\n" +
                       "<多媒体对象 xlink:type=\"simple\" 信息实体标识=\"ICN-001\" 多媒体类型=\"video\">\n" +
                       "<热点 应用结构标识=\"aps-001\" 应用结构名称=\"部件A\" id=\"hs-001\"/>\n" +
                       "</多媒体对象>\n" +
                       "</多媒体>";

        String enXml = MultimediaConverter.toEnMultimediaXml(cnXml);
        String backToCn = MultimediaConverter.toCnMultimediaXml(enXml);

        assertEquals(cnXml, backToCn);
    }

    @Test
    public void testConversion_NullInput() {
        assertNull(MultimediaConverter.toCnMultimediaXml(null));
        assertNull(MultimediaConverter.toEnMultimediaXml(null));
    }
}
