package com.ste.restaurant.service;

import com.ste.restaurant.dto.common.StringDto;
import com.ste.restaurant.dto.TableTopDto;
import com.ste.restaurant.entity.TableStatus;
import com.ste.restaurant.entity.TableTop;
import com.ste.restaurant.exception.*;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.TableTopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TableTopService {

    private final TableTopRepository tableRepository;
    private final OrderMapper orderMapper;

    public TableTopService(TableTopRepository tableTopRepository, OrderMapper orderMapper) {
        this.tableRepository = tableTopRepository;
        this.orderMapper = orderMapper;
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

        TableTop savedTable = tableRepository.save(tableTop);
        return orderMapper.tableTopToTableTopDto(savedTable);
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
}
