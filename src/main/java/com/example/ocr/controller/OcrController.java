package com.example.ocr.controller;

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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@RestController
public class OcrController {

    @PostMapping(value = "/ocr/image")
    public ResponseEntity<HttpStatus> upload(
            @RequestParam(value = "file")MultipartFile file
            ) throws IOException, TesseractException {

        ITesseract instance = new Tesseract();
        BufferedImage in = ImageIO.read(convert(file));

        BufferedImage newImage = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();
        g.drawImage(in, 0, 0, null);
        g.dispose();

        instance.setDatapath("./tessdata/");
        String result = instance.doOCR(newImage);
        System.out.println(result);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    public static File convert(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        convFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }

}
