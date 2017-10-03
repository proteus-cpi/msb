/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.openmos.msb.services.rest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 *
 * @author valerio.gentile
 */
@Path("/api/v1/uploadedfiles")
public class FileUploadController {
    private final Logger logger = Logger.getLogger(FileUploadController.class.getName());
    
@POST
@Consumes({MediaType.MULTIPART_FORM_DATA})
public Response uploadPdfFile(  @FormDataParam("file") InputStream fileInputStream,
                                @FormDataParam("file") FormDataContentDisposition fileMetaData) throws FileNotFoundException, IOException
{
    logger.debug("fileInputStream" + fileInputStream);
    logger.debug("fileMetaData" + fileMetaData);
    String UPLOAD_PATH = "c:/temp/";

    int read = 0;
    byte[] bytes = new byte[1024];

    OutputStream out = new FileOutputStream(new File(UPLOAD_PATH + fileMetaData.getFileName()));
    while ((read = fileInputStream.read(bytes)) != -1)
    {
        out.write(bytes, 0, read);
    }
    out.flush();
    out.close();

    return Response.ok("Data uploaded successfully !!").build();
}    

}