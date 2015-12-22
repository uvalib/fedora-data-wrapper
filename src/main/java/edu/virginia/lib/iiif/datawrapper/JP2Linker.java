package edu.virginia.lib.iiif.datawrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import edu.virginia.lib.ole.akubra.BitStringMapper;

/**
 * RewriteRule ^/iiif/uva-lib:([0-9][0-9])([0-9][0-9])([0-9][0-9])([0-9])/(.*)$ /iipsrv?IIIF=/var/www/html/uva-lib/$1/$2/$3/$4/$1$2$3$4.jp2/$5 [PT]
 */
public class JP2Linker {

    /**
     * this crude program creates directories (using java) and outputs commands (that can later be run 
     * as a bash script) to create the links.  This is because a) I was too lazy to write bash script commands
     * to create directories, and b) I didn't want to depend on java 8 to create symbolic links.
     * @param args
     * @throws Exception
     */
    public static void main(String [] args) throws Exception {
        final File destRoot = new File("/var/www/html");
        JP2Linker l = new JP2Linker();
        
        // iterate over pids
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(args.length == 1 ? args[0] : "corks-pids.txt")));
        String pid = null;
        while ((pid = r.readLine()) != null) {
            pid = pid.trim();
            final File jp2File = l.findDatastreamFileFromRef(l.getRefOfMostRecentContentDatastream(l.findObjectFoxml(pid)));
            final File destFile = new File(destRoot, pidToId(pid));
            destFile.getParentFile().mkdirs();
            System.out.println("ln -s " + jp2File.getAbsolutePath() + " " + destFile);
        }
    }
    
    private BitStringMapper mapper;
    
    private File objectStore;
    private File dsstore1;
    private File dsstore2;
    
    public JP2Linker() {
        dsstore1 = new File("/lib_content38/dataStore");
        dsstore2 = new File("/lib_content39/dataStore");
        objectStore = new File("/lib_content38/objectStore");
        mapper = new BitStringMapper();
    }
    
    public static String pidToId(String pid) {
        StringBuffer newId = new StringBuffer();
        String[] split = pid.split(":");
        newId.append(split[0]);
        for (int i = 0; i < split[1].length(); i ++) {
            if (i % 2 == 0) {
                newId.append(File.separatorChar);
            }
            newId.append(split[1].charAt(i));
        }
        newId.append(File.separatorChar);
        newId.append(split[1]);
        newId.append(".jp2");
        
        return newId.toString();
        
    }
    
    public File findObjectFoxml(String pid) throws URISyntaxException {
        final URI uri = new URI("info:fedora/" + pid);
        final URI internalId = mapper.getInternalId(uri);
        return new File(objectStore, internalId.toString());
    }
    
    public String getRefOfMostRecentContentDatastream(File foxml) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        Document foxmlDoc = builderFactory.newDocumentBuilder().parse(foxml); 
        XPath xPath = XPathFactory.newInstance().newXPath();
        return xPath.compile("digitalObject/datastream[@ID='content']/datastreamVersion[last()]/contentLocation[@TYPE='INTERNAL_ID']/@REF").evaluate(foxmlDoc);
    }
    
    public File findDatastreamFileFromRef(String ref) throws URISyntaxException {
        final URI uri = new URI("info:fedora/" + ref.replace("+", "/"));
        final URI internalId = mapper.getInternalId(uri);
        if (internalId.toString().charAt(1) == '0') {
            return new File(dsstore1, internalId.toString().substring(3));
        } else {
            return new File(dsstore2, internalId.toString().substring(3));
        }
    }
    
}
