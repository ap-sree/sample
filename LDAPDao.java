package com.sreemat.ldap.dao;

import com.novell.ldap.*;
import com.sreemat.ldap.constants.LdapConstants;
import java.util.ArrayList;
import java.util.List;

/**
 * LDAP Data Access Object for all LDAP operations
 */
public class LdapDAO {
    
    private static final String LDAP_HOST = "localhost";
    private static final int LDAP_PORT = 389;
    private static final String ADMIN_DN = "cn=admin," + LdapConstants.BASE_DN;
    private static final String ADMIN_PASSWORD = "admin_password";
    
    /**
     * Get LDAP connection
     */
    public LDAPConnection getConnection() throws LDAPException {
        LDAPConnection conn = new LDAPConnection();
        conn.connect(LDAP_HOST, LDAP_PORT);
        conn.bind(3, ADMIN_DN, ADMIN_PASSWORD.getBytes());
        return conn;
    }
    
    /**
     * Close LDAP connection
     */
    public void closeConnection(LDAPConnection conn) {
        try {
            if (conn != null && conn.isConnected()) {
                conn.disconnect();
            }
        } catch (LDAPException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Search entries in LDAP
     */
    public List<LDAPEntry> search(String baseDN, int scope, String filter) {
        List<LDAPEntry> entries = new ArrayList<>();
        LDAPConnection conn = null;
        
        try {
            conn = getConnection();
            LDAPSearchResults results = conn.search(baseDN, scope, filter, null, false);
            
            while (results.hasMore()) {
                entries.add(results.next());
            }
        } catch (LDAPException e) {
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
        
        return entries;
    }
    
    /**
     * Search single entry in LDAP
     */
    public LDAPEntry searchSingle(String baseDN, int scope, String filter) {
        LDAPConnection conn = null;
        
        try {
            conn = getConnection();
            LDAPSearchResults results = conn.search(baseDN, scope, filter, null, false);
            
            if (results.hasMore()) {
                return results.next();
            }
        } catch (LDAPException e) {
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
        
        return null;
    }
    
    /**
     * Add entry to LDAP
     */
    public boolean addEntry(LDAPEntry entry) {
        LDAPConnection conn = null;
        try {
            conn = getConnection();
            conn.add(entry);
            return true;
        } catch (LDAPException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnection(conn);
        }
    }
    
    /**
     * Modify entry in LDAP
     */
    public boolean modifyEntry(String dn, LDAPModification[] mods) {
        LDAPConnection conn = null;
        try {
            conn = getConnection();
            conn.modify(dn, mods);
            return true;
        } catch (LDAPException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnection(conn);
        }
    }
    
    /**
     * Add member to group
     */
    public boolean addMemberToGroup(String groupDN, String memberDN) {
        LDAPModification mod = new LDAPModification(
            LDAPModification.ADD,
            new LDAPAttribute(LdapConstants.ATTR_MEMBER, memberDN)
        );
        return modifyEntry(groupDN, new LDAPModification[]{mod});
    }
    
    /**
     * Remove member from group
     */
    public boolean removeMemberFromGroup(String groupDN, String memberDN) {
        LDAPModification mod = new LDAPModification(
            LDAPModification.DELETE,
            new LDAPAttribute(LdapConstants.ATTR_MEMBER, memberDN)
        );
        return modifyEntry(groupDN, new LDAPModification[]{mod});
    }
    
    /**
     * Create organizational unit
     */
    public boolean createOU(String ouDN, String ouName) {
        LDAPAttributeSet attributeSet = new LDAPAttributeSet();
        attributeSet.add(new LDAPAttribute(LdapConstants.ATTR_OBJECT_CLASS, LdapConstants.ORGANIZATIONAL_UNIT));
        attributeSet.add(new LDAPAttribute(LdapConstants.ATTR_OU, ouName));
        
        LDAPEntry newEntry = new LDAPEntry(ouDN, attributeSet);
        return addEntry(newEntry);
    }
    
    /**
     * Create group
     */
    public boolean createGroup(String groupDN, String groupName) {
        LDAPAttributeSet attributeSet = new LDAPAttributeSet();
        attributeSet.add(new LDAPAttribute(LdapConstants.ATTR_OBJECT_CLASS, LdapConstants.GROUP_OF_NAMES));
        attributeSet.add(new LDAPAttribute(LdapConstants.ATTR_CN, groupName));
        // Add empty member initially (required for groupOfNames)
        attributeSet.add(new LDAPAttribute(LdapConstants.ATTR_MEMBER, ""));
        
        LDAPEntry newEntry = new LDAPEntry(groupDN, attributeSet);
        return addEntry(newEntry);
    }
    
    /**
     * Get branch DN based on branch name
     */
    public String getBranchDN(String branch) {
        if (LdapConstants.BRANCH_INTERNAL.equalsIgnoreCase(branch)) {
            return LdapConstants.INTERNAL_GROUPS;
        } else if (LdapConstants.BRANCH_EXTERNAL.equalsIgnoreCase(branch)) {
            return LdapConstants.EXTERNAL_GROUPS;
        }
        return null;
    }
    
    /**
     * Check if entry exists
     */
    public boolean entryExists(String dn) {
        LDAPConnection conn = null;
        try {
            conn = getConnection();
            LDAPEntry entry = conn.read(dn);
            return entry != null;
        } catch (LDAPException e) {
            return false;
        } finally {
            closeConnection(conn);
        }
    }
    
    /**
     * Check if user is member of specific group by checking member attribute
     */
    public boolean isMemberOfGroup(String uid, String groupDN) {
        LDAPConnection conn = null;
        try {
            conn = getConnection();
            LDAPEntry entry = conn.read(groupDN);
            
            if (entry != null) {
                LDAPAttribute memberAttr = entry.getAttribute(LdapConstants.ATTR_MEMBER);
                if (memberAttr != null) {
                    String[] members = memberAttr.getStringValueArray();
                    for (String member : members) {
                        if (member.startsWith("uid=" + uid + ",")) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (LDAPException e) {
            return false;
        } finally {
            closeConnection(conn);
        }
    }
}