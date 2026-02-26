import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;

public class NovellLdapMoveCN {

    private static final String LDAP_HOST     = "ldap.example.com";
    private static final int    LDAP_PORT     = 389;

    private static final String BIND_DN       = "cn=admin,dc=example,dc=com";
    private static final String BIND_PASSWORD = "adminSecret";

    private static final String CN_VALUE      = "jdoe";
    private static final String DN_A          = "ou=contractors,dc=example,dc=com";
    private static final String DN_B          = "ou=employees,dc=example,dc=com";

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || (!args[0].equals("forward") && !args[0].equals("back"))) {
            System.out.println("Usage: NovellLdapMoveCN <forward|back>");
            System.out.println("  forward  ->  moves CN from DN_A to DN_B");
            System.out.println("  back     ->  moves CN from DN_B to DN_A");
            System.exit(1);
        }

        boolean isForward  = args[0].equals("forward");
        String sourceDN    = "cn=" + CN_VALUE + "," + (isForward ? DN_A : DN_B);
        String targetParent = isForward ? DN_B : DN_A;
        String newRDN      = "cn=" + CN_VALUE;

        LDAPConnection conn = new LDAPConnection();

        try {
            conn.connect(LDAP_HOST, LDAP_PORT);
            conn.bind(LDAPConnection.LDAP_V3, BIND_DN, BIND_PASSWORD.getBytes("UTF8"));

            System.out.println("Moving entry...");
            System.out.println("  From : " + sourceDN);
            System.out.println("  To   : " + newRDN + "," + targetParent);

            conn.rename(sourceDN, newRDN, targetParent, true);

            System.out.println("Success! Entry moved to: " + newRDN + "," + targetParent);

        } catch (LDAPException e) {
            System.err.println("LDAP Error: " + e.getMessage());
            System.err.println("Result Code: " + e.getResultCode());
            System.err.println("Matched DN : " + e.getMatchedDN());
            e.printStackTrace();
        } finally {
            if (conn.isConnected()) {
                try {
                    conn.disconnect();
                    System.out.println("Disconnected.");
                } catch (LDAPException e) {
                    System.err.println("Error disconnecting: " + e.getMessage());
                }
            }
        }
    }
}
