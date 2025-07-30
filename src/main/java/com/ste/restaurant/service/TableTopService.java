package com.ste.restaurant.service;

import com.ste.restaurant.dto.StringDto;
import com.ste.restaurant.dto.TableTopDto;
import com.ste.restaurant.entity.TableStatus;
import com.ste.restaurant.entity.TableTop;
import com.ste.restaurant.exception.*;
import com.ste.restaurant.repository.TableTopRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TableTopService {

    @Autowired
    TableTopRepository tableRepository;

    public TableTopDto saveTable(TableTopDto tableTopDto) {
        if (tableTopDto.getTableNumber() == null) {
            throw new NullValueException("Table", "number");
        }

        if (tableRepository.existsTableTopByTableNumber(tableTopDto.getTableNumber())) {
            throw new AlreadyExistsException("Table", tableTopDto.getTableNumber());
        }
        TableTop tableTop = new TableTop();
        BeanUtils.copyProperties(tableTopDto, tableTop,
                ServiceUtil.getNullPropertyNames(tableTopDto));
        TableTop savedTable = tableRepository.save(tableTop);
        BeanUtils.copyProperties(savedTable, tableTopDto);
        return tableTopDto;
    }

    public List<TableTopDto> getAllTables() {
        List<TableTop> tableTops = tableRepository.findAll();
        List<TableTopDto> tableTopDtos = new ArrayList<>();
        for (TableTop tableTop : tableTops) {
            TableTopDto tableTopDto = new TableTopDto();
            BeanUtils.copyProperties(tableTop, tableTopDto);
            tableTopDtos.add(tableTopDto);
        }
        return tableTopDtos;
    }

    public TableTopDto getTableByName(String name) {
        TableTop table = tableRepository.findByTableNumber(name)
                .orElseThrow(() -> new NotFoundException("Table", name));

        TableTopDto tableTopDto = new TableTopDto();
        BeanUtils.copyProperties(table, tableTopDto);
        return tableTopDto;
    }


    public TableTopDto deleteTableByName(String name) {
        TableTop table = tableRepository.findByTableNumber(name)
                .orElseThrow(() -> new NotFoundException("Table", name));

        tableRepository.delete(table);
        TableTopDto tableTopDto = new TableTopDto();
        BeanUtils.copyProperties(table, tableTopDto);
        return tableTopDto;
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

        TableTop savedTable = tableRepository.save(tableOld);
        TableTopDto tableDto = new TableTopDto();
        BeanUtils.copyProperties(savedTable, tableDto);
        return tableDto;
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
        TableTopDto tableResponse = new TableTopDto();
        BeanUtils.copyProperties(table, tableResponse);
        return tableResponse;
    }

    public List<TableTopDto> getAvailableTables() {
        List<TableTopDto> tableTopDtos = new ArrayList<>();
        List<TableTop> tableTops = tableRepository.findAllByTableStatus(TableStatus.AVAILABLE);

        for (TableTop tableTop : tableTops) {
            TableTopDto tableTopDto = new TableTopDto();
            BeanUtils.copyProperties(tableTop, tableTopDto);
            tableTopDtos.add(tableTopDto);
        }

        return tableTopDtos;
    }
}
