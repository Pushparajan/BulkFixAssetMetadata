package com.aem.assets.metadata.fix;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.LoggerFactory;


public class Main {

	private static final String FILE_PATH = "Assets_Missing_File_Size_Test.xlsx";
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);
	private static Workbook workbook;
	private static FileInputStream fis = null;
	private static javax.jcr.Session session = null;
	private static Repository repository = null;

	public static void main(String[] args) {
		try {
			
			fis = new FileInputStream(FILE_PATH);
			workbook = new XSSFWorkbook(fis);
			Sheet sheet = workbook.getSheetAt(0);
			System.out.println(sheet.getSheetName());
			log.info(sheet.getSheetName());
			List<String> imagePaths = getImagePaths(sheet);
			System.out.println(imagePaths.toString());
			log.info(imagePaths.toString());
			Map<String, Long> data = getImageSize (imagePaths);
			System.out.println(data.toString());
			
			write2aem(data);
			
			fis.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private static List<String> getImagePaths(Sheet sheet) {
		// TODO Auto-generated method stub
		List<String> retValue = new ArrayList<String>();  
		Iterator rows = sheet.rowIterator();
		while (rows.hasNext()) {
			
			//skip the header
			rows.next();
			
			XSSFRow row = (XSSFRow) rows.next();
			/*
			XSSFCell cell1 = (XSSFCell) row.getCell(72);
			cell1.setCellType(Cell.CELL_TYPE_STRING);
			RichTextString path = cell1.getRichStringCellValue();
			
			XSSFCell cell2 = (XSSFCell) row.getCell(72);
			cell2.setCellType(Cell.CELL_TYPE_STRING);
			RichTextString name = cell2.getRichStringCellValue();
			
			retValue.add(path.getString()+name.getString());
			*/
			XSSFCell cell1 = (XSSFCell) row.getCell(72);
			cell1.setCellType(Cell.CELL_TYPE_STRING);
			RichTextString path = cell1.getRichStringCellValue();
			
			retValue.add(path.getString());
		}
		
		return retValue;
	}

	private static Map<String, Long> getImageSize (List<String> imagePaths) throws IOException {
		Map<String, Long> retValue = new HashMap<String, Long>();
		// TODO Auto-generated method stub

		for (String imagePath : imagePaths) {

			System.out.println("getting size of" + imagePath);

			URL url;
			url = new URL("https://www.somesite.com/"+imagePath);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			InputStream is = url.openStream();
			byte[] b = new byte[2^16];
			int read = is.read(b);
			while (read>-1) {
				baos.write(b,0,read);
				read = is.read(b);
			}
			int countInBytes = baos.toByteArray().length;
			System.out.println("Size in KBytes : " + countInBytes/1024);
			retValue.put(imagePath, (long) countInBytes);

		}
		return retValue;
	}

	public static void write2aem (Map<String, Long> data) throws RepositoryException {

		// Create a connection to the CQ repository running on local host
		repository = JcrUtils.getRepository("http://localhost:4502/crx/server");
		// Create a Session
		session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));

		try {

			for (Map.Entry<String,Long> entry : data.entrySet())  {
				System.out.println("Key = " + entry.getKey() + 
						", Value = " + entry.getValue()); 
				String imagePath = entry.getKey();
				long size = (long) entry.getValue();
				// Create a node that represents the root node
				//Node root = session.getRootNode();

				Node node = session.getNode(imagePath+"/jcr:content/metadata");
				node.setProperty("dam:size", size);

				System.out.println(node.getPath());
				System.out.println(node.getProperty("dam:size").getLong());

				// Save the session changes
				session.save();
			}
			session.logout();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
