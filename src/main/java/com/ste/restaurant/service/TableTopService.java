package com.ste.restaurant.service;

import com.ste.restaurant.dto.common.StringDto;
import com.ste.restaurant.dto.TableTopDto;
import com.ste.restaurant.dto.TableTopDtoQr;
import com.ste.restaurant.entity.enums.TableStatus;
import com.ste.restaurant.entity.TableTop;
import com.ste.restaurant.exception.*;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.TableTopRepository;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class TableTopService {

    private final TableTopRepository tableRepository;
    private final OrderMapper orderMapper;
    private final String siteBaseUrl;
    private final String qrCodeDir;

    private static final int QR_CODE_SIZE = 250;

    public TableTopService(TableTopRepository tableTopRepository, OrderMapper orderMapper,
                           @Value("${site.base.url}") String siteBaseUrl,
                           @Value("${app.image.qr-code-dir}") String qrCodeDir) {
        this.tableRepository = tableTopRepository;
        this.orderMapper = orderMapper;
        this.siteBaseUrl = siteBaseUrl;
        this.qrCodeDir = qrCodeDir;
    }

    public TableTopDto saveTable(TableTopDto tableTopDto) {
        if (tableTopDto.getTableNumber() == null) {
            throw new NullValueException("Table", "number");
        }

        if (tableRepository.existsTableTopByTableNumber(tableTopDto.getTableNumber())) {
            throw new AlreadyExistsException("Table", tableTopDto.getTableNumber());
        }
        TableTop tableTop = new TableTop();
        orderMapper.updateTableFromDto(tableTopDto, tableTop);

        try {
            String qrCodeFileName = createQrCodeFile(tableTop);
            tableTop.setQrCode(qrCodeFileName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ImageProcessingException("Failed to generate QR code for table: " + tableTop.getTableNumber());
        }

        TableTop savedTable = tableRepository.save(tableTop);
        return orderMapper.tableTopToTableTopDto(savedTable);
    }

    public List<TableTopDtoQr> getAllQrCodes() {
        List<TableTop> tableTops = tableRepository.findAll();
        List<TableTopDtoQr> qrCodes = new ArrayList<>();
        for (TableTop table : tableTops) {
            qrCodes.add(new TableTopDtoQr(table.getTableNumber(), table.getQrCode()));
        }
        return qrCodes;
    }

    public List<TableTopDto> getAllTables() {
        List<TableTop> tableTops = tableRepository.findAll();
        return orderMapper.tableTopsToTableTopDtos(tableTops);
    }

    public TableTopDto getTableByName(String name) {
        TableTop table = tableRepository.findByTableNumber(name)
                .orElseThrow(() -> new NotFoundException("Table", name));

        return orderMapper.tableTopToTableTopDto(table);
    }

    public TableTopDto deleteTableByName(String name) {
        TableTop table = tableRepository.findByTableNumber(name)
                .orElseThrow(() -> new NotFoundException("Table", name));

        tableRepository.delete(table);
        return orderMapper.tableTopToTableTopDto(table);
    }

    public TableTopDto updateTable(String name, TableTopDto table) {
        TableTop tableOld = tableRepository.findByTableNumber(name)
                .orElseThrow(() -> new NotFoundException("Table", name));

        if (table.getTableNumber() != null && !table.getTableNumber().equals(name)) {
            if (tableRepository.findByTableNumber(table.getTableNumber()).isPresent()) {
                throw new AlreadyExistsException("Table", table.getTableNumber());
            }
            tableOld.setTableNumber(table.getTableNumber());
        }
        if (table.getCapacity() != null) {
            tableOld.setCapacity(table.getCapacity());
        }
        if (tableOld.getTableStatus() != table.getTableStatus()) {
            tableOld.setTableStatus(table.getTableStatus());
        }

        TableTop savedTable = tableRepository.save(tableOld);
        return orderMapper.tableTopToTableTopDto(savedTable);
    }

    @Transactional
    public TableTopDto updateTableStatusByName(String name, StringDto statusDto) {
        if (statusDto.getName() == null) {
            throw new NullValueException("Table", "status");
        }
        String status = statusDto.getName();

        TableTop table = tableRepository.findByTableNumber(name).
                orElseThrow(() -> new NotFoundException("Table", name));

        TableStatus newStatus;
        try {
            newStatus = TableStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("Table", "status", status);
        }

        if (table.getTableStatus().equals(newStatus)) {
            throw new AlreadyHasException("Table", "status", status);
        }

        table.setTableStatus(newStatus);

        tableRepository.save(table);
        return orderMapper.tableTopToTableTopDto(table);
    }

    public List<TableTopDto> getAvailableTables() {
        List<TableTop> tableTops = tableRepository.findAllByTableStatus(TableStatus.AVAILABLE);
        return orderMapper.tableTopsToTableTopDtos(tableTops);
    }

    public void createQrForTables() {
        List<TableTop> tables = tableRepository.findAll();

        for (TableTop table : tables) {
            try {
                String qrCodeFileName = createQrCodeFile(table);
                table.setQrCode(qrCodeFileName);
                tableRepository.save(table);
            }
            catch (Exception e) {
                System.err.println("Failed to generate QR code for table: " + table.getTableNumber());
                e.printStackTrace();
            }
        }
    }

    private String createQrCodeFile(TableTop table) throws IOException {
        String tableNumber = table.getTableNumber();
        String qrCodeUrl = siteBaseUrl + "/menu?table=" + tableNumber;
        String fileName = "table_" + tableNumber + ".jpg";

        File qrCodeFile = new File(qrCodeDir + fileName);

        File dir = new File(qrCodeDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            Path qrCodePath = qrCodeFile.toPath();
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeUrl, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);
            MatrixToImageWriter.writeToPath(bitMatrix, "JPG", qrCodePath);
        } catch (WriterException e) {
            throw new IOException("Failed to generate QR code", e);
        }

        return fileName;
    }

    public void deleteAllQrCodes() {
        List<TableTop> tables = tableRepository.findAll();

        for (TableTop table : tables) {
            String qrCodeFileName = table.getQrCode();
            if (qrCodeFileName != null) {
                File qrCodeFile = new File(qrCodeDir + qrCodeFileName);
                if (qrCodeFile.exists()) {
                    if (!qrCodeFile.delete()) {
                        System.err.println("Failed to delete QR code file: " + qrCodeFileName);
                    }
                }
                table.setQrCode(null);
                tableRepository.save(table);
            }
        }
    }
}
