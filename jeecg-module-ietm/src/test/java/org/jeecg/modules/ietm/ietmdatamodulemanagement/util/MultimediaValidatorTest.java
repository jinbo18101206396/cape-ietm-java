package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.*;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.MultimediaValidator.ValidationResult;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/**
 * MultimediaValidator 单元测试
 */
public class MultimediaValidatorTest {

    @Test
    public void testValidateMultimedia_Valid() {
        MultimediaResponseVO multimedia = new MultimediaResponseVO();
        multimedia.setTitle("有效多媒体");
        MultimediaObjectItemVO obj = new MultimediaObjectItemVO();
        obj.setIcn("ICN-001");
        obj.setHotspots(new ArrayList<>());
        multimedia.setMultimediaObjects(Arrays.asList(obj));
        ValidationResult result = MultimediaValidator.validateMultimedia(multimedia);
        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void testValidateMultimedia_Null() {
        ValidationResult result = MultimediaValidator.validateMultimedia(null);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("multimedia对象不能为空"));
    }

    @Test
    public void testValidateMultimedia_EmptyTitle() {
        MultimediaResponseVO multimedia = new MultimediaResponseVO();
        multimedia.setTitle("");
        MultimediaObjectItemVO obj = new MultimediaObjectItemVO();
        obj.setIcn("ICN-001");
        multimedia.setMultimediaObjects(Arrays.asList(obj));
        ValidationResult result = MultimediaValidator.validateMultimedia(multimedia);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Multimedia标题必填"));
    }

    @Test
    public void testValidateMultimedia_TitleTooLong() {
        MultimediaResponseVO multimedia = new MultimediaResponseVO();
        StringBuilder longTitle = new StringBuilder();
        for (int i = 0; i < 201; i++) longTitle.append("a");
        multimedia.setTitle(longTitle.toString());
        MultimediaObjectItemVO obj = new MultimediaObjectItemVO();
        obj.setIcn("ICN-001");
        multimedia.setMultimediaObjects(Arrays.asList(obj));
        ValidationResult result = MultimediaValidator.validateMultimedia(multimedia);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Multimedia标题长度不能超过200个字符"));
    }

    @Test
    public void testValidateMultimedia_EmptyObjects() {
        MultimediaResponseVO multimedia = new MultimediaResponseVO();
        multimedia.setTitle("标题");
        multimedia.setMultimediaObjects(new ArrayList<>());
        ValidationResult result = MultimediaValidator.validateMultimedia(multimedia);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("至少需要添加一个多媒体对象"));
    }

    @Test
    public void testValidateMultimedia_EmptyIcn() {
        MultimediaResponseVO multimedia = new MultimediaResponseVO();
        multimedia.setTitle("标题");
        MultimediaObjectItemVO obj = new MultimediaObjectItemVO();
        obj.setIcn("");
        obj.setHotspots(new ArrayList<>());
        multimedia.setMultimediaObjects(Arrays.asList(obj));
        ValidationResult result = MultimediaValidator.validateMultimedia(multimedia);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("第1个多媒体对象的ICN编码不能为空"));
    }

    @Test
    public void testValidateHotspots_Valid() {
        HotspotItemVO hs = new HotspotItemVO();
        hs.setApsid("aps-001");
        hs.setName("部件A");
        hs.setId("hs-001");
        ValidationResult result = MultimediaValidator.validateHotspots(Arrays.asList(hs), 0);
        assertTrue(result.isValid());
    }

    @Test
    public void testValidateHotspots_EmptyApsid() {
        HotspotItemVO hs = new HotspotItemVO();
        hs.setApsid("");
        hs.setName("部件A");
        ValidationResult result = MultimediaValidator.validateHotspots(Arrays.asList(hs), 0);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("第1个对象的第1个热点的应用结构标识(apsid)不能为空"));
    }

    @Test
    public void testValidateHotspots_EmptyName() {
        HotspotItemVO hs = new HotspotItemVO();
        hs.setApsid("aps-001");
        hs.setName("");
        ValidationResult result = MultimediaValidator.validateHotspots(Arrays.asList(hs), 0);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("第1个对象的第1个热点的名称不能为空"));
    }
}
