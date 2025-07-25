package com.ste.restaurant.service;

import com.ste.restaurant.dto.TableTopDto;
import com.ste.restaurant.entity.TableStatus;
import com.ste.restaurant.entity.TableTop;
import com.ste.restaurant.repository.TableTopRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
            return null;
        }

        if (tableRepository.existsTableTopByTableNumber(tableTopDto.getTableNumber())) {
            return null;
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
        Optional<TableTop> tableTop = tableRepository.findByTableNumber(name);
        if (tableTop.isEmpty()) {
            return null;
        }
        TableTopDto tableTopDto = new TableTopDto();
        BeanUtils.copyProperties(tableTop.get(), tableTopDto);
        return tableTopDto;
    }


    public TableTopDto deleteTableByName(String name) {
        Optional<TableTop> tableTop = tableRepository.findByTableNumber(name);
        if (tableTop.isEmpty()) {
            return null;
        }
        TableTop table = tableTop.get();
        tableRepository.delete(table);
        TableTopDto tableTopDto = new TableTopDto();
        BeanUtils.copyProperties(table, tableTopDto);
        return tableTopDto;
    }

    public TableTopDto updateTable(String name, TableTopDto table) {
        Optional<TableTop> tableTopOpt = tableRepository.findByTableNumber(name);
        if (tableTopOpt.isEmpty()) {
            return null;
        }
        TableTop tableOld = tableTopOpt.get();
        if (table.getTableNumber() != null) {
            if (tableRepository.findByTableNumber(table.getTableNumber()).isPresent()) {
                return null;
            }
            tableOld.setTableNumber(table.getTableNumber());
        }
        if (table.getTableStatus() != null) {
            tableOld.setTableStatus(table.getTableStatus());
        }
        tableRepository.save(tableOld);

        TableTopDto tableDto = new TableTopDto();
        BeanUtils.copyProperties(table, tableDto);
        return tableDto;
    }

    @Transactional
    public TableTopDto updateTableStatusByName(String name, String status) {
        if (status == null) {
            throw new IllegalArgumentException("Status must not be null");
        }

        TableTop table = tableRepository.findByTableNumber(name).
                orElseThrow(() -> new UsernameNotFoundException("Table not found" + name));

        TableStatus newStatus;
        try {
            newStatus = TableStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status " + status);
        }

        if (table.getTableStatus() != null && table.getTableStatus().equals(newStatus)) {
            return null;
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
