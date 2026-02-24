import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;

public class NovellLdapMoveCN {

    private static final String LDAP_HOST     = "ldap.example.com";
    private static final int    LDAP_PORT     = 389;

    private static final String BIND_DN       = "cn=admin,dc=example,dc=com";
    private static final String BIND_PASSWORD = "adminSecret";

    private static final String CN_VALUE      = "jdoe";
    private static final String SOURCE_OU_DN  = "ou=contractors,dc=example,dc=com";
    private static final String TARGET_OU_DN  = "ou=employees,dc=example,dc=com";

    public static void main(String[] args) {

        String currentDN = "cn=" + CN_VALUE + "," + SOURCE_OU_DN;
        String newRDN    = "cn=" + CN_VALUE;

        LDAPConnection conn = new LDAPConnection();

        try {
            conn.connect(LDAP_HOST, LDAP_PORT);
            conn.bind(LDAPConnection.LDAP_V3, BIND_DN, BIND_PASSWORD.getBytes("UTF8"));

            System.out.println("Moving entry...");
            System.out.println("  From : " + currentDN);
            System.out.println("  To   : " + newRDN + "," + TARGET_OU_DN);

            conn.rename(currentDN, newRDN, TARGET_OU_DN, true);

            System.out.println("Success! Entry moved to: " + newRDN + "," + TARGET_OU_DN);

        } catch (LDAPException e) {
            System.err.println("LDAP Error: " + e.getMessage());
            System.err.println("Result Code: " + e.getResultCode());
            System.err.println("Matched DN : " + e.getMatchedDN());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("General Error: " + e.getMessage());
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
