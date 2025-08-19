package com.ste.restaurant.service;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyDescriptor;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ServiceUtil {

    public static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<String>();
        for (PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) emptyNames.add(pd.getName());
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null) {return "";}
        String extension;
        try {
            int dotIndex = fileName.lastIndexOf(".");
            if (dotIndex == -1 || dotIndex == 0) {return "";}
            extension = fileName
                    .substring(dotIndex + 1)
                    .toLowerCase();
        }
        catch (Exception e) {
            extension = "";
        }
        return extension;
    }

    public static boolean cropAndResizeToSquare(MultipartFile imageFile, String filePath, int size) {
        try (InputStream is = imageFile.getInputStream()) {
            BufferedImage originalImage = ImageIO.read(is);

            // Crop to 1:1 ratio (center crop)
            int width = originalImage.getWidth();
            int height = originalImage.getHeight();
            int squareSize = Math.min(width, height);
            int x = (width - squareSize) / 2;
            int y = (height - squareSize) / 2;
            BufferedImage croppedImage = originalImage.getSubimage(x, y, squareSize, squareSize);

            // Resize to fixed size
            BufferedImage resizedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resizedImage.createGraphics();
            g2d.drawImage(croppedImage, 0, 0, size, size, null);
            g2d.dispose();

            ImageIO.write(resizedImage, getFileExtension(imageFile.getOriginalFilename()), new java.io.File(filePath));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static <T> Page<T> createPage(List<T> list, Pageable pageable) {
        if (list == null || list.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        int total = list.size();
        long offset = pageable.getOffset();

        if (offset >= total) {
            return new PageImpl<>(Collections.emptyList(), pageable, total);
        }
        List<T> pagedContent = list.stream()
                .skip(offset)
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());

        return new PageImpl<>(pagedContent, pageable, total);
    }
}
