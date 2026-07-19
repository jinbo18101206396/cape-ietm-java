package utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import java.io.*;
import java.security.Key;
import java.security.SecureRandom;

/**
 * @Description: TODO
 * @author: wangdong
 * @date: 2023年06月05日 13:12
 */
public class DESUtils {
    private static Key key;

    //设置秘钥
    private static String KEY_STR = "bzhgz";

    static {
        try {
            //生成des算法对象
            KeyGenerator generator = KeyGenerator.getInstance("DES");
            //采用SHA1安全策略
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            //设置密匙种子
            secureRandom.setSeed(KEY_STR.getBytes());
            generator.init(secureRandom);
            //生成密匙
            key = generator.generateKey();
            generator = null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 对字符串进行加密，返回BASE64的加密字符串
     * <功能详细描述>
     *
     * @param str
     * @return
     * @see [类、类#方法、类#成员]
     */
    public static String getjiami(String str) {

        BASE64Encoder base64Encoder = new BASE64Encoder();
        System.out.println(key);
        try {
            byte[] strBytes = str.getBytes("UTF-8");
            //获取加密对象
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptStrBytes = cipher.doFinal(strBytes);
            return base64Encoder.encode(encryptStrBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 对BASE64加密字符串进行解密
     * <功能详细描述>
     *
     * @param str
     * @return
     * @see [类、类#方法、类#成员]
     */
    public static String getjiemi(String str) {
        BASE64Decoder base64Decoder = new BASE64Decoder();
        try {    //将密码转化base64
            byte[] strBytes = base64Decoder.decodeBuffer(str);
            //初始化加密对象
            Cipher cipher = Cipher.getInstance("DES");
            //初始化加密信息按照
            cipher.init(Cipher.DECRYPT_MODE, key);
            //解密的得到数组
            byte[] encryptStrBytes = cipher.doFinal(strBytes);
            return new String(encryptStrBytes, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 文件加密
     *
     * @param inputs   文件的输入流
     * @param filePath 加密后文件的位置
     * @param realPath
     * @throws Exception
     */
    public static void encodeBase64File(InputStream inputs, String filePath, String realPath) throws Exception {
        BASE64Encoder base64En = new BASE64Encoder();
        FileOutputStream fo = null;
        FileOutputStream foreal = null;
        try {
            File temFile = new File(filePath);
            if (StringUtils.isNotEmpty(realPath)) {
                File realFile = new File(realPath);
                if (realFile.exists()) {
                    realFile.delete();
                }
            }
//            if(temFile.exists()){
//                temFile.delete();
//            }
            byte[] buffer = new byte[inputs.available()];
            inputs.read(buffer);
            fo = new FileOutputStream(filePath);
            String result = base64En.encode(buffer);
            fo.write(result.getBytes());
            fo.close();
            if (StringUtils.isNotBlank(realPath)) {
                foreal = new FileOutputStream(realPath);
                foreal.write(result.getBytes());
                foreal.close();
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("加密文件失败");
            fo.close();
            if (foreal != null) {
                foreal.close();
            }
        }
    }


    /**
     * 解密文件
     *
     * @param pathStr    加密文件所在的全路径
     * @param toFilename 输出的解密文件的 文件名
     * @return
     */
    public static boolean decodeBase64File(String pathStr, String toFilename) {
        File file = new File(pathStr);
        if (!file.exists()) {
            return false;
        }
        try {
            FileReader reader = new FileReader(file);
            BufferedReader bReader = new BufferedReader(reader);
            StringBuilder sb = new StringBuilder();
            String result = "";
            while ((result = bReader.readLine()) != null) {
                sb.append(result);
            }
            String str = sb.toString();
            bReader.close();
            reader.close();
            byte[] bytes = new BASE64Decoder().decodeBuffer(str);
            FileOutputStream out = new FileOutputStream(toFilename);
            out.write(bytes);
            out.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解密文件
     *
     * @param pathStr    加密文件所在的全路径
     * @return
     */
    public static byte[] decodeBase64Bytes(String pathStr) throws Exception {
        File file = new File(pathStr);
        if (!file.exists()) {
            return null;
        }
        try {
            FileReader reader = new FileReader(file);
            BufferedReader bReader = new BufferedReader(reader);
            StringBuilder sb = new StringBuilder();
            String result = "";
            while ((result = bReader.readLine()) != null) {
                sb.append(result);
            }
            String str = sb.toString();
            bReader.close();
            reader.close();
            byte[] bytes = new BASE64Decoder().decodeBuffer(str);
            return bytes;
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    /**
     * 流式解密文件（用于文件下载）
     *
     * @param encryptedInput 加密文件输入流
     * @param output         输出流
     * @param unused         保留参数（兼容旧接口）
     * @throws Exception 解密异常
     */
    public static void decodeBase64File(InputStream encryptedInput, OutputStream output, Object unused) throws Exception {
        try {
            // 读取加密内容
            BufferedReader reader = new BufferedReader(new InputStreamReader(encryptedInput));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            // Base64解密
            byte[] decodedBytes = new BASE64Decoder().decodeBuffer(sb.toString());

            // 写入输出流
            output.write(decodedBytes);
        } catch (Exception e) {
            throw new Exception("流式解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 新增：分段解密Base64加密的字节数组
     * @param srcBytes 待解密的字节数组
     * @param offset 起始偏移量
     * @param length 解密长度
     * @return 解密后的字节数组
     * @throws Exception 解密异常
     */
    public static byte[] decodeBase64Bytes(byte[] srcBytes, int offset, int length) throws Exception {
        // 1. 截取指定范围的字节
        byte[] targetBytes = new byte[length];
        System.arraycopy(srcBytes, offset, targetBytes, 0, length);
        return Base64.decodeBase64(targetBytes);
    }

    public static void encodeOneFiles(String path, String toPath, String filename) {
        BASE64Encoder base64En = new BASE64Encoder();
        try {
            File f = new File(path);
            FileInputStream inputFile = new FileInputStream(f);
            byte[] buffer = new byte[(int) f.length()];
            inputFile.read(buffer);
            inputFile.close();
            createDir(toPath);// 文件夹
            FileOutputStream fo = new FileOutputStream(toPath + "\\" + filename);
            String result = base64En.encode(buffer);
            fo.write(result.getBytes());
            fo.close();
        } catch (Exception e) {
            System.out.println("加密文件失败:" + e.getMessage());

        }
    }

    public static void encodeALLFiles(String path) {
        BASE64Encoder base64En = new BASE64Encoder();
        try {
            File file = new File(path);
            File[] fas = file.listFiles();
            for (int i = 0; i < fas.length; i++) {
                File f = fas[i];
                FileInputStream inputFile = new FileInputStream(f);
                byte[] buffer = new byte[(int) f.length()];
                inputFile.read(buffer);
                inputFile.close();
                createDir(path + "\\encod\\");// 文件夹
                FileOutputStream fo = new FileOutputStream(path + "\\encod\\" + f.getName());
                String result = base64En.encode(buffer);
                fo.write(result.getBytes());
                fo.close();
            }

        } catch (Exception e) {
            System.out.println("加密文件失败:" + e.getMessage());
        }
    }

    /**
     * 创建目录
     *
     * @param destDirName
     * @return
     */
    public static Boolean createDir(String destDirName) {
        File dir = new File(destDirName);
        if (!dir.exists()) {
            dir.mkdirs();// 不存在就全部创建
        }
        return false;
    }

    public static void main(String[] args) {
        encodeALLFiles("D:\\bzhgzResource\\file_resource\\standard_pdf_view");
        /*String name ="root";
        String password="sy601bzhgz";
        String encryname = getjiami(name);
        String encrypassword = getjiami(password);
        System.out.println(encryname);
        System.out.println(encrypassword);

        System.out.println(getjiemi(encryname));
        System.out.println(getjiemi(encrypassword));*/
    }


}

