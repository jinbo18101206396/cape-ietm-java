package org.jeecg.modules.ietm.icnmanage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * determinePreviewType 方法单元测试
 * 验证后端P0修复：新增7种3D格式识别
 */
@SpringBootTest
public class DeterminePreviewTypeTest {

    /**
     * 通过反射调用 private 方法
     */
    private String invokeDeterminePreviewType(String fileExt) throws Exception {
        // 实际项目中需要注入 IetmIcnManageServiceImpl
        // 这里仅作为测试示例，实际需要调整
        Class<?> clazz = Class.forName("org.jeecg.modules.ietm.icnmanage.service.impl.IetmIcnManageServiceImpl");
        Object instance = clazz.getDeclaredConstructor().newInstance();

        Method method = clazz.getDeclaredMethod("determinePreviewType", String.class);
        method.setAccessible(true);

        return (String) method.invoke(instance, fileExt);
    }

    @Test
    @DisplayName("测试VRML格式 - wrl应返回3D")
    void testVRMLFormat() throws Exception {
        assertEquals("3D", invokeDeterminePreviewType("wrl"));
        assertEquals("3D", invokeDeterminePreviewType("WRL"));
    }

    @Test
    @DisplayName("测试glTF格式 - gltf和glb应返回3D")
    void testGLTFFormats() throws Exception {
        assertEquals("3D", invokeDeterminePreviewType("gltf"));
        assertEquals("3D", invokeDeterminePreviewType("GLTF"));
        assertEquals("3D", invokeDeterminePreviewType("glb"));
        assertEquals("3D", invokeDeterminePreviewType("GLB"));
    }

    @Test
    @DisplayName("测试OBJ格式 - obj应返回3D")
    void testOBJFormat() throws Exception {
        assertEquals("3D", invokeDeterminePreviewType("obj"));
        assertEquals("3D", invokeDeterminePreviewType("OBJ"));
    }

    @Test
    @DisplayName("测试STL格式 - stl应返回3D")
    void testSTLFormat() throws Exception {
        assertEquals("3D", invokeDeterminePreviewType("stl"));
        assertEquals("3D", invokeDeterminePreviewType("STL"));
    }

    @Test
    @DisplayName("测试FBX格式 - fbx应返回3D")
    void testFBXFormat() throws Exception {
        assertEquals("3D", invokeDeterminePreviewType("fbx"));
        assertEquals("3D", invokeDeterminePreviewType("FBX"));
    }

    @Test
    @DisplayName("测试COLLADA格式 - dae应返回3D")
    void testColladaFormat() throws Exception {
        assertEquals("3D", invokeDeterminePreviewType("dae"));
        assertEquals("3D", invokeDeterminePreviewType("DAE"));
    }

    @Test
    @DisplayName("测试PLY格式 - ply应返回3D")
    void testPLYFormat() throws Exception {
        assertEquals("3D", invokeDeterminePreviewType("ply"));
        assertEquals("3D", invokeDeterminePreviewType("PLY"));
    }

    @Test
    @DisplayName("测试图片格式")
    void testImageFormats() throws Exception {
        assertEquals("IMAGE", invokeDeterminePreviewType("jpg"));
        assertEquals("IMAGE", invokeDeterminePreviewType("png"));
        assertEquals("IMAGE", invokeDeterminePreviewType("bmp"));
        assertEquals("IMAGE", invokeDeterminePreviewType("gif"));
    }

    @Test
    @DisplayName("测试其他格式")
    void testOtherFormats() throws Exception {
        assertEquals("CGM", invokeDeterminePreviewType("cgm"));
        assertEquals("VIDEO", invokeDeterminePreviewType("mp4"));
        assertEquals("AUDIO", invokeDeterminePreviewType("mp3"));
        assertEquals("FLASH", invokeDeterminePreviewType("swf"));
        assertEquals("SMG", invokeDeterminePreviewType("smg"));
    }

    @Test
    @DisplayName("测试不支持的格式")
    void testUnsupportedFormats() throws Exception {
        assertEquals("OTHER", invokeDeterminePreviewType("txt"));
        assertEquals("OTHER", invokeDeterminePreviewType("pdf"));
        assertEquals("OTHER", invokeDeterminePreviewType("doc"));
        assertEquals("OTHER", invokeDeterminePreviewType("xyz"));
    }

    @Test
    @DisplayName("测试空值和null")
    void testEmptyAndNull() throws Exception {
        assertEquals("OTHER", invokeDeterminePreviewType(null));
        assertEquals("OTHER", invokeDeterminePreviewType(""));
        assertEquals("OTHER", invokeDeterminePreviewType("   "));
    }
}
