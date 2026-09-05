/**
 * CREATED AT : 09/04/2026 (M-D-Y)
 * CREATED BY : SUBRATA ROY
 */

/*
     * ==========================================
     * API Functions inside the Attachment Controller:
     * ==========================================
     * 1. generateUploadPresignedUrl - Requests a temporary cloud storage URL for secure client-side uploading
     * 2. getAttachmentDownloadUrl   - Generates a time-limited, secure link to view/download a file attachment
     * 3. deleteAttachmentFile       - Removes a file attachment entry from the system
 */
 /*
 * =================================================================================
 * ATTACHMENT CONTROLLER ENDPOINT DOCUMENTATION
 * =================================================================================
 *
 * 1. generateUploadPresignedUrl
 *    - HTTP Method: POST
 *    - Role Allowed: Authenticated users (Students uploading work, Teachers uploading materials)
 *    - How it works: The client sends file metadata (like filename and content type) 
 *      to the server. The server communicates with a cloud storage provider (like 
 *      AWS S3 or Google Cloud Storage) to generate a short-lived, authenticated URL. 
 *      This URL is returned to the client, allowing the frontend to upload the file 
 *      directly to the cloud bucket without bloating the backend server.
 *    - Why it’s used: Ensures secure, optimized file uploads by bypassing the core 
 *      application server for heavy file transfers while enforcing access controls.
 *
 * 2. getAttachmentDownloadUrl
 *    - HTTP Method: GET
 *    - Role Allowed: Users with permission to view the item (e.g., the student who 
 *      owns it, or the instructor grading it)
 *    - How it works: Verifies that the requesting user has the right to access the 
 *      requested file ID. Once validated, it requests a time-limited read URL from 
 *      the cloud storage provider. The frontend uses this temporary link to display 
 *      or download the file safely.
 *    - Why it’s used: Keeps the underlying cloud files private and protected from 
 *      public access, ensuring links expire quickly so they cannot be shared with 
 *      unauthorized users.
 *
 * 3. deleteAttachmentFile
 *    - HTTP Method: DELETE
 *    - Role Allowed: Owner of the attachment or an Administrator
 *    - How it works: Receives the specific attachment ID, finds the record in the 
 *      database, and deletes it. Simultaneously, it triggers a command to the cloud 
 *      storage bucket to permanently remove the physical file file object associated 
 *      with that reference.
 *    - Why it’s used: Allows students to replace files on draft submissions, permits 
 *      teachers to clean up outdated class resources, and prevents storage waste.
 */
package com.M198.Majorproject.controller.attachment;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/attachments")
public class AttachmentController {

}
