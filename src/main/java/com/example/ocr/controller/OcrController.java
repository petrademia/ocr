package com.example.ocr.controller;

import com.example.ocr.entity.TextInObjectResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class OcrController {

    /**
     * Application name.
     */
    private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    /**
     * Directory to store authorization tokens for this application.
     */
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE, DriveScopes.DRIVE_FILE);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    private static final String FOLDER_NAME = "sawitpro";
    private static final String MIME_TYPE = "text/plain";
    private static final String UPLOAD_FILE_PATH = "./file.txt";


    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = OcrController.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user1");
        //returns an authorized Credential object.
        return credential;
    }


    @PostMapping(value = "/ocr/image")
    public ResponseEntity<HttpStatus> upload(
            @RequestParam(value = "file") MultipartFile file
    ) throws IOException, TesseractException, GeneralSecurityException {

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        String folderId = findFolderId(service, FOLDER_NAME);
        if (folderId == null) {
            System.err.println("Folder not found.");
        }

        java.io.File fileContent = java.io.File.createTempFile("temp", null);
        file.transferTo(fileContent);
        File fileMetadata = new File();
        fileMetadata.setName(file.getOriginalFilename());
        fileMetadata.setParents(Collections.singletonList(folderId));
        FileContent mediaContent = new FileContent(file.getContentType(), fileContent);
        File uploadedFile = service.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();
        System.out.println("File ID: " + uploadedFile.getId());

        ITesseract instanceInEn = new Tesseract();
        ITesseract instanceInChi = new Tesseract();
        BufferedImage in = ImageIO.read(fileContent);

        BufferedImage newImage = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();
        g.drawImage(in, 0, 0, null);
        g.dispose();

        instanceInEn.setDatapath("./tessdata/");
        instanceInEn.setLanguage("eng");

        System.out.println("English sentence extracted:");
        String resultEn = instanceInEn.doOCR(newImage);
        System.out.println(resultEn);

        String[] dataInEn = resultEn.split("\\s+");
        List<String> wordWithOEn = Arrays.stream(dataInEn).filter(d -> d.contains("o") || d.contains("O")).collect(Collectors.toList());

        for (String word : wordWithOEn) {
            System.out.println((char)27 + "[34m" + word);
        }

        instanceInChi.setDatapath("./tessdata/");
        instanceInChi.setLanguage("chi_sim");

        System.out.println("Chinese sentence extracted:");
        String resultChi = instanceInChi.doOCR(newImage);
        System.out.println(resultChi);

        String[] dataInChi = resultChi.split("\\s+");
        List<String> wordWithChi = Arrays.stream(dataInChi).filter(d -> d.contains("o") || d.contains("O")).collect(Collectors.toList());

        for (String word : wordWithChi) {
            System.out.println((char)27 + "[34m" + word);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    public static java.io.File convert(MultipartFile file) throws IOException {
        java.io.File convFile = new java.io.File(file.getOriginalFilename());
        convFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }

    private static String findFolderId(Drive driveService, String folderName) throws IOException {
        String pageToken = null;
        do {
            FileList result = driveService.files().list()
                    .setQ("mimeType='application/vnd.google-apps.folder' and trashed = false and name = '" + folderName + "'")
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(pageToken)
                    .execute();
            for (com.google.api.services.drive.model.File file : result.getFiles()) {
                return file.getId();
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
        return null;
    }


}
