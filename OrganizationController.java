package com.sreemat.ldap.controller;

import com.sreemat.ldap.dto.ApiResponse;
import com.sreemat.ldap.dto.OrgResponse;
import com.sreemat.ldap.manager.OrgManager;
import com.sreemat.ldap.utils.PermissionUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * REST Controller for Organization operations
 */
@Path("/branches/{branch}/organizations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrganizationController {
    
    private final OrgManager orgManager;
    
    public OrganizationController() {
        this.orgManager = new OrgManager();
    }
    
    /**
     * GET /branches/{branch}/organizations
     * Get organizations for user in specified branch
     */
    @GET
    public Response getOrganizations(
            @PathParam("branch") String branch,
            @HeaderParam("uid") String uid,
            @QueryParam("name") String orgName,
            @QueryParam("nested") @DefaultValue("false") boolean includeSubOrgs) {
        
        try {
            // Validate inputs
            if (uid == null || uid.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("UID header is required"))
                    .build();
            }
            
            if (!PermissionUtils.isValidBranch(branch)) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Invalid branch. Must be 'internal' or 'external'"))
                    .build();
            }
            
            // Get organizations
            List<String> orgDNs = orgManager.getOrganizationsForUser(uid, branch, orgName, includeSubOrgs);
            List<OrgResponse> organizations = new ArrayList<>();
            
            for (String orgDN : orgDNs) {
                String extractedOrgName = PermissionUtils.extractOrgName(orgDN);
                if (extractedOrgName != null) {
                    OrgResponse orgResponse = new OrgResponse(orgDN, extractedOrgName, branch);
                    
                    // Add sub-organizations if nested is true
                    if (includeSubOrgs) {
                        List<String> subOrgDNs = orgManager.getSubOrganizations(orgDN);
                        for (String subOrgDN : subOrgDNs) {
                            String subOrgName = PermissionUtils.extractOrgName(subOrgDN);
                            if (subOrgName != null) {
                                orgResponse.addSubOrg(new OrgResponse(subOrgDN, subOrgName, branch));
                            }
                        }
                    }
                    
                    organizations.add(orgResponse);
                }
            }
            
            return Response.ok(ApiResponse.success("Organizations retrieved successfully", organizations))
                    .build();
            
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Internal server error: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * POST /branches/{branch}/organizations
     * Create new organization
     */
    @POST
    public Response createOrganization(
            @PathParam("branch") String branch,
            @HeaderParam("uid") String uid,
            @QueryParam("orgName") String orgName) {
        
        try {
            // Validate inputs
            if (uid == null || uid.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("UID header is required"))
                    .build();
            }
            
            if (!PermissionUtils.isValidBranch(branch)) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Invalid branch. Must be 'internal' or 'external'"))
                    .build();
            }
            
            if (orgName == null || orgName.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Organization name is required"))
                    .build();
            }
            
            // Check permissions
            if (!PermissionUtils.canCreateOrganization(uid)) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.error("Only super admin can create organizations"))
                    .build();
            }
            
            // Create organization
            boolean created = orgManager.createOrganization(orgName.trim(), branch);
            
            if (created) {
                return Response.status(Response.Status.CREATED)
                    .entity(ApiResponse.success("Organization created successfully"))
                    .build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to create organization"))
                    .build();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Internal server error: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * POST /branches/{branch}/organizations/{orgName}/subOrg
     * Create sub-organization
     */
    @POST
    @Path("/{orgName}/subOrg")
    public Response createSubOrganization(
            @PathParam("branch") String branch,
            @PathParam("orgName") String orgName,
            @HeaderParam("uid") String uid,
            @QueryParam("subOrgName") String subOrgName) {
        
        try {
            // Validate inputs
            if (uid == null || uid.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("UID header is required"))
                    .build();
            }
            
            if (!PermissionUtils.isValidBranch(branch)) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Invalid branch. Must be 'internal' or 'external'"))
                    .build();
            }
            
            if (subOrgName == null || subOrgName.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Sub-organization name is required"))
                    .build();
            }
            
            // Find parent organization
            String parentOrgDN = orgManager.findOrganizationDN(orgName, branch);
            if (parentOrgDN == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("Parent organization not found"))
                    .build();
            }
            
            // Check permissions
            if (!PermissionUtils.canCreateSubOrganization(uid, parentOrgDN)) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.error("Insufficient permissions to create sub-organization"))
                    .build();
            }
            
            // Create sub-organization
            boolean created = orgManager.createSubOrganization(subOrgName.trim(), parentOrgDN);
            
            if (created) {
                return Response.status(Response.Status.CREATED)
                    .entity(ApiResponse.success("Sub-organization created successfully"))
                    .build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to create sub-organization"))
                    .build();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Internal server error: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * POST /branches/{branch}/organizations/{orgName}/admins
     * Add organization admin
     */
    @POST
    @Path("/{orgName}/admins")
    public Response addOrgAdmin(
            @PathParam("branch") String branch,
            @PathParam("orgName") String orgName,
            @HeaderParam("uid") String requesterUid,
            @QueryParam("adminUid") String adminUid) {
        
        try {
            // Validate inputs
            if (requesterUid == null || requesterUid.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("UID header is required"))
                    .build();
            }
            
            if (!PermissionUtils.isValidBranch(branch)) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Invalid branch. Must be 'internal' or 'external'"))
                    .build();
            }
            
            if (adminUid == null || adminUid.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Admin UID is required"))
                    .build();
            }
            
            // Find organization
            String orgDN = orgManager.findOrganizationDN(orgName, branch);
            if (orgDN == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("Organization not found"))
                    .build();
            }
            
            // Check permissions
            if (!PermissionUtils.canManageOrganization(requesterUid, orgDN)) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.error("Insufficient permissions to manage organization"))
                    .build();
            }
            
            // Add admin
            boolean added = orgManager.addOrgAdmin(orgDN, adminUid.trim());
            
            if (added) {
                return Response.ok(ApiResponse.success("Organization admin added successfully"))
                        .build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to add organization admin"))
                    .build();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Internal server error: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * DELETE /branches/{branch}/organizations/{orgName}/admins
     * Remove organization admin
     */
    @DELETE
    @Path("/{orgName}/admins")
    public Response removeOrgAdmin(
            @PathParam("branch") String branch,
            @PathParam("orgName") String orgName,
            @HeaderParam("uid") String requesterUid,
            @QueryParam("adminUid") String adminUid) {
        
        try {
            // Validate inputs
            if (requesterUid == null || requesterUid.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("UID header is required"))
                    .build();
            }
            
            if (!PermissionUtils.isValidBranch(branch)) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Invalid branch. Must be 'internal' or 'external'"))
                    .build();
            }
            
            if (adminUid == null || adminUid.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Admin UID is required"))
                    .build();
            }
            
            // Find organization
            String orgDN = orgManager.findOrganizationDN(orgName, branch);
            if (orgDN == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("Organization not found"))
                    .build();
            }
            
            // Check permissions
            if (!PermissionUtils.canManageOrganization(requesterUid, orgDN)) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.error("Insufficient permissions to manage organization"))
                    .build();
            }
            
            // Remove admin
            boolean removed = orgManager.removeOrgAdmin(orgDN, adminUid.trim());
            
            if (removed) {
                return Response.ok(ApiResponse.success("Organization admin removed successfully"))
                        .build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to remove organization admin"))
                    .build();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Internal server error: " + e.getMessage()))
                    .build();
        }
    }
}