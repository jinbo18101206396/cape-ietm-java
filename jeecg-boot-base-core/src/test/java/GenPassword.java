import org.jeecg.common.util.PasswordUtil;

public class GenPassword {
    public static void main(String[] args) {
        // encrypt(username, password, salt)
        // Returns encrypted password for: admin / 123456
        String enc = PasswordUtil.encrypt("admin", "123456", "RCGTeGiH");
        System.out.println("Hash: " + enc);

        // Also try the default salt from the util class
        String enc2 = PasswordUtil.encrypt("admin", "123456", "63293188");
        System.out.println("Hash (static salt): " + enc2);
    }
}
