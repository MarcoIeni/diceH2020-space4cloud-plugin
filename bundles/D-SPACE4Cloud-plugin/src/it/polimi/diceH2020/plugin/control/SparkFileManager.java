package it.polimi.diceH2020.plugin.control;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;


import it.polimi.diceH2020.plugin.preferences.Preferences;

public class SparkFileManager {

    /**
     * I substitute the placeholder in the word after id in the file
     *
     * @param id
     * @param placeholder
     *            the word after id will be substituted with this value
     * @param inputFilename
     * @param outputFileName
     */
    public static void putPlaceHolder(String id, String placeholder, File inputFile) {
        try {
            Path path = Paths.get(inputFile.getAbsolutePath());
            String content = new String(Files.readAllBytes(path));

            String regex = id + " ([0-9]*[.])?[0-9]+";
            String replacement = id + " " + placeholder;

            // Placeholder in .def file when using Hadoop GSPN
            if (id.startsWith("ia{")){
                regex = "ia\\{[0-9]\\-[0-9]+\\}";
                replacement = "ia{1-" + placeholder + "}";
            }

            if (id.startsWith("Ra{")){
                regex = "Ra\\{[0-9]\\-[0-9]+\\}";
                replacement = "Ra{1-" + placeholder + "}";
            }

            if (id.startsWith("sa{")){
                regex = "sa\\{[0-9]\\-[0-9]+\\}";
                replacement = "sa{1-" + placeholder + "}";
            }

            String finalContent = content.replaceAll(regex, replacement);

            try (PrintWriter out = new PrintWriter(inputFile)) {
                out.println(finalContent);
                out.close();
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void editFiles(int cdid, String alt, SparkIds sparkIds) {
        String savingDir = Preferences.getSavingDir();
        Configuration conf = Configuration.getCurrent();

        File folder = new File(savingDir + "tmp/");
        File files[] = folder.listFiles();
        String outputFilePath;

        if (Configuration.getCurrent().getIsPrivate()) {
            outputFilePath = savingDir + conf.getID() + "J" + cdid + "inHouse" + alt;
        } else {
            outputFilePath = savingDir + conf.getID() + "J" + cdid + alt.replaceAll("-", "");
        }
        File netFile = null;
        File defFile = null;

        for (File f : files) {
            if (f.getName().endsWith(".net")) {
                netFile = f;
            } else if (f.getName().endsWith(".def")) {
                defFile = f;
            }
        }

        System.out.println("UsersId: " + sparkIds.getUsers());
        System.out.println("devices2resourcesId: " + sparkIds.getDevices2resources());
        System.out.println("Last Transaction Id: " + sparkIds.getNumberOfConcurrentUsers());

        System.out.println("Putting placeholders over net file");

        putPlaceHolder(sparkIds.getDevices2resources(), "@@CORES@@", netFile);
        putPlaceHolder(sparkIds.getUsers(), "@@CORES@@", netFile);

        moveFile(netFile, outputFilePath, "net");
        moveFile(defFile, outputFilePath, "def");

    }

    public static void editJSIMG(int cdid, String alt, SparkIds sparkIds) {
        Configuration conf = Configuration.getCurrent();

        String filename;
        if (conf.getIsPrivate()) {
            filename = Preferences.getSavingDir() + conf.getID() + "J" + cdid + "inHouse" + alt;
        } else {
            filename = Preferences.getSavingDir() + conf.getID() + "J" + cdid + alt.replaceAll("-", "");
        }

        System.out.println(String.format("Putting placeholders over %s.jsimg", filename));

        File jsimgFile = new File(filename + ".jsimg");
        putPlaceHolderXML(sparkIds.getUsers(), "@@CORES@@", jsimgFile);
        putPlaceHolderXML(sparkIds.getDevices2resources(), "@@CORES@@", jsimgFile);
    }

    public static void editJSIMG(int cdid, String alt, String idToReplace) {
        String savingDir = Preferences.getSavingDir();
        Configuration conf = Configuration.getCurrent();

        String filename;
        if (Configuration.getCurrent().getIsPrivate()) {
            filename = savingDir + conf.getID() + "J" + cdid + "inHouse" + alt;
        } else {
            filename = savingDir + conf.getID() + "J" + cdid + alt.replaceAll("-", "");
        }

        System.out.println(String.format("Putting placeholders over %s.jsimg", filename));

        File jsimgFile = new File(filename + ".jsimg");
        putPlaceHolderXML(idToReplace, "@@CORES@@", jsimgFile);
    }

    private static void putPlaceHolderXML(String id, String placeholder, File file){

        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();

            XPathExpression expr = xpath.compile("//stationPopulations[@stationName=\""+id+"\"]/classPopulation");

            Node node = (Node) expr.evaluate(doc, XPathConstants.NODE);
            Node attr = node.getAttributes().getNamedItem("population");
            attr.setNodeValue(placeholder);

            // Write changes to a file
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(file));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void moveFile(File InputFile, String outputFilePath, String extension) {
        Path input = Paths.get(InputFile.getAbsolutePath());
        Path output = Paths.get(outputFilePath + "." + extension);
        try {
            Files.move(input, output, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createStatFile(int cdid, String alt, String LastTransitionId){

        Configuration conf = Configuration.getCurrent();
        String filename;

        if (conf.getIsPrivate()) {
            filename = Preferences.getSavingDir() + conf.getID() + "J" + cdid + "inHouse" + alt;
        } else {
            filename = Preferences.getSavingDir() + conf.getID() + "J" + cdid + alt.replaceAll("-", "");
        }

        File statFile = new File(filename + ".stat");

        try (PrintWriter out = new PrintWriter(statFile)){
            out.println(LastTransitionId);
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
