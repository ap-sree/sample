package com.sreemat.ldap.controller;

import com.sreemat.ldap.dto.ApiResponse;
import com.sreemat.ldap.dto.GroupResponse;
import com.sreemat.ldap.manager.GroupManager;
import com.sreemat.ldap.manager.OrgManager;
import com.sreemat.ldap.utils.PermissionUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * REST Controller for Group operations
 */
@Path("/branches/{branch}/groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GroupController {
    
    private final GroupManager groupManager;
    private final OrgManager orgManager;
    
    public GroupController() {
        this.groupManager = new GroupManager();
        this.orgManager = new OrgManager();
    }
    
    /**
     * GET /branches/{branch}/groups
     * Get groups for user in specified branch
     */
    @GET
    public Response getGroups(
            @PathParam("branch") String branch,
            @HeaderParam("uid") String uid) {
        
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
            
            // Get groups
            List<String> groupDNs = groupManager.getGroupsForUser(uid, branch);
            List<GroupResponse> groups = new ArrayList<>();
            
            for (String groupDN : groupDNs) {
                String groupName = PermissionUtils.extractGroupName(groupDN);
                String orgDN = PermissionUtils.extractOrgDNFromGroup(groupDN);
                String orgName = PermissionUtils.extractOrgName(orgDN);
                
                if (groupName != null && orgName != null) {
                    GroupResponse groupResponse = new GroupResponse(groupDN, groupName, orgDN, orgName, branch);
                    groups.add(groupResponse);
                }
            }
            
            return Response.ok(ApiResponse.success("Groups retrieved successfully", groups))
                    .build();
            
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Internal server error: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * POST /branches/{branch}/groups
     * Create new group
     */
    @POST
    public Response createGroup(
            @PathParam("branch") String branch,
            @HeaderParam("uid") String uid,
            @QueryParam("groupName") String groupName,
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
            
            if (groupName == null || groupName.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Group name is required"))
                    .build();
            }
            
            if (orgName == null || orgName.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Organization name is required"))
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
            if (!PermissionUtils.canCreateGroup(uid, orgDN)) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.error("Insufficient permissions to create group"))
                    .build();
            }
            
            // Create group
            boolean created = groupManager.createGroup(groupName.trim(), orgDN);
            
            if (created) {
                return Response.status(Response.Status.CREATED)
                    .entity(ApiResponse.success("Group created successfully"))
                    .build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to create group"))
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
     * POST /branches/{branch}/groups/{groupName}/admins
     * Add group admin
     */
    @POST
    @Path("/{groupName}/admins")
    public Response addGroupAdmin(
            @PathParam("branch") String branch,
            @PathParam("groupName") String groupName,
            @HeaderParam("uid") String requesterUid,
            @QueryParam("adminUid") String adminUid,
            @QueryParam("orgName") String orgName) {
        
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
            
            if (orgName == null || orgName.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Organization name is required"))
                    .build();
            }
            
            // Find group
            String groupDN = groupManager.findGroupDN(groupName, orgName, branch);
            if (groupDN == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("Group not found"))
                    .build();
            }
            
            // Check permissions
            if (!PermissionUtils.canManageGroupAdmins(requesterUid, groupDN)) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.error("Insufficient permissions to manage group admins"))
                    .build();
            }
            
            // Add group admin
            boolean added = groupManager.addGroupAdmin(groupDN, adminUid.trim());
            
            if (added) {
                return Response.ok(ApiResponse.success("Group admin added successfully"))
                        .build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to add group admin"))
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
     * DELETE /branches/{branch}/groups/{groupName}/admins
     * Remove group admin
     */
    @DELETE
    @Path("/{groupName}/admins")
    public Response removeGroupAdmin(
            @PathParam("branch") String branch,
            @PathParam("groupName") String groupName,
            @HeaderParam("uid") String requesterUid,
            @QueryParam("adminUid") String adminUid,
            @QueryParam("orgName") String orgName) {
        
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
            
            if (orgName == null || orgName.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Organization name is required"))
                    .build();
            }
            
            // Find group
            String groupDN = groupManager.findGroupDN(groupName, orgName, branch);
            if (groupDN == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("Group not found"))
                    .build();
            }
            
            // Check permissions
            if (!PermissionUtils.canManageGroupAdmins(requesterUid, groupDN)) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.error("Insufficient permissions to manage group admins"))
                    .build();
            }
            
            // Remove group admin
            boolean removed = groupManager.removeGroupAdmin(groupDN, adminUid.trim());
            
            if (removed) {
                return Response.ok(ApiResponse.success("Group admin removed successfully"))
                        .build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to remove group admin"))
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
     * POST /branches/{branch}/groups/{groupName}/members
     * Add group member
     */
    @POST
    @Path("/{groupName}/members")
    public Response addGroupMember(
            @PathParam("branch") String branch,
            @PathParam("groupName") String groupName,
            @HeaderParam("uid") String requesterUid,
            @QueryParam("memberUid") String memberUid,
            @QueryParam("orgName") String orgName) {
        
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
            
            if (memberUid == null || memberUid.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Member UID is required"))
                    .build();
            }
            
            if (orgName == null || orgName.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Organization name is required"))
                    .build();
            }
            
            // Find group
            String groupDN = groupManager.findGroupDN(groupName, orgName, branch);
            if (groupDN == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("Group not found"))
                    .build();
            }
            
            // Check permissions - only group admins can add members
            if (!PermissionUtils.canManageGroupMembers(requesterUid, groupDN)) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.error("Only group admins can add group members"))
                    .build();
            }
            
            // Add group member
            boolean added = groupManager.addGroupMember(groupDN, memberUid.trim());
            
            if (added) {
                return Response.ok(ApiResponse.success("Group member added successfully"))
                        .build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to add group member"))
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
     * DELETE /branches/{branch}/groups/{groupName}/members
     * Remove group member
     */
    @DELETE
    @Path("/{groupName}/members")
    public Response removeGroupMember(
            @PathParam("branch") String branch,
            @PathParam("groupName") String groupName,
            @HeaderParam("uid") String requesterUid,
            @QueryParam("memberUid") String memberUid,
            @QueryParam("orgName") String orgName) {
        
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
            
            if (memberUid == null || memberUid.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Member UID is required"))
                    .build();
            }
            
            if (orgName == null || orgName.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Organization name is required"))
                    .build();
            }
            
            // Find group
            String groupDN = groupManager.findGroupDN(groupName, orgName, branch);
            if (groupDN == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("Group not found"))
                    .build();
            }
            
            // Check permissions - only group admins can remove members
            if (!PermissionUtils.canManageGroupMembers(requesterUid, groupDN)) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.error("Only group admins can remove group members"))
                    .build();
            }
            
            // Remove group member
            boolean removed = groupManager.removeGroupMember(groupDN, memberUid.trim());
            
            if (removed) {
                return Response.ok(ApiResponse.success("Group member removed successfully"))
                        .build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to remove group member"))
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