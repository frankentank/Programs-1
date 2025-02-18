package edu.nmsu.cs.webserver;

/**
 * Web worker: an object of this class executes in its own new thread to receive and respond to a
 * single HTTP request. After the constructor the object executes on its "run" method, and leaves
 * when it is done.
 *
 * One WebWorker object is only responsible for one client connection. This code uses Java threads
 * to parallelize the handling of clients: each WebWorker runs in its own thread. This means that
 * you can essentially just think about what is happening on one client at a time, ignoring the fact
 * that the entirety of the webserver execution might be handling other clients, too.
 *
 * This WebWorker class (i.e., an object of this class) is where all the client interaction is done.
 * The "run()" method is the beginning -- think of it as the "main()" for a client interaction. It
 * does three things in a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it writes out some HTML
 * content for the response content. HTTP requests and responses are just lines of text (in a very
 * particular format).
 * 
 * @author Jon Cook, Ph.D.
 *
 **/

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.File;
import java.util.Scanner;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.io.FileInputStream;

public class WebWorker implements Runnable
{

   private Socket socket;

	/**
	 * Constructor: must have a valid open socket
	 **/
   public WebWorker(Socket s)
   {
      socket = s;
   }

	/**
	 * Worker thread starting point. Each worker handles just one HTTP request and then returns, which
	 * destroys the thread. This method assumes that whoever created the worker created it with a
	 * valid open socket object.
	 **/
   public void run() {
      System.err.println("Handling connection...");
      try {  
         
         File file; 
         boolean found; 
         String filePath, contentType;
         
         InputStream is = socket.getInputStream();
         OutputStream os = socket.getOutputStream();
         file = readHTTPRequest(is);
         System.out.println(file.length());
         found = file.exists();
         filePath = file.getAbsolutePath();
         contentType = getContentType(filePath);
         
         
         writeHTTPHeader(os, contentType, found);
         writeContent(os, found, contentType, file);
         os.flush();
         socket.close();
      }
      catch (Exception e)
      {
         System.err.println("Output error: " + e);
      }
      System.err.println("Done handling connection.");
      return;
   } //end run
   
   /**
	 * Get the content type based on the filepath
	 **/
   public static String getContentType(String filePath) {
      String temp, contentType;
      
      temp = filePath.substring(filePath.indexOf(".") + 1);
      
      if (temp.equals("html"))
         contentType = "text/html";
      else //content type is an image
         contentType = "image/" + temp;
         
      return contentType;
   } //end getContentType

	/**
	 * Read the HTTP request header.
	 **/
   private File readHTTPRequest(InputStream is) {
      File file = null;
      String line;
      BufferedReader r = new BufferedReader(new InputStreamReader(is));
      while (true) {
         try {
            while (!r.ready())
               Thread.sleep(1);
            line = r.readLine();
            System.err.println("Request line: (" + line + ")");
            
            if (line.startsWith("GET")) {
               file = new File("C:\\Users\\frank\\Documents\\School stuff\\CS371\\Programs-1\\SimpleWebServer\\www" +
                                 line.substring(4, line.indexOf("HTTP") - 1));
            } //end if 
            
            if (line.length() == 0)
               break;
         } //end try
         catch (Exception e) {
            System.err.println("Request error: " + e);
            break;
         } //end catch
      } //end while
      return file;
   } //end readHTTPRequest

	/**
	 * Write the HTTP header lines to the client network connection.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 * @param contentType
	 *          is the string MIME content type (e.g. "text/html")
	 **/
   private void writeHTTPHeader(OutputStream os, String contentType, boolean found) throws Exception {
      String status;
      Date d = new Date();
      DateFormat df = DateFormat.getDateTimeInstance();
      df.setTimeZone(TimeZone.getTimeZone("GMT"));
      
      if (found) {
         status = "200 OK";
      } //end if
      else { // not found
         status = "404 Not Found";
      } //end else 
      os.write(("HTTP/1.1 " + status + "\n").getBytes());
      os.write("Date: ".getBytes());
      os.write((df.format(d)).getBytes());
      os.write("\n".getBytes());
      os.write("Server: Jon's very own server\n".getBytes());
   	// os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
   	// os.write("Content-Length: 438\n".getBytes());
      os.write("Connection: close\n".getBytes());
      os.write("Content-Type: ".getBytes());
      os.write(contentType.getBytes());
      os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
      return;
   }

	/**
	 * Write the data content to the client network connection. This MUST be done after the HTTP
	 * header has been written out.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
    * @param found
    *          is the status of whether the requested file was found or not
	 **/
   private void writeContent(OutputStream os, boolean found, String contentType, File file) throws Exception {
      
      if (found) { 
         switch (contentType) {
            case "text/html":
               Scanner filescan = new Scanner(file);
               String input;
               while (filescan.hasNext()) {
                  input = filescan.nextLine();
                  input = replaceTags(input);
                  os.write(input.getBytes());
               } //end while
               break;
            case "image/png":
            case "image/jpg":
            case "image/gif":
               FileInputStream image = new FileInputStream(file);
               
               os.write(image.readAllBytes());
               
               
               break;
               
            default:
               os.write("<html><head></head><body>\n".getBytes());
               os.write(("<h3>My web server works!</h3>\n").getBytes());
               os.write("</body></html>\n".getBytes());
               break;
         } //end switch
      } //end if
      else { //not found
         os.write("<html><head></head><body>\n".getBytes());
         os.write(("<h3>404 source not found</h3>\n").getBytes());
         os.write("</body></html>\n".getBytes());
      } //end else
      
   
   } //end writeContent
   
   public static String replaceTags(String input) {
      int date, server;
      String i = input;
      
      Date d = new Date();
      DateFormat df = DateFormat.getDateTimeInstance();
      // df.setTimeZone(TimeZone.getTimeZone("GMT"));
      
      date = i.indexOf("<cs371date>");
      
      if (date != -1) {
         i = i.replace((CharSequence) "<cs371date>", (CharSequence) df.format(d));
      } //end if
      
      server = i.indexOf("<cs371server>");
      if (server != -1) {
         i = i.replace((CharSequence) "<cs371server>", (CharSequence) "FrankenServer");
      } //end if 
      
      return i;
   } //end replaceTags

} // end class
